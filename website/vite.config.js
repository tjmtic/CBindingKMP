import { defineConfig } from 'vite'

export default defineConfig({
    // Set base to './' for relative paths, or '/CBindingKMP/' if hosted on a subpath
    base: './',
    build: {
        outDir: 'dist',
    }
})
