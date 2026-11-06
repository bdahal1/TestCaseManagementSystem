import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, expect, it } from 'vitest';
import GherkinView from './GherkinView';
afterEach(cleanup);
it('renders keywords and a semantic examples table without executing scenario text', () => {
    render(<GherkinView source={'Scenario Outline: Sign in\nGiven I am <user>\nThen I see "dashboard"\nExamples:\n | user | page |\n | <script>alert(1)</script> | dashboard |'} />);
    expect(screen.getByText('Given')).toHaveStyle({ color: '#047857' });
    expect(screen.getByText('Then')).toHaveStyle({ color: '#7c3aed' });
    expect(screen.getByRole('table', { name: 'Gherkin data table' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'user' })).toBeInTheDocument();
    expect(document.querySelector('script')).toBeNull();
});
