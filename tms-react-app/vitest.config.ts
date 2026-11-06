/// <reference types="vitest" />
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
    plugins: [react()],
    test: {
        globals: true,
        environment: 'jsdom',
        setupFiles: './src/test/setup.ts',
        css: false,
        // Keep interactive MUI tests stable on shared CI runners.
        maxWorkers: 2,
        testTimeout: 15000,
    },
});
