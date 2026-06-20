<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import AccessPanel from '$lib/components/AccessPanel.svelte';
	import Icons from '$lib/components/Icons.svelte';
	import Notice from '$lib/components/Notice.svelte';
	import PageTitle from '$lib/components/PageTitle.svelte';
	import { api, readSession, saveSession } from '$lib/api';
	import type { LoginData, User } from '$lib/types';

	let user = $state<User | null>(null);
	let session = $state<LoginData | null>(null);
	let loading = $state(true);
	let showEmailModal = $state(false);
	let emailForm = $state('');
	let busy = $state(false);
	let error = $state('');
	let success = $state('');

	const routeUserId = $derived(page.params.id);
	const userId = $derived(routeUserId === 'me' ? (session?.userId ?? 0) : Number(routeUserId));
	const portalPrefix = $derived(
		page.url.pathname.startsWith('/admin/') ? '/admin' :
		page.url.pathname.startsWith('/app/') ? '/app' : ''
	);
	const isSelf = $derived(Boolean(session?.userId && (routeUserId === 'me' || userId === session.userId)));

	onMount(() => {
		session = readSession();
		void loadUser();
	});

	async function loadUser() {
		loading = true;
		error = '';
		try {
			if (!userId) throw new Error('User tidak valid.');
			const payload = await api<User>(`/api/users/${userId}`);
			user = payload.data ?? null;
			emailForm = user?.email ?? '';
		} catch (err) {
			error = err instanceof Error ? err.message : 'Gagal memuat detail user.';
		} finally {
			loading = false;
		}
	}

	function dateLabel(value?: string | null, withTime = false) {
		if (!value) return '-';
		const parsed = new Date(value);
		if (Number.isNaN(parsed.getTime())) return '-';
		return new Intl.DateTimeFormat('id-ID', withTime ? {
			dateStyle: 'medium',
			timeStyle: 'short'
		} : {
			dateStyle: 'medium'
		}).format(parsed);
	}

	function userStatusLabel(value?: string) {
		if (value === 'GRADUATED') return 'Lulus';
		if (value === 'DROP_OUT') return 'Drop Out';
		return 'Aktif';
	}

	async function updateEmail() {
		if (!isSelf) return;
		error = '';
		success = '';
		busy = true;
		try {
			const payload = await api<User>(`/api/users/${userId}`, {
				method: 'PATCH',
				body: JSON.stringify({ email: emailForm })
			});
			user = payload.data ?? user;
			emailForm = user?.email ?? emailForm;
			if (session && user?.email) {
				session = { ...session, email: user.email };
				saveSession(session);
			}
			showEmailModal = false;
			success = 'Email berhasil diupdate.';
		} catch (err) {
			error = err instanceof Error ? err.message : 'Gagal mengupdate email.';
		} finally {
			busy = false;
		}
	}
</script>

<svelte:head><title>Detail User - Divdik Course</title></svelte:head>

<button class="btn btn-ghost back-button" type="button" onclick={() => goto(`${portalPrefix}/users`)}>
	<Icons name="chevronLeft" size={17} />
	<span>Kembali</span>
</button>

<PageTitle eyebrow="User Profile" title="Detail User" description="Ringkasan profil administratif peserta." />

<Notice {error} {success} />

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
				<div class="hero-actions">
					<span class="badge" class:badge-green={user.status !== 'DROP_OUT'} class:badge-red={user.status === 'DROP_OUT'}>
						{userStatusLabel(user.status)}
					</span>
					{#if isSelf}
						<button class="btn btn-secondary" type="button" onclick={() => showEmailModal = true}>
							<Icons name="edit" size={16} />
							<span>Ganti Email</span>
						</button>
					{/if}
				</div>
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
					<strong>{dateLabel(user.createdAt, true)}</strong>
				</div>
				<div>
					<span>Lulus pada</span>
					<strong>{dateLabel(user.graduatedAt)}</strong>
				</div>
			</div>
		</section>
	{/if}
</AccessPanel>

{#if showEmailModal && isSelf}
	<div class="modal-backdrop" role="presentation" onclick={() => showEmailModal = false}>
		<section class="modal-panel email-modal" role="dialog" aria-modal="true" tabindex="-1" onclick={(event) => event.stopPropagation()} onkeydown={(event) => event.stopPropagation()}>
			<div class="modal-head">
				<div>
					<h3>Ganti Email</h3>
					<p>Email ini akan menjadi email administratif profil user.</p>
				</div>
				<button class="btn btn-ghost icon-btn" type="button" aria-label="Tutup modal" onclick={() => showEmailModal = false}>
					<Icons name="x" size={18} />
				</button>
			</div>
			<form class="modal-form" onsubmit={(event) => { event.preventDefault(); void updateEmail(); }}>
				<label>
					<span>Email baru</span>
					<input bind:value={emailForm} type="email" required />
				</label>
				<div class="modal-actions">
					<button class="btn btn-ghost" type="button" onclick={() => showEmailModal = false}>Batal</button>
					<button class="btn btn-primary" type="submit" disabled={busy}>
						{busy ? 'Menyimpan...' : 'Simpan Email'}
					</button>
				</div>
			</form>
		</section>
	</div>
{/if}

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

	.hero-actions {
		display: flex;
		align-items: center;
		justify-content: flex-end;
		gap: 0.6rem;
		flex-wrap: wrap;
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

		.hero-actions {
			justify-content: flex-start;
		}
	}
</style>
