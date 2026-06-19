<script lang="ts">
	import { goto } from '$app/navigation';
	import { onMount } from 'svelte';
	import Notice from '$lib/components/Notice.svelte';
	import Icons from '$lib/components/Icons.svelte';
	import { api, clearSession } from '$lib/api';

	let token = $state('');
	let password = $state('');
	let confirmPassword = $state('');
	let showPassword = $state(false);
	let showConfirmPassword = $state(false);
	let loading = $state(false);
	let error = $state('');
	let success = $state('');

	onMount(() => {
		token = new URLSearchParams(window.location.search).get('token') ?? '';
	});

	async function resetPassword() {
		error = '';
		success = '';
		if (password !== confirmPassword) {
			error = 'Konfirmasi password tidak sama.';
			return;
		}

		loading = true;
		try {
			await api<null>('/api/auths/passwords/reset', {
				method: 'POST',
				body: JSON.stringify({ token, password })
			});
			clearSession();
			success = 'Password berhasil diubah. Silakan login kembali.';
			setTimeout(() => void goto('/login'), 1500);
		} catch (err) {
			error = err instanceof Error ? err.message : 'Reset password gagal.';
		} finally {
			loading = false;
		}
	}
</script>

<svelte:head><title>Reset Password - Divdik Course</title></svelte:head>

<div class="reset-page">
	<div class="reset-header">
		<div class="reset-icon">
			<Icons name="shield" size={32} />
		</div>
		<h1>Reset Password</h1>
		<p>Buat password baru untuk akun Anda. Link reset hanya dapat dipakai satu kali.</p>
	</div>

	<Notice {error} {success} />

	<div class="card reset-card">
		<form
			class="reset-form"
			onsubmit={(event) => {
				event.preventDefault();
				void resetPassword();
			}}
		>
			<div class="form-group">
				<label for="token">Token Reset</label>
				<input id="token" bind:value={token} placeholder="Token dari email" required />
			</div>
			<div class="form-group">
				<label for="password">Password Baru</label>
				<div class="input-with-icon">
					<input
						id="password"
						bind:value={password}
						autocomplete="new-password"
						minlength="8"
						type={showPassword ? 'text' : 'password'}
						placeholder="Minimal 8 karakter"
						required
					/>
					<button type="button" class="toggle-password" onclick={() => showPassword = !showPassword}>
						<Icons name={showPassword ? 'eyeOff' : 'eye'} size={18} />
					</button>
				</div>
			</div>
			<div class="form-group">
				<label for="confirm-password">Konfirmasi Password</label>
				<div class="input-with-icon">
					<input
						id="confirm-password"
						bind:value={confirmPassword}
						autocomplete="new-password"
						minlength="8"
						type={showConfirmPassword ? 'text' : 'password'}
						placeholder="Ulangi password baru"
						required
					/>
					<button type="button" class="toggle-password" onclick={() => showConfirmPassword = !showConfirmPassword}>
						<Icons name={showConfirmPassword ? 'eyeOff' : 'eye'} size={18} />
					</button>
				</div>
			</div>
			<button class="btn btn-primary w-full mt-2" disabled={loading} type="submit">
				{loading ? 'Menyimpan...' : 'Simpan Password Baru'}
			</button>
		</form>
	</div>

	<div class="reset-footer">
		<a href="/login" class="btn btn-ghost">Kembali ke Login</a>
	</div>
</div>

<style>
	.reset-page {
		width: min(100%, 480px);
		margin: 4rem auto;
	}

	.reset-header {
		text-align: center;
		margin-bottom: 2rem;
	}

	.reset-header p {
		margin-top: 0.5rem;
		color: var(--text-muted);
	}

	.reset-icon {
		width: 64px;
		height: 64px;
		background: var(--primary-light);
		color: var(--primary);
		border-radius: 16px;
		display: grid;
		place-items: center;
		margin: 0 auto 1.5rem;
	}

	.reset-card {
		margin-bottom: 0;
	}

	.reset-form {
		display: flex;
		flex-direction: column;
		gap: 1rem;
	}

	.input-with-icon {
		position: relative;
	}

	.input-with-icon input {
		width: 100%;
		padding-right: 2.75rem;
	}

	.toggle-password {
		position: absolute;
		right: 0.75rem;
		top: 50%;
		transform: translateY(-50%);
		background: none;
		border: none;
		cursor: pointer;
		color: var(--text-light);
		padding: 0;
		display: flex;
		align-items: center;
	}

	.reset-footer {
		text-align: center;
		margin-top: 1.5rem;
	}

	.reset-footer a {
		font-size: 0.875rem;
	}

	@media (max-width: 560px) {
		.reset-page {
			margin: 2rem auto;
		}
	}
</style>
