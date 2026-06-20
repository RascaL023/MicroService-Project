<script lang="ts">
	import { onMount } from 'svelte';
	import AccessPanel from '$lib/components/AccessPanel.svelte';
	import ConfirmModal from '$lib/components/ConfirmModal.svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import Icons from '$lib/components/Icons.svelte';
	import Notice from '$lib/components/Notice.svelte';
	import PageTitle from '$lib/components/PageTitle.svelte';
	import Pagination from '$lib/components/Pagination.svelte';
	import { api, hasAnyAuthority, pageItems, paginationMeta, readSession } from '$lib/api';
	import type { LoginData, Major, PageData, PaginationMeta } from '$lib/types';

	let majors = $state<Major[]>([]);
	let pageMeta = $state<PaginationMeta>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
	let session = $state<LoginData | null>(null);
	let keyword = $state('');
	let form = $state({ id: '', name: '' });
	let selectedMajor = $state<Major | null>(null);
	let patchName = $state('');
	let showCreate = $state(false);
	let showUpdate = $state(false);
	let loading = $state(true);
	let busy = $state('');
	let confirmState = $state({ open: false, title: '', message: '', confirmLabel: 'Ya, lanjutkan' });
	let pendingConfirm: (() => Promise<void>) | null = null;
	let error = $state('');
	let success = $state('');

	const canCreate = $derived(hasAnyAuthority(session, ['major.create', 'major.*']));
	const canUpdate = $derived(hasAnyAuthority(session, ['major.update', 'major.*']));
	const canDelete = $derived(hasAnyAuthority(session, ['major.delete', 'major.*']));
	const canManage = $derived(canUpdate || canDelete);

	onMount(() => {
		const nextSession = readSession();
		session = nextSession;
		if (hasAnyAuthority(nextSession, ['major.read', 'major.*'])) void load();
	});

	async function load() {
		loading = true;
		try {
			const query = new URLSearchParams({
				page: String(pageMeta.page),
				size: String(pageMeta.size),
				sort: 'id,asc'
			});
			if (keyword.trim()) query.set('name', keyword.trim());
			const payload = await api<PageData<Major> | Major[]>(`/api/majors?${query}`);
			majors = pageItems(payload);
			pageMeta = paginationMeta(payload, pageMeta);
		} catch (err) {
			error = err instanceof Error ? err.message : 'Gagal memuat jurusan.';
		} finally {
			loading = false;
		}
	}

	async function submit(task: () => Promise<void>) {
		error = '';
		success = '';
		try { await task(); } catch (err) { error = err instanceof Error ? err.message : 'Request gagal.'; }
	}

	async function createMajor() {
		await submit(async () => {
			busy = 'create';
			try {
				await api<Major>('/api/majors', {
					method: 'POST',
					body: JSON.stringify({ id: form.id, name: form.name })
				});
				form = { id: '', name: '' };
				showCreate = false;
				await load();
				success = 'Jurusan berhasil dibuat.';
			} finally {
				busy = '';
			}
		});
	}

	function openUpdateModal(major: Major) {
		selectedMajor = major;
		patchName = major.name;
		showUpdate = true;
	}

	async function updateMajor() {
		if (!selectedMajor) return;
		const id = selectedMajor.id;
		await submit(async () => {
			busy = 'update';
			try {
				await api<Major>(`/api/majors/${id}`, {
					method: 'PATCH',
					body: JSON.stringify({ name: patchName })
				});
				selectedMajor = null;
				patchName = '';
				showUpdate = false;
				await load();
				success = 'Jurusan berhasil diupdate.';
			} finally {
				busy = '';
			}
		});
	}

	function deleteMajor(major: Major) {
		askConfirm({
			title: 'Hapus jurusan?',
			message: `Jurusan ${major.id} - ${major.name} akan dihapus. Jurusan yang masih dipakai user aktif tidak bisa dihapus.`,
			confirmLabel: 'Hapus Jurusan'
		}, async () => {
			await submit(async () => {
				await api<null>(`/api/majors/${major.id}`, { method: 'DELETE' });
				await load();
				success = 'Jurusan berhasil dihapus.';
			});
		});
	}

	function askConfirm(config: { title: string; message: string; confirmLabel?: string }, action: () => Promise<void>) {
		confirmState = { open: true, title: config.title, message: config.message, confirmLabel: config.confirmLabel ?? 'Ya, lanjutkan' };
		pendingConfirm = action;
	}

	function closeConfirm() {
		confirmState = { ...confirmState, open: false };
		pendingConfirm = null;
	}

	function runConfirm() {
		const action = pendingConfirm;
		closeConfirm();
		if (action) void action();
	}

	function applyFilters() {
		pageMeta = { ...pageMeta, page: 0 };
		void load();
	}

	function changePage(page: number) {
		pageMeta = { ...pageMeta, page };
		void load();
	}

	function changePageSize(size: number) {
		pageMeta = { ...pageMeta, page: 0, size };
		void load();
	}
