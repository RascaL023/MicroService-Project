// import { sveltekit } from '@sveltejs/kit/vite';
// import { defineConfig } from 'vite';
//
// export default defineConfig({
// 	plugins: [sveltekit()],
// 	server: {
// 		proxy: {
// 			'/api': {
// 				target: 'http://localhost:8000',
// 				changeOrigin: true
// 			}
// 		}
// 	}
// });

import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';

export default defineConfig({
	plugins: [sveltekit()],
	server: {
		host: '0.0.0.0',
		allowedHosts: ['dev.rascal.my.id'],
		proxy: {
			'/api': {
				target: 'http://localhost:8000',
				changeOrigin: true
			}
		}
	}
});
