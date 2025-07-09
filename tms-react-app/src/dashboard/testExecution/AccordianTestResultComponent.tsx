import React, { useState } from 'react';
import axios from 'axios';
import {
    Box,
    Button,
    Paper,
    TextField,
    Typography,
    Autocomplete
} from '@mui/material';
import {ResultStatus, TestCase} from '../../types/TestCase';

interface AccordionTestResultProps {
    testCase: TestCase;
    executionId?: number;
    token?: string |null;
    onResultUpdate: (status: ResultStatus | null, comment: string) => void;
}

const statusOptions: ResultStatus[] = ['PASS', 'FAIL', 'SKIPPED', 'BLOCKED', 'NOT_RUN'];

const AccordionTestResultComponent: React.FC<AccordionTestResultProps> = ({testCase, executionId, token, onResultUpdate}) => {
    const [expanded, setExpanded] = useState(false);
    const [status, setStatus] = useState<ResultStatus | null>(testCase.resultStatus ?? 'NOT_RUN');
    const [comment, setComment] = useState(testCase.resultComment ?? '');

    const handleSave = async () => {
        try {
            await axios.post(
                `http://localhost:8080/dhtcms/api/v1/testExecutions/results`,
                {
                    executionId,
                    testCaseId: testCase.id,
                    resultStatus: status,
                    resultComment: comment
                },
                { headers: { Authorization: `Bearer ${token}` } }
            );
            onResultUpdate(status, comment);
        } catch (err) {
            console.error('Failed to save result', err);
        }
    };

    return (
        <Paper variant="outlined" sx={{ border: 2, borderColor: getStatusColor(status), mb: 2, p: 2 }}>
            <Box onClick={() => setExpanded(!expanded)} sx={{ cursor: 'pointer' }}>
                <Typography variant="subtitle1">{testCase.testName}</Typography>
            </Box>

            {expanded && (
                <Box mt={2}>
                    <Autocomplete
                        value={status}
                        onChange={(_, newValue) => setStatus(newValue ?? 'NOT_RUN')}
                        options={statusOptions}
                        renderInput={(params) => <TextField {...params} label="Status" />}
                        fullWidth
                        sx={{ mb: 2 }}
                    />
                    <TextField
                        label="Comment"
                        value={comment}
                        onChange={(e) => setComment(e.target.value)}
                        fullWidth
                        multiline
                        rows={3}
                        sx={{ mb: 2 }}
                    />
                    <Button variant="contained" onClick={handleSave}>
                        Save
                    </Button>
                </Box>
            )}
        </Paper>
    );
};

const getStatusColor = (status: ResultStatus | null | undefined): string => {
    switch (status) {
        case 'PASS':
            return 'green';
        case 'FAIL':
            return 'red';
        case 'BLOCKED':
            return 'gray';
        case 'SKIPPED':
            return 'orange';
        default:
            return 'lightgray';
    }
};

export default AccordionTestResultComponent;
