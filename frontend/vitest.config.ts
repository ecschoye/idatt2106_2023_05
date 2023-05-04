import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
    plugins: [vue()],
    test: {
        environment: 'jsdom',
        globals: true,
        coverage: {
            provider: 'c8'
        }
    },
    resolve: {
        alias: {
            '~': __dirname,
        },
    },
    logLevel: 'warn',
})
