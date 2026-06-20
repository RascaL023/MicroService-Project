<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import AccessPanel from '$lib/components/AccessPanel.svelte';
	import Icons from '$lib/components/Icons.svelte';
	import Notice from '$lib/components/Notice.svelte';
	import PageTitle from '$lib/components/PageTitle.svelte';
	import { api } from '$lib/api';
	import type { User } from '$lib/types';

	let user = $state<User | null>(null);
	let loading = $state(true);
	let error = $state('');

	const userId = $derived(Number(page.params.id));
	const portalPrefix = $derived(page.url.pathname.startsWith('/admin/') ? '/admin' : '');

	onMount(() => void loadUser());

	async function loadUser() {
		loading = true;
		error = '';
		try {
			const payload = await api<User>(`/api/users/${userId}`);
			user = payload.data ?? null;
		} catch (err) {
			error = err instanceof Error ? err.message : 'Gagal memuat detail user.';
		} finally {
			loading = false;
		}
	}

	function dateLabel(value?: string | null) {
		if (!value) return '-';
		const parsed = new Date(value);
		if (Number.isNaN(parsed.getTime())) return '-';
		return new Intl.DateTimeFormat('id-ID', {
			dateStyle: 'medium',
			timeStyle: 'short'
		}).format(parsed);
	}
</script>

<svelte:head><title>Detail User - Divdik Course</title></svelte:head>

<button class="btn btn-ghost back-button" type="button" onclick={() => goto(`${portalPrefix}/users`)}>
	<Icons name="chevronLeft" size={17} />
	<span>Kembali</span>
</button>

<PageTitle eyebrow="User Profile" title="Detail User" description="Ringkasan profil administratif peserta." />

<Notice {error} />

<AccessPanel authorities={['user.read', 'user.*']}>
	{#if loading}
		<div class="card">Memuat detail user...</div>
	{:else if !user}
		<div class="card">User tidak ditemukan.</div>
	{:else}
		<section class="profile-shell">
			<div class="profile-hero">
				<div class="avatar-xl">{user.name.slice(0, 1).toUpperCase()}</div>
				<div>
					<h2>{user.name}</h2>
					<p>{user.email}</p>
				</div>
				<span class="badge badge-green">{user.status ?? 'ACTIVE'}</span>
			</div>

			<div class="detail-grid">
				<div>
					<span>Jurusan</span>
					<strong>{user.majorName ?? '-'}</strong>
					<small>{user.majorId ?? '-'}</small>
				</div>
				<div>
					<span>Batch</span>
					<strong>{user.batch}</strong>
				</div>
				<div>
					<span>Gender</span>
					<strong>{user.gender ?? '-'}</strong>
				</div>
				<div>
					<span>Di daftarkan pada</span>
					<strong>{dateLabel(user.createdAt)}</strong>
				</div>
				<div>
					<span>Lulus pada</span>
					<strong>{dateLabel(user.graduatedAt)}</strong>
				</div>
			</div>
		</section>
	{/if}
</AccessPanel>

<style>
	.back-button {
		margin-bottom: 1rem;
	}

	.profile-shell {
		display: grid;
		gap: 1.25rem;
	}

	.profile-hero {
		display: grid;
		grid-template-columns: auto minmax(0, 1fr) auto;
		gap: 1rem;
		align-items: center;
		padding: 1.25rem;
		border: 1px solid var(--border);
		border-radius: var(--radius);
		background: var(--bg-surface);
		box-shadow: var(--shadow-sm);
	}

	.avatar-xl {
		width: 64px;
		height: 64px;
		display: grid;
		place-items: center;
		border-radius: var(--radius-sm);
		background: var(--primary-soft);
		color: var(--primary);
		font-size: 1.5rem;
		font-weight: 900;
		border: 1px solid var(--primary-border);
	}

	.profile-hero h2 {
		font-size: 1.35rem;
	}

	.profile-hero p {
		margin-top: 0.25rem;
		overflow-wrap: anywhere;
	}

	.detail-grid {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
		gap: 1rem;
	}

	.detail-grid > div {
		display: grid;
		gap: 0.25rem;
		padding: 1rem;
		border: 1px solid var(--border);
		border-radius: var(--radius-sm);
		background: var(--bg-surface);
		box-shadow: var(--shadow-sm);
	}

	.detail-grid span {
		color: var(--text-muted);
		font-size: 0.72rem;
		font-weight: 800;
		text-transform: uppercase;
	}

	.detail-grid strong {
		font-size: 0.95rem;
		overflow-wrap: anywhere;
	}

	@media (max-width: 640px) {
		.profile-hero {
			grid-template-columns: 1fr;
		}
	}
</style>
