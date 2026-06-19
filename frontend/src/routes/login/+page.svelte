<script lang="ts">
	import Notice from '$lib/components/Notice.svelte';
	import Icons from '$lib/components/Icons.svelte';
	import { api, isAdminSession, saveSession } from '$lib/api';
	import type { LoginData } from '$lib/types';
	import { fade } from 'svelte/transition';

	let mode = $state<'login' | 'activate' | 'forgot'>('login');
	let email = $state('');
	let password = $state('');
	let showPassword = $state(false);
	let activationEmail = $state('');
	let resetEmail = $state('');
	let busy = $state(false);
	let error = $state('');
	let success = $state('');

	async function login() {
		busy = true; error = ''; success = '';
		try {
			const payload = await api<LoginData>('/api/auths/login', {
				method: 'POST',
				body: JSON.stringify({ email, password })
			});
			if (!payload.data) throw new Error('Login response kosong.');
			saveSession(payload.data);
			success = 'Login berhasil. Selamat datang kembali!';
			setTimeout(() => {
				window.location.href = isAdminSession(payload.data ?? null) ? '/admin' : '/app';
			}, 800);
		} catch (err) {
			error = err instanceof Error ? err.message : 'Login gagal.';
		} finally {
			busy = false;
		}
	}

	async function requestActivation() {
		busy = true; error = ''; success = '';
		try {
			const payload = await api<null>('/api/auths/activations/request', {
				method: 'POST',
				body: JSON.stringify({ email: activationEmail })
			});
			success = payload.message || 'Instruksi aktivasi telah dikirim ke email Anda.';
			activationEmail = '';
		} catch (err) {
			error = err instanceof Error ? err.message : 'Permintaan aktivasi gagal.';
		} finally {
			busy = false;
		}
	}

	async function requestPasswordReset() {
		busy = true; error = ''; success = '';
		try {
			const payload = await api<null>('/api/auths/passwords/forgot', {
				method: 'POST',
				body: JSON.stringify({ email: resetEmail })
			});
			success = payload.message || 'Jika email valid, instruksi reset password telah dikirim.';
			resetEmail = '';
		} catch (err) {
			error = err instanceof Error ? err.message : 'Permintaan reset password gagal.';
		} finally {
			busy = false;
		}
	}
</script>

<svelte:head>
	<title>{mode === 'login' ? 'Masuk' : mode === 'activate' ? 'Aktivasi' : 'Reset Password'} - Divdik Course</title>
</svelte:head>

