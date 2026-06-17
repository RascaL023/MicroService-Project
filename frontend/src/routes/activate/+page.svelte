<script lang="ts">
	import { goto } from '$app/navigation';
	import { onMount } from 'svelte';
	import Notice from '$lib/components/Notice.svelte';
	import PageTitle from '$lib/components/PageTitle.svelte';
	import Icons from '$lib/components/Icons.svelte';
	import { api, saveSession } from '$lib/api';
	import type { LoginData } from '$lib/types';

	let token = $state('');
	let password = $state('');
	let loading = $state(false);
	let error = $state('');
	let success = $state('');

	onMount(() => {
		token = new URLSearchParams(window.location.search).get('token') ?? '';
	});

	async function completeActivation() {
		loading = true;
		error = '';
		success = '';
		try {
			const payload = await api<LoginData>('/api/auths/activations/complete', {
				method: 'POST',
				body: JSON.stringify({ token, password })
			});
			if (payload.data) saveSession(payload.data);
			success = 'Akun berhasil diaktifkan. Mengalihkan...';
			setTimeout(() => void goto('/'), 1500);
		} catch (err) {
			error = err instanceof Error ? err.message : 'Aktivasi gagal.';
		} finally {
			loading = false;
		}
	}
</script>

<svelte:head><title>Aktivasi Akun - Divdik Course</title></svelte:head>

<div style="max-width: 480px; margin: 4rem auto;">
	<div style="text-align: center; margin-bottom: 2rem;">
		<div style="width: 64px; height: 64px; background: var(--primary-light); color: var(--primary); border-radius: 16px; display: grid; place-items: center; margin: 0 auto 1.5rem;">
			<Icons name="shield" size={32} />
		</div>
		<h1>Aktivasi Akun</h1>
		<p style="color: var(--text-muted); margin-top: 0.5rem;">Silakan buat password baru untuk mengaktifkan akses Anda.</p>
	</div>

	<Notice {error} {success} />

	<div class="card">
		<form
			class="flex flex-direction-column gap-4"
			onsubmit={(event) => {
				event.preventDefault();
				void completeActivation();
			}}
		>
			<div class="form-group">
				<label for="token">Token Aktivasi</label>
				<input id="token" bind:value={token} placeholder="Masukkan token dari email" required />
			</div>
			<div class="form-group">
				<label for="password">Password Baru</label>
				<input id="password" bind:value={password} autocomplete="new-password" minlength="8" type="password" placeholder="Minimal 8 karakter" required />
			</div>
			<button class="btn btn-primary w-full mt-2" disabled={loading} type="submit">
				{loading ? 'Mengaktifkan...' : 'Aktifkan Sekarang'}
			</button>
		</form>
	</div>

	<div class="text-center mt-4">
		<a href="/login" class="btn btn-ghost" style="font-size: 0.875rem;">Kembali ke Login</a>
	</div>
</div>

<style>
	.flex-direction-column { flex-direction: column; }
	.text-center { text-align: center; }
</style>