</script>

<svelte:head><title>Jurusan - Divdik Course</title></svelte:head>

<PageTitle eyebrow="User Management" title="Jurusan Kuliah" description="Kelola master jurusan yang melekat ke profil peserta dan instruktur." />

<Notice {error} {success} />

<AccessPanel authorities={['major.read', 'major.*']}>
	<section class="toolbar">
		<form class="major-filter" onsubmit={(event) => { event.preventDefault(); applyFilters(); }}>
			<label>
				<span>Cari jurusan</span>
				<input bind:value={keyword} placeholder="Contoh: TI atau Informatika" />
			</label>
			<button class="btn btn-secondary" type="submit">
				<Icons name="search" size={17} />
				<span>Cari</span>
			</button>
		</form>

		{#if canCreate}
			<button class="btn btn-primary" type="button" onclick={() => showCreate = true}>
				<Icons name="book" size={17} />
				<span>Buat Jurusan</span>
			</button>
		{/if}
	</section>

	{#if loading}
		<section class="major-grid">
			{#each Array(6) as _}<div class="major-skeleton"></div>{/each}
		</section>
	{:else if majors.length === 0}
		<EmptyState text="Data jurusan tidak ditemukan." />
	{:else}
		<section class="major-grid">
			{#each majors as major}
				<article class="major-card">
					<div class="major-code">{major.id}</div>
					<div class="major-body">
						<div>
							<h3>{major.name}</h3>
							<p>ID Jurusan {major.id}</p>
						</div>
						<span class="badge badge-blue">{major.userCount ?? 0} User</span>
					</div>
					{#if canManage}
						<div class="major-actions">
							{#if canUpdate}
								<button class="btn btn-ghost icon-btn" aria-label="Edit jurusan" type="button" onclick={() => openUpdateModal(major)}>
									<Icons name="edit" size={15} />
								</button>
							{/if}
							{#if canDelete}
								<button class="btn btn-ghost icon-btn danger" aria-label="Hapus jurusan" type="button" onclick={() => deleteMajor(major)}>
									<Icons name="x" size={15} />
								</button>
							{/if}
						</div>
					{/if}
				</article>
			{/each}
		</section>
		<Pagination meta={pageMeta} onPage={changePage} onSize={changePageSize} />
	{/if}
</AccessPanel>

{#if showCreate && canCreate}
	<div class="modal-backdrop" role="presentation" onclick={() => showCreate = false}>
		<section class="modal-panel" role="dialog" aria-modal="true" tabindex="-1" onclick={(event) => event.stopPropagation()} onkeydown={(event) => event.stopPropagation()}>
			<div class="modal-head">
				<div>
					<h3>Buat Jurusan</h3>
					<p>Gunakan ID pendek dan stabil, misalnya TI.</p>
				</div>
				<button class="btn btn-ghost icon-btn" aria-label="Tutup modal" type="button" onclick={() => showCreate = false}>
					<Icons name="x" size={18} />
				</button>
			</div>
			<form class="modal-form" onsubmit={(event) => { event.preventDefault(); void createMajor(); }}>
				<label>
					<span>ID Jurusan</span>
					<input bind:value={form.id} placeholder="TI" required />
				</label>
				<label>
					<span>Nama Jurusan</span>
					<input bind:value={form.name} placeholder="Teknik Informatika" required />
				</label>
				<div class="modal-actions">
					<button class="btn btn-ghost" type="button" onclick={() => showCreate = false}>Batal</button>
					<button class="btn btn-primary" type="submit" disabled={busy === 'create'}>
						{busy === 'create' ? 'Menyimpan...' : 'Buat'}
					</button>
				</div>
			</form>
		</section>
	</div>
{/if}

{#if showUpdate && canUpdate && selectedMajor}
	<div class="modal-backdrop" role="presentation" onclick={() => showUpdate = false}>
		<section class="modal-panel" role="dialog" aria-modal="true" tabindex="-1" onclick={(event) => event.stopPropagation()} onkeydown={(event) => event.stopPropagation()}>
			<div class="modal-head">
				<div>
					<h3>Update Jurusan</h3>
					<p>ID {selectedMajor.id} tetap dipertahankan, hanya nama yang diubah.</p>
				</div>
				<button class="btn btn-ghost icon-btn" aria-label="Tutup modal" type="button" onclick={() => showUpdate = false}>
					<Icons name="x" size={18} />
				</button>
			</div>
			<form class="modal-form" onsubmit={(event) => { event.preventDefault(); void updateMajor(); }}>
				<label>
					<span>Nama Jurusan</span>
					<input bind:value={patchName} required />
				</label>
				<div class="modal-actions">
					<button class="btn btn-ghost" type="button" onclick={() => showUpdate = false}>Batal</button>
					<button class="btn btn-primary" type="submit" disabled={busy === 'update'}>
						{busy === 'update' ? 'Menyimpan...' : 'Update'}
					</button>
				</div>
			</form>
		</section>
	</div>
{/if}

<ConfirmModal
	open={confirmState.open}
	title={confirmState.title}
	message={confirmState.message}
	confirmLabel={confirmState.confirmLabel}
	onConfirm={runConfirm}
	onCancel={closeConfirm}
/>

<style>
	.toolbar {
		display: flex;
		align-items: flex-end;
		justify-content: space-between;
		gap: 1rem;
		margin-bottom: 1.2rem;
	}

	.major-filter {
		display: flex;
		align-items: flex-end;
		gap: 0.75rem;
		flex: 1;
	}

	.major-filter label {
		display: grid;
		gap: 0.35rem;
		flex: 1;
	}

	.major-filter span,
	.modal-form span {
		color: var(--text-muted);
		font-size: 0.75rem;
		font-weight: 700;
	}

	.major-grid {
		display: grid;
		grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
		gap: 0.9rem;
	}

	.major-card {
		position: relative;
		display: grid;
		grid-template-columns: auto 1fr;
		gap: 0.85rem;
		padding: 1rem;
		background: var(--bg-surface);
		border: 1px solid var(--border);
		border-radius: var(--radius-md);
		box-shadow: var(--shadow-sm);
	}

	.major-card:hover {
		border-color: var(--primary-border);
		transform: translateY(-1px);
	}

	.major-code {
		display: grid;
		place-items: center;
		width: 52px;
		height: 52px;
		border-radius: 16px;
		background: var(--primary-soft);
		color: var(--primary);
		font-size: 1rem;
		font-weight: 900;
		letter-spacing: 0.05em;
	}

	.major-body {
		display: grid;
		gap: 0.7rem;
		min-width: 0;
	}

	.major-body h3 {
		margin: 0;
		font-size: 1rem;
	}

	.major-body p {
		margin: 0.2rem 0 0;
		color: var(--text-muted);
		font-size: 0.75rem;
	}

	.major-actions {
		position: absolute;
		top: 0.7rem;
		right: 0.7rem;
		display: flex;
		gap: 0.25rem;
	}

	.icon-btn {
		width: 38px;
		height: 38px;
		padding: 0;
	}

	.danger {
		color: var(--error);
	}

	.major-skeleton {
		min-height: 116px;
		border-radius: var(--radius-md);
		background: var(--border-light);
		animation: pulse 1.2s ease-in-out infinite alternate;
	}

	@keyframes pulse {
		to { opacity: 0.55; }
	}

	@media (max-width: 680px) {
		.toolbar,
		.major-filter {
			display: grid;
			grid-template-columns: 1fr;
		}

		.toolbar .btn,
		.major-filter .btn {
			width: 100%;
		}
	}
</style>
