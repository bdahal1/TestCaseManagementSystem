import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import AutomationImportDialog from './AutomationImportDialog';
import api from '../../services/api';
import { type ImportPreviewResponse, type ImportSaveResponse } from './types';

vi.mock('../../services/api', () => ({
    default: {
        get: vi.fn().mockResolvedValue({ data: [] }),
        post: vi.fn(),
    },
}));

beforeEach(() => {
    vi.mocked(api.post).mockReset();
    vi.mocked(api.get).mockResolvedValue({ data: [{ locatorName: 'login.submit', locatorType: 'CSS', locatorValue: '#submit' }] });
});
afterEach(cleanup);

describe('AutomationImportDialog', () => {
    const mockPreviewData: ImportPreviewResponse = {
        fileName: 'login.feature',
        fileType: 'FEATURE',
        hasErrors: false,
        canSave: true,
        globalErrors: [],
        totalCases: 1,
        totalSteps: 2,
        cases: [
            {
                name: 'Valid Login',
                testType: 'CUCUMBER_AUTOMATION',
                sourceLine: 2,
                conflict: true,
                existingCaseId: 10,
                existingCaseProjectId: 'DEMO-10',
                existingCaseName: 'Valid Login',
                action: 'CREATE_NEW',
                unmappedCount: 0,
                errorCount: 0,
                steps: [
                    {
                        order: 1,
                        sourceLine: 3,
                        keyword: 'Given',
                        rawText: 'open page "https://demo.app"',
                        template: 'OPEN_PAGE',
                        textValue: 'https://demo.app',
                        mapped: true,
                    },
                    {
                        order: 2,
                        sourceLine: 4,
                        keyword: 'When',
                        rawText: 'click element "login.submit"',
                        template: 'CLICK_ELEMENT',
                        locatorType: 'NAMED',
                        locatorValue: 'login.submit',
                        mapped: true,
                    },
                ],
            },
        ],
    };

    it('renders the upload step initially', async () => {
        await act(async () => { render(
            <AutomationImportDialog
                open={true}
                projectId={1}
                onClose={vi.fn()}
                onImportSuccess={vi.fn()}
            />
        ); });

        expect(screen.getByText('Import Automation Test Cases')).toBeInTheDocument();
        expect(screen.getByText('Select Automation Definition File')).toBeInTheDocument();
        expect(screen.getByText('Browse File')).toBeInTheDocument();
        expect(screen.getByText('Analyze & Preview')).toBeDisabled();
    });

    it('handles file selection and transitions to preview on analyze', async () => {
        (api.post as any).mockResolvedValueOnce({ data: mockPreviewData });

        render(
            <AutomationImportDialog
                open={true}
                projectId={1}
                onClose={vi.fn()}
                onImportSuccess={vi.fn()}
            />
        );

        const fileInput = document.getElementById('import-file-input') as HTMLInputElement;
        const testFile = new File(['Feature: Test\nScenario: Valid Login'], 'login.feature', {
            type: 'text/plain',
        });

        fireEvent.change(fileInput, { target: { files: [testFile] } });

        expect(screen.getByText(/Selected: login.feature/)).toBeInTheDocument();
        const analyzeBtn = screen.getByText('Analyze & Preview');
        expect(analyzeBtn).not.toBeDisabled();

        fireEvent.click(analyzeBtn);

        await waitFor(() => {
            expect(screen.getByText(/File:\s*login\.feature/)).toBeInTheDocument();
            expect(screen.getByDisplayValue('Valid Login')).toBeInTheDocument();
            expect(screen.getByText('open page "https://demo.app"')).toBeInTheDocument();
            expect(screen.getByText('click element "login.submit"')).toBeInTheDocument();
            expect(screen.getByText(/Name Conflict: A test case named "Valid Login" already exists/)).toBeInTheDocument();
        });
    });

    it('supports selecting conflict replacement and saves confirmed import', async () => {
        (api.post as any).mockResolvedValueOnce({ data: mockPreviewData });

        const mockSaveResponse: ImportSaveResponse = {
            createdCount: 0,
            replacedCount: 1,
            savedCases: [
                {
                    id: 10,
                    testProjectId: 'DEMO-10',
                    name: 'Valid Login',
                    testType: 'CUCUMBER_AUTOMATION',
                    action: 'REPLACED',
                },
            ],
        };
        (api.post as any).mockResolvedValueOnce({ data: mockSaveResponse });

        const onImportSuccess = vi.fn();
        render(
            <AutomationImportDialog
                open={true}
                projectId={1}
                onClose={vi.fn()}
                onImportSuccess={onImportSuccess}
            />
        );

        // Upload and preview
        const fileInput = document.getElementById('import-file-input') as HTMLInputElement;
        const testFile = new File(['Feature: Test'], 'login.feature', { type: 'text/plain' });
        fireEvent.change(fileInput, { target: { files: [testFile] } });
        fireEvent.click(screen.getByText('Analyze & Preview'));

        await waitFor(() => {
            expect(screen.getByText('Replace existing case (DEMO-10)')).toBeInTheDocument();
        });

        // Switch conflict resolution to REPLACE_EXISTING
        const replaceRadio = screen.getByLabelText('Replace existing case (DEMO-10)');
        fireEvent.click(replaceRadio);

        // Save
        const saveBtn = screen.getByText('Save to Test Case Library');
        expect(saveBtn).not.toBeDisabled();
        fireEvent.click(saveBtn);

        await waitFor(() => {
            expect(screen.getByText('Import Completed Successfully!')).toBeInTheDocument();
            expect(screen.getByText(/\[DEMO-10\] Valid Login/)).toBeInTheDocument();
            expect(onImportSuccess).toHaveBeenCalledWith(mockSaveResponse.savedCases);
        });
    });

    it('flags unmapped steps and prevents save until mapped', async () => {
        const previewWithUnmapped: ImportPreviewResponse = {
            fileName: 'custom.feature',
            fileType: 'FEATURE',
            hasErrors: true,
            canSave: false,
            globalErrors: [],
            totalCases: 1,
            totalSteps: 1,
            cases: [
                {
                    name: 'Custom Flow',
                    testType: 'CUCUMBER_AUTOMATION',
                    sourceLine: 2,
                    conflict: false,
                    action: 'CREATE_NEW',
                    unmappedCount: 1,
                    errorCount: 1,
                    steps: [
                        {
                            order: 1,
                            sourceLine: 3,
                            keyword: 'When',
                            rawText: 'magic step without template',
                            template: undefined,
                            mapped: false,
                            validationError: 'Unrecognized step text.',
                        },
                    ],
                },
            ],
        };

        (api.post as any).mockResolvedValueOnce({ data: previewWithUnmapped });

        render(
            <AutomationImportDialog
                open={true}
                projectId={1}
                onClose={vi.fn()}
                onImportSuccess={vi.fn()}
            />
        );

        const fileInput = document.getElementById('import-file-input') as HTMLInputElement;
        const testFile = new File(['Feature: Test'], 'custom.feature', { type: 'text/plain' });
        fireEvent.change(fileInput, { target: { files: [testFile] } });
        fireEvent.click(screen.getByText('Analyze & Preview'));

        await waitFor(() => {
            expect(screen.getByText('magic step without template')).toBeInTheDocument();
            expect(screen.getByText('1 Unmapped Step(s)')).toBeInTheDocument();
            // Save button should be disabled
            expect(screen.getByText('Save to Test Case Library')).toBeDisabled();
        });
    });
    async function openPreview(data: ImportPreviewResponse) {
        vi.mocked(api.post).mockResolvedValueOnce({ data });
        render(<AutomationImportDialog open projectId={1} onClose={vi.fn()} onImportSuccess={vi.fn()} />);
        const input=document.getElementById('import-file-input') as HTMLInputElement;
        fireEvent.change(input,{ target: { files: [new File(['Feature: Test'],'test.feature',{type:'text/plain'})] } });
        fireEvent.click(screen.getByText('Analyze & Preview'));
        await screen.findByDisplayValue(data.cases[0].name);
    }

    it('reinfers a corrected locator and submits CSS rather than the stale named type', async () => {
        const data=structuredClone(mockPreviewData);
        data.hasErrors=true; data.canSave=false;
        data.cases[0].steps[1].locatorValue='Login.Missing';
        data.cases[0].steps[1].validationError='Reusable named locator is missing';
        await openPreview(data);
        expect(screen.getByText('Save to Test Case Library')).toBeDisabled();
        const input=screen.getByDisplayValue('Login.Missing') as HTMLInputElement;
        fireEvent.change(input,{target:{value:'#submit'}});
        await waitFor(()=>expect(screen.getByText('Save to Test Case Library')).not.toBeDisabled());
        vi.mocked(api.post).mockResolvedValueOnce({data:{createdCount:1,replacedCount:0,savedCases:[]}});
        fireEvent.click(screen.getByText('Save to Test Case Library'));
        await screen.findByText('Import Completed Successfully!');
        expect(vi.mocked(api.post).mock.calls[1][1]).toMatchObject({cases:[{steps:[{template:'OPEN_PAGE'}, {locatorType:'CSS',locatorValue:'#submit'}]}]});
    });

    it('requires the URL after mapping an unknown step and revalidates a cleared case name', async () => {
        const data=structuredClone(mockPreviewData); data.hasErrors=true; data.canSave=false;
        data.cases[0].steps=[{order:1,rawText:'custom navigation',mapped:false}];
        await openPreview(data);
        fireEvent.mouseDown(document.getElementById('step-template-select-0-0')!);
        fireEvent.click(await screen.findByRole('option',{name:'Open page'}));
        expect(screen.getByText('Save to Test Case Library')).toBeDisabled();
        fireEvent.change(document.getElementById('step-value-input-0-0')!,{target:{value:'https://example.test'}});
        await waitFor(()=>expect(screen.getByText('Save to Test Case Library')).not.toBeDisabled());
        fireEvent.change(screen.getByLabelText('Test Case Name'),{target:{value:'   '}});
        expect(screen.getByText('Save to Test Case Library')).toBeDisabled();
        expect(screen.getByText('Name cannot be empty')).toBeInTheDocument();
        fireEvent.change(screen.getByLabelText('Test Case Name'),{target:{value:'Corrected name'}});
        expect(screen.getByText('Save to Test Case Library')).not.toBeDisabled();
    });

    it('keeps unsupported types blocked after editing a mapped step', async () => {
        const data=structuredClone(mockPreviewData); data.hasErrors=true; data.canSave=false;
        data.cases[0].testType='MANUAL'; data.cases[0].validationError='Only automation types are supported';
        await openPreview(data);
        fireEvent.change(document.getElementById('step-value-input-0-0')!,{target:{value:'https://example.test'}});
        expect(screen.getByText('Save to Test Case Library')).toBeDisabled();
        expect(screen.getByText('Only CUCUMBER_AUTOMATION and KEYWORD_DRIVEN test types are supported')).toBeInTheDocument();
    });

    it('preserves and submits all five new template fields on save', async () => {
        const data: ImportPreviewResponse = {
            fileName: 'extended.feature',
            fileType: 'FEATURE',
            hasErrors: false,
            canSave: true,
            globalErrors: [],
            totalCases: 1,
            totalSteps: 5,
            cases: [
                {
                    name: 'Extended Controls Case',
                    testType: 'CUCUMBER_AUTOMATION',
                    sourceLine: 1,
                    conflict: false,
                    action: 'CREATE_NEW',
                    unmappedCount: 0,
                    errorCount: 0,
                    steps: [
                        {
                            order: 1,
                            keyword: 'When',
                            rawText: 'select option "pw" by value in "#framework-select"',
                            template: 'SELECT_OPTION',
                            locatorType: 'CSS',
                            locatorValue: '#framework-select',
                            textValue: 'pw',
                            selectBy: 'VALUE',
                            mapped: true,
                        },
                        {
                            order: 2,
                            keyword: 'When',
                            rawText: 'set checkbox "#subscribe-checkbox" to checked',
                            template: 'SET_CHECKBOX',
                            locatorType: 'CSS',
                            locatorValue: '#subscribe-checkbox',
                            checkedValue: true,
                            mapped: true,
                        },
                        {
                            order: 3,
                            keyword: 'When',
                            rawText: 'press key "Enter" on "#keyboard-input"',
                            template: 'PRESS_KEY',
                            locatorType: 'CSS',
                            locatorValue: '#keyboard-input',
                            keyValue: 'Enter',
                            mapped: true,
                        },
                        {
                            order: 4,
                            keyword: 'Then',
                            rawText: 'verify url contains "/demo/extended"',
                            template: 'VERIFY_URL',
                            textValue: '/demo/extended',
                            urlComparison: 'CONTAINS',
                            mapped: true,
                        },
                        {
                            order: 5,
                            keyword: 'Then',
                            rawText: 'verify attribute "data-status" of "#action-btn" equals "ready"',
                            template: 'VERIFY_ATTRIBUTE',
                            locatorType: 'CSS',
                            locatorValue: '#action-btn',
                            attributeName: 'data-status',
                            textValue: 'ready',
                            mapped: true,
                        },
                    ],
                },
            ],
        };

        await openPreview(data);
        expect(screen.getByText('Save to Test Case Library')).not.toBeDisabled();
        vi.mocked(api.post).mockResolvedValueOnce({ data: { createdCount: 1, replacedCount: 0, savedCases: [] } });
        fireEvent.click(screen.getByText('Save to Test Case Library'));
        await screen.findByText('Import Completed Successfully!');

        const saveCall = vi.mocked(api.post).mock.calls[1][1] as any;
        expect(saveCall.cases[0].steps).toEqual([
            expect.objectContaining({ template: 'SELECT_OPTION', selectBy: 'VALUE', textValue: 'pw' }),
            expect.objectContaining({ template: 'SET_CHECKBOX', checkedValue: true }),
            expect.objectContaining({ template: 'PRESS_KEY', keyValue: 'Enter' }),
            expect.objectContaining({ template: 'VERIFY_URL', urlComparison: 'CONTAINS', textValue: '/demo/extended' }),
            expect.objectContaining({ template: 'VERIFY_ATTRIBUTE', attributeName: 'data-status', textValue: 'ready' }),
        ]);
    });

    it('requires an explicit missing checkbox state and preserves variable key input', async () => {
        const data=structuredClone(mockPreviewData); data.hasErrors=true; data.canSave=false;
        data.cases[0].steps=[
            {order:1,template:'SET_CHECKBOX',locatorValue:'#box',mapped:true,validationError:'Choose Checked or Unchecked'},
            {order:2,template:'PRESS_KEY',locatorValue:'#input',keyValue:'${key}',mapped:true},
        ];
        await openPreview(data);
        expect(screen.getByText('Save to Test Case Library')).toBeDisabled();
        fireEvent.mouseDown(screen.getByRole('combobox', {name:'Checkbox state'}));
        fireEvent.click(await screen.findByRole('option',{name:'Unchecked'}));
        await waitFor(()=>expect(screen.getByText('Save to Test Case Library')).not.toBeDisabled());
        vi.mocked(api.post).mockResolvedValueOnce({data:{createdCount:1,replacedCount:0,savedCases:[]}});
        fireEvent.click(screen.getByText('Save to Test Case Library'));
        await screen.findByText('Import Completed Successfully!');
        expect(vi.mocked(api.post).mock.calls[1][1]).toMatchObject({cases:[{steps:[{checkedValue:false},{keyValue:'${key}'}]}]});
    });

});