<div class="login-container">
	<div class="login-card card">
		<div class="brand-header">
			<div class="brand-icon">D</div>
			<h2>Divdik Course</h2>
			<p>Portal Akademik & Manajemen Kursus</p>
		</div>

		<div class="mode-toggle">
			<button class:active={mode === 'login'} onclick={() => mode = 'login'}>Login</button>
			<button class:active={mode === 'activate'} onclick={() => mode = 'activate'}>Aktivasi Akun</button>
			<button class:active={mode === 'forgot'} onclick={() => mode = 'forgot'}>Reset Password</button>
		</div>

		<Notice {error} {success} />

		{#if mode === 'login'}
			<form onsubmit={(e) => { e.preventDefault(); void login(); }} in:fade={{ duration: 200 }}>
				<div class="form-group">
					<label for="email">Email Address</label>
					<div class="input-with-icon">
						<Icons name="users" size={18} />
						<input bind:value={email} id="email" type="email" placeholder="nama@email.com" required />
					</div>
				</div>
				<div class="form-group">
					<label for="password">Password</label>
					<div class="input-with-icon">
						<Icons name="shield" size={18} />
						<input bind:value={password} id="password" type={showPassword ? 'text' : 'password'} placeholder="••••••••" required />
						<button type="button" class="toggle-password" onclick={() => showPassword = !showPassword}>
							<Icons name={showPassword ? 'eyeOff' : 'eye'} size={18} />
						</button>
					</div>
				</div>
				<button class="btn btn-primary w-full" disabled={busy} type="submit">
					{#if busy}
						<span class="spinner"></span>
					{:else}
						<span>Masuk ke Akun</span>
					{/if}
				</button>
			</form>
		{:else if mode === 'activate'}
			<form onsubmit={(e) => { e.preventDefault(); void requestActivation(); }} in:fade={{ duration: 200 }}>
				<div class="activation-info">
					<Icons name="book" size={24} />
					<p>Akun Anda harus didaftarkan terlebih dahulu oleh Admin. Masukkan email yang terdaftar untuk menerima link aktivasi.</p>
				</div>
				<div class="form-group">
					<label for="act-email">Email Terdaftar</label>
					<div class="input-with-icon">
						<Icons name="users" size={18} />
						<input bind:value={activationEmail} id="act-email" type="email" placeholder="nama@email.com" required />
					</div>
				</div>
				<button class="btn btn-primary w-full" disabled={busy} type="submit">
					{#if busy}
						<span class="spinner"></span>
					{:else}
						<span>Kirim Link Aktivasi</span>
					{/if}
				</button>
			</form>
		{:else}
			<form onsubmit={(e) => { e.preventDefault(); void requestPasswordReset(); }} in:fade={{ duration: 200 }}>
				<div class="activation-info">
					<Icons name="shield" size={24} />
					<p>Masukkan email akun aktif Anda. Sistem akan mengirim link untuk membuat password baru.</p>
				</div>
				<div class="form-group">
					<label for="reset-email">Email Akun</label>
					<div class="input-with-icon">
						<Icons name="users" size={18} />
						<input bind:value={resetEmail} id="reset-email" type="email" placeholder="nama@email.com" required />
					</div>
				</div>
				<button class="btn btn-primary w-full" disabled={busy} type="submit">
					{#if busy}
						<span class="spinner"></span>
					{:else}
						<span>Kirim Link Reset</span>
					{/if}
				</button>
			</form>
		{/if}

		<div class="login-footer">
			<p>&copy; 2026 Divdik Management System</p>
		</div>
	</div>
</div>

<style>
	.login-container {
		min-height: calc(100vh - 4rem);
		display: grid;
		place-items: center;
		padding: 1rem;
		background: radial-gradient(circle at top right, var(--primary-light), transparent),
		            radial-gradient(circle at bottom left, var(--primary-soft), transparent);
	}

	.login-card {
		width: 100%;
		max-width: 440px;
		padding: 2.5rem;
		border: none;
		box-shadow: var(--shadow-lg);
	}

	.brand-header {
		text-align: center;
		margin-bottom: 2rem;
	}

	.brand-header .brand-icon {
		margin: 0 auto 1rem;
		width: 48px;
		height: 48px;
		font-size: 1.5rem;
	}

	.brand-header h2 {
		margin-bottom: 0.25rem;
		color: var(--primary);
	}

	.brand-header p {
		font-size: 0.875rem;
		font-weight: 500;
	}

	.mode-toggle {
		display: grid;
		grid-template-columns: repeat(3, minmax(0, 1fr));
		background: var(--border-light);
		padding: 0.375rem;
		border-radius: 10px;
		margin-bottom: 2rem;
	}

	.mode-toggle button {
		border: none;
		background: transparent;
		padding: 0.625rem;
		font-size: 0.875rem;
		font-weight: 700;
		border-radius: 8px;
		cursor: pointer;
		transition: all 0.2s;
		color: var(--secondary);
	}

	.mode-toggle button.active {
		background: var(--bg-surface);
		color: var(--primary);
		box-shadow: var(--shadow-sm);
	}

	.input-with-icon {
		position: relative;
	}

	.input-with-icon > :global(svg) {
		position: absolute;
		left: 1rem;
		top: 50%;
		transform: translateY(-50%);
		color: var(--text-light);
	}

	.input-with-icon input {
		padding-left: 2.75rem;
		padding-right: 2.75rem;
	}

	.toggle-password {
		position: absolute;
		right: 1rem;
		top: 50%;
		transform: translateY(-50%);
		background: none;
		border: none;
		cursor: pointer;
		color: var(--text-light);
		padding: 0;
		display: flex;
		align-items: center;
		z-index: 10;
	}

	.activation-info {
		background: var(--primary-soft);
		border-radius: var(--radius-sm);
		padding: 1rem;
		display: flex;
		gap: 0.75rem;
		margin-bottom: 1.5rem;
	}

	.activation-info p {
		font-size: 0.8125rem;
		color: var(--primary-hover);
		line-height: 1.4;
	}

	.login-footer {
		text-align: center;
		margin-top: 2rem;
		padding-top: 1.5rem;
		border-top: 1px solid var(--border-light);
	}

	.login-footer p {
		font-size: 0.75rem;
		color: var(--text-light);
	}

	.spinner {
		width: 20px;
		height: 20px;
		border: 2px solid rgba(255,255,255,0.3);
		border-radius: 50%;
		border-top-color: var(--text-on-primary);
		animation: spin 0.8s linear infinite;
	}

	@keyframes spin {
		to { transform: rotate(360deg); }
	}

	@media (max-width: 480px) {
		.login-card {
			padding: 1.5rem;
		}
	}
</style>
