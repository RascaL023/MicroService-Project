<script lang="ts">
	import { onMount } from 'svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import AccessPanel from '$lib/components/AccessPanel.svelte';
	import ConfirmModal from '$lib/components/ConfirmModal.svelte';
	import Notice from '$lib/components/Notice.svelte';
	import PageTitle from '$lib/components/PageTitle.svelte';
	import Icons from '$lib/components/Icons.svelte';
	import Pagination from '$lib/components/Pagination.svelte';
	import { api, hasAnyAuthority, pageItems, paginationMeta, readSession } from '$lib/api';
	import type { LoginData, PageData, PaginationMeta, Subject } from '$lib/types';

	let subjects = $state<Subject[]>([]);
	let pageMeta = $state<PaginationMeta>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
	let session = $state<LoginData | null>(null);
	let name = $state('');
	let selectedSubject = $state<Subject | null>(null);
	let patchName = $state('');
	let showCreate = $state(false);
	let showUpdate = $state(false);
	let confirmState = $state({ open: false, title: '', message: '', confirmLabel: 'Ya, lanjutkan' });
	let pendingConfirm: (() => Promise<void>) | null = null;
	let error = $state('');
	let success = $state('');

	const canCreate = $derived(hasAnyAuthority(session, ['subject.create', 'subject.*']));
	const canUpdate = $derived(hasAnyAuthority(session, ['subject.update', 'subject.*']));
	const canDelete = $derived(hasAnyAuthority(session, ['subject.delete', 'subject.*']));

	onMount(() => {
		const nextSession = readSession();
		session = nextSession;
		if (hasAnyAuthority(nextSession, ['subject.read', 'subject.*'])) void load();
	});

	async function load() {
		const query = new URLSearchParams({
			page: String(pageMeta.page),
			size: String(pageMeta.size),
			sort: 'name,asc'
		});
		if (name.trim()) query.set('name', name.trim());
		const payload = await api<PageData<Subject> | Subject[]>(`/api/subjects?${query}`);
		subjects = pageItems(payload);
		pageMeta = paginationMeta(payload, pageMeta);
	}

	async function submit(task: () => Promise<void>) {
		error = ''; success = '';
		try { await task(); } catch (err) { error = err instanceof Error ? err.message : 'Request gagal.'; }
	}

	async function createSubject() {
		await submit(async () => {
			await api<Subject>('/api/subjects', { method: 'POST', body: JSON.stringify({ name }) });
			name = '';
			showCreate = false;
			await load();
			success = 'Subjek berhasil ditambahkan.';
		});
	}

	function openUpdateModal(subject: Subject) {
		selectedSubject = subject;
		patchName = subject.name;
		showUpdate = true;
	}

	async function updateSubject() {
		if (!selectedSubject) return;
		const id = selectedSubject.id;
		await submit(async () => {
			await api<Subject>(`/api/subjects/${id}`, { method: 'PATCH', body: JSON.stringify({ name: patchName }) });
			selectedSubject = null;
			patchName = '';
			showUpdate = false;
			await load();
			success = 'Subjek berhasil diupdate.';
		});
	}

	async function deleteSubject(subject: Subject) {
		askConfirm({
			title: 'Hapus subjek?',
			message: `Subjek ${subject.name} akan dihapus dari katalog.`,
			confirmLabel: 'Hapus Subjek'
		}, async () => {
			await submit(async () => {
				await api<null>(`/api/subjects/${subject.id}`, { method: 'DELETE' });
				await load();
				success = 'Subjek dihapus.';
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

	function changePage(page: number) {
		pageMeta = { ...pageMeta, page };
		void load();
	}

	function changePageSize(size: number) {
		pageMeta = { ...pageMeta, page: 0, size };
		void load();
	}

	function applyFilters() {
		pageMeta = { ...pageMeta, page: 0 };
		void load();
	}
</script>

<svelte:head><title>Subjects - Divdik Course</title></svelte:head>

<PageTitle eyebrow="Course Catalog" title="Master Subjek" description="Kelola daftar mata pelajaran atau subjek kursus yang tersedia di sistem." />

<Notice {error} {success} />

<AccessPanel authorities={['subject.read', 'subject.*']}>
	<section class="subject-toolbar" aria-label="Filter dan aksi subjek">
		<form class="subject-filter" onsubmit={(event) => { event.preventDefault(); applyFilters(); }}>
			<label>
				<span>Cari subject</span>
				<input bind:value={name} placeholder="Contoh: Java, Logika Algoritma" />
			</label>
			<button class="btn btn-secondary filter-submit" type="submit">
				<Icons name="search" size={17} />
				<span>Cari</span>
			</button>
		</form>

		{#if canCreate}
			<button class="btn btn-primary" type="button" onclick={() => showCreate = true}>
				<Icons name="book" size={17} />
				<span>Tambah Subjek</span>
			</button>
		{/if}
	</section>
	
	<section>
		<div class="subject-grid">
			{#each subjects as subject}
				<article class="subject-card">
					<div class="subject-main">
						<div class="subject-icon">
							<Icons name="book" size={18} />
						</div>
						<div>
							<div class="subject-name">{subject.name}</div>
							<div class="subject-id">ID #{subject.id}</div>
						</div>
					</div>
					{#if canUpdate || canDelete}
						<div class="subject-actions">
							{#if canUpdate}
								<button class="btn btn-ghost btn-sm" aria-label="Edit subjek" type="button" onclick={() => openUpdateModal(subject)}>
									<Icons name="edit" size={14} />
								</button>
							{/if}
							{#if canDelete}
								<button class="btn btn-ghost btn-sm danger" aria-label="Hapus subjek" type="button" onclick={() => deleteSubject(subject)}>
									<Icons name="x" size={14} />
								</button>
							{/if}
						</div>
					{/if}
				</article>
			{:else}
				<div style="grid-column: 1 / -1;"><EmptyState text="Belum ada data subjek." /></div>
			{/each}
		</div>
		<Pagination meta={pageMeta} onPage={changePage} onSize={changePageSize} />
	</section>
</AccessPanel>

{#if showCreate && canCreate}
	<div class="modal-backdrop" role="presentation" onclick={() => showCreate = false}>
		<section class="modal-panel" role="dialog" aria-modal="true" tabindex="-1" onclick={(event) => event.stopPropagation()} onkeydown={(event) => event.stopPropagation()}>
			<div class="modal-head">
				<div>
					<h3>Tambah Subjek</h3>
					<p>Tambahkan mata pelajaran baru ke katalog kursus.</p>
				</div>
				<button class="btn btn-ghost btn-icon" aria-label="Tutup modal" type="button" onclick={() => showCreate = false}>
					<Icons name="x" size={18} />
				</button>
			</div>
			<form class="modal-form" onsubmit={(event) => { event.preventDefault(); void createSubject(); }}>
				<div class="form-group">
					<label for="subjectName">Nama Subjek</label>
					<input id="subjectName" bind:value={name} placeholder="Contoh: Pemrograman Java" required />
				</div>
				<div class="modal-actions">
					<button class="btn btn-ghost" type="button" onclick={() => showCreate = false}>Batal</button>
					<button class="btn btn-primary" type="submit">Tambah</button>
				</div>
			</form>
		</section>
	</div>
{/if}

{#if showUpdate && canUpdate && selectedSubject}
	<div class="modal-backdrop" role="presentation" onclick={() => showUpdate = false}>
		<section class="modal-panel" role="dialog" aria-modal="true" tabindex="-1" onclick={(event) => event.stopPropagation()} onkeydown={(event) => event.stopPropagation()}>
			<div class="modal-head">
				<div>
					<h3>Update Subjek</h3>
					<p>Ubah nama subjek {selectedSubject.name}.</p>
				</div>
				<button class="btn btn-ghost btn-icon" aria-label="Tutup modal" type="button" onclick={() => showUpdate = false}>
					<Icons name="x" size={18} />
				</button>
			</div>
			<form class="modal-form" onsubmit={(event) => { event.preventDefault(); void updateSubject(); }}>
				<div class="form-group">
					<label for="patchSubjectName">Nama Baru</label>
					<input id="patchSubjectName" bind:value={patchName} required />
				</div>
				<div class="modal-actions">
					<button class="btn btn-ghost" type="button" onclick={() => showUpdate = false}>Batal</button>
					<button class="btn btn-primary" type="submit">Update</button>
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
	.subject-toolbar {
		display: flex;
		align-items: flex-end;
		justify-content: space-between;
		gap: 1rem;
		margin-bottom: 1.2rem;
	}

	.subject-filter {
		display: flex;
		align-items: flex-end;
		gap: 0.75rem;
		flex: 1;
	}

	.subject-filter label {
		display: grid;
		gap: 0.35rem;
		flex: 1;
	}

	.subject-filter span {
		color: var(--text-muted);
		font-size: 0.75rem;
		font-weight: 700;
	}

	.subject-grid {
		display: grid;
		grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
		gap: 0.9rem;
	}

	.subject-card {
		position: relative;
		display: grid;
		grid-template-columns: minmax(0, 1fr) auto;
		align-items: flex-start;
		gap: 0.8rem;
		min-height: 104px;
		padding: 1rem;
		background: var(--bg-surface);
		border: 1px solid var(--border);
		border-radius: var(--radius-md);
		box-shadow: var(--shadow-sm);
	}

	.subject-card:hover {
		border-color: var(--primary-border);
		transform: translateY(-1px);
	}

	.subject-main {
		display: flex;
		align-items: flex-start;
		gap: 0.85rem;
		min-width: 0;
	}

	.subject-main > div:last-child {
		min-width: 0;
	}
    
	.subject-actions {
		display: flex;
		gap: 0.25rem;
		flex: 0 0 auto;
	}

	.subject-icon {
		width: 46px;
		height: 46px;
		background: var(--primary-soft);
		color: var(--primary);
		border-radius: 14px;
		display: grid;
		place-items: center;
		flex: 0 0 auto;
	}
    
	.subject-name {
		font-weight: 700;
		line-height: 1.3;
		overflow-wrap: anywhere;
		word-break: break-word;
	}
    
	.subject-id {
		font-size: 0.75rem;
		color: var(--text-muted);
	}
    
	.btn-sm,
	.btn-icon {
		width: 38px;
		height: 38px;
		padding: 0;
	}
    
	.btn-sm {
		display: flex;
		justify-content: center;
		align-items: center;
	}
    
	.danger {
		color: var(--error);
	}

	@media (max-width: 560px) {
		.subject-toolbar,
		.subject-filter {
			display: grid;
			grid-template-columns: 1fr;
		}

		.subject-toolbar .btn,
		.subject-filter .btn {
			width: 100%;
		}

		.subject-grid {
			grid-template-columns: 1fr;
		}
	}
</style>
