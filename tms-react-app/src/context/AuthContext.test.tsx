import { render, screen, cleanup } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import { AuthProvider, useAuth } from './AuthContext';

function SessionStatus() {
    const { loading, isAuthenticated } = useAuth();
    return <span>{loading ? 'loading' : isAuthenticated ? 'signed in' : 'signed out'}</span>;
}
afterEach(() => { cleanup(); localStorage.clear(); });
describe('session restoration', () => {
    it('recovers from malformed local storage', async () => {
        localStorage.setItem('accessToken', 'old-token');
        localStorage.setItem('user', '{invalid-json');
        render(<AuthProvider><SessionStatus /></AuthProvider>);
        expect(await screen.findByText('signed out')).toBeInTheDocument();
        expect(localStorage.getItem('accessToken')).toBeNull();
        expect(localStorage.getItem('user')).toBeNull();
    });
});
