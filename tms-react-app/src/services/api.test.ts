import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest';
import { AxiosError, AxiosHeaders, type AxiosResponse } from 'axios';
import api from './api';

describe('API authentication', () => {
    const originalAdapter = api.defaults.adapter;
    beforeEach(() => localStorage.clear());
    afterEach(() => { api.defaults.adapter = originalAdapter; vi.restoreAllMocks(); });
    it('keeps the session on a permission error and expires it on 401', async () => {
        const logout = vi.fn();
        window.addEventListener('auth:logout', logout);
        try {
            for (const status of [403, 401]) {
                api.defaults.adapter = async config => {
                    const response: AxiosResponse = { data: {}, status, statusText: 'Denied', config, headers: new AxiosHeaders() };
                    throw new AxiosError('Denied', 'ERR_BAD_REQUEST', config, undefined, response);
                };
                await expect(api.get('/testCase')).rejects.toThrow('Denied');
                expect(logout).toHaveBeenCalledTimes(status === 403 ? 0 : 1);
            }
        } finally { window.removeEventListener('auth:logout', logout); }
    });
    it('attaches tokens only to protected requests and does not log headers', async () => {
        localStorage.setItem('accessToken', 'sensitive-token');
        const log = vi.spyOn(console, 'log');
        const headers: unknown[] = [];
        api.defaults.adapter = async config => {
            headers.push(config.headers.Authorization);
            return { data: {}, status: 200, statusText: 'OK', config, headers: new AxiosHeaders() };
        };
        await api.get('/testCase');
        await api.post('/login');
        expect(headers).toEqual(['Bearer sensitive-token', undefined]);
        expect(log).not.toHaveBeenCalled();
    });
});
