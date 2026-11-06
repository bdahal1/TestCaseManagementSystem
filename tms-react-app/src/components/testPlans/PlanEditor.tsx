import { useEffect, useState } from 'react';
import { Alert, Autocomplete, Box, Button, Checkbox, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle, MenuItem, TextField, Typography } from '@mui/material';
import api from '../../services/api';
import { projectCases } from './projectCases';
import { type AvailableCase, type Metadata, type Person, emptyMetadata, errorMessage, metadataBody } from './types';

interface Props { open: boolean; projectId: number; planId?: number; initial?: Metadata; onClose: () => void; onSaved: (id: number) => void }
export default function PlanEditor({ open, projectId, planId, initial, onClose, onSaved }: Props) {
    const [form, setForm] = useState<Metadata>(emptyMetadata);
    const [cases, setCases] = useState<AvailableCase[]>([]);
    const [selected, setSelected] = useState<AvailableCase[]>([]);
    const [testers, setTesters] = useState<Person[]>([]);
    const [assignee, setAssignee] = useState('');
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    useEffect(() => {
        if (!open) return;
        let active = true;
        setForm(initial ? { ...emptyMetadata, ...Object.fromEntries(Object.entries(initial).map(([key, value]) => [key, value ?? ''])) } : emptyMetadata);
        setSelected([]); setAssignee(''); setError('');
        if (!planId) {
            setLoading(true);
            Promise.all([projectCases(projectId), api.get(`/testPlans/project/${projectId}/testers`)])
                .then(([caseResponse, people]) => { if (active) { setCases(caseResponse); setTesters(people.data); } })
                .catch(e => { if (active) setError(errorMessage(e, 'Could not load project cases.')); })
                .finally(() => { if (active) setLoading(false); });
        }
        return () => { active = false; };
    }, [open, projectId, planId, initial]);
    const field = (name: keyof Metadata) => ({ value: form[name], onChange: (e: React.ChangeEvent<HTMLInputElement>) => setForm(previous => ({ ...previous, [name]: e.target.value })) });
    const save = async () => {
        setSaving(true); setError('');
        try {
            let id = planId;
            if (id) await api.put(`/testPlans/${id}`, metadataBody(form));
            else {
                const response = await api.post('/testPlans', { projectId, metadata: metadataBody(form), caseIds: selected.map(tc => tc.id), assigneeId: assignee ? Number(assignee) : null });
                id = response.data.id;
            }
            onSaved(id!);
        } catch (e) { setError(errorMessage(e, 'Could not save this plan.')); }
        finally { setSaving(false); }
    };
    return <Dialog open={open} onClose={saving ? undefined : onClose} maxWidth="md" fullWidth>
        <DialogTitle>{planId ? 'Edit test plan' : 'Plan a release test run'}</DialogTitle>
        <DialogContent>
            <Typography color="text.secondary" sx={{ mb: 3 }}>Choose the scope, release details, and people responsible for testing.</Typography>
            {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2 }}>
                <TextField label="Plan name" required autoFocus {...field('name')} inputProps={{ maxLength: 255 }} sx={{ gridColumn: '1 / -1' }} />
                <TextField label="Release" placeholder="e.g. Version 2.4" {...field('release')} inputProps={{ maxLength: 100 }} />
                <TextField label="Build" placeholder="e.g. 2.4.0-rc.1" {...field('build')} inputProps={{ maxLength: 100 }} />
                <TextField label="Environment" placeholder="e.g. Staging" {...field('environment')} inputProps={{ maxLength: 100 }} />
                <TextField label="Due date" type="date" InputLabelProps={{ shrink: true }} {...field('dueDate')} />
                <TextField label="Scope and notes" multiline minRows={2} {...field('description')} inputProps={{ maxLength: 4000 }} sx={{ gridColumn: '1 / -1' }} />
                {!planId && <>
                    <Autocomplete multiple disableCloseOnSelect options={cases} value={selected} loading={loading}
                        getOptionLabel={tc => `#${tc.id} · ${tc.testName}`}
                        isOptionEqualToValue={(a, b) => a.id === b.id} onChange={(_, value) => setSelected(value)}
                        renderOption={(props, option, { selected: checked }) => <li {...props} key={option.id}><Checkbox checked={checked} sx={{ mr: 1 }} />{option.testName}</li>}
                        renderInput={params => <TextField {...params} label="Test cases" required helperText={`${selected.length} selected. Select up to 1,000 cases. Search by name or ID.`} />}
                        sx={{ gridColumn: '1 / -1' }} />
                    <TextField select label="Assign selected cases to" value={assignee} onChange={e => setAssignee(e.target.value)} sx={{ gridColumn: '1 / -1' }}>
                        <MenuItem value="">Assign later</MenuItem>{testers.map(person => <MenuItem key={person.id} value={person.id}>{person.name}</MenuItem>)}
                    </TextField>
                    <Typography variant="caption" color="text.secondary" sx={{ gridColumn: '1 / -1' }}>The plan starts as a draft. You can assign individual cases before starting.</Typography>
                </>}
            </Box>
        </DialogContent>
        <DialogActions sx={{ p: 3 }}><Button onClick={onClose} disabled={saving}>Cancel</Button><Button variant="contained" disabled={saving || loading || !form.name.trim() || (!planId && (!selected.length || selected.length > 1000))} onClick={save}>
            {saving ? <CircularProgress size={20} /> : planId ? 'Save changes' : 'Create draft plan'}
        </Button></DialogActions>
    </Dialog>;
}
