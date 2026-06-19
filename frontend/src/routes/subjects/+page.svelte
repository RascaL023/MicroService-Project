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
</script>

<svelte:head><title>Subjects - Divdik Course</title></svelte:head>

<PageTitle eyebrow="Course Catalog" title="Master Subjek" description="Kelola daftar mata pelajaran atau subjek kursus yang tersedia di sistem." />

<Notice {error} {success} />

<AccessPanel authorities={['subject.read', 'subject.*']}>
	<section class="page-actions" aria-label="Aksi subjek">
		{#if canCreate}
			<button class="btn btn-primary" type="button" onclick={() => showCreate = true}>
				<Icons name="book" size={17} />
				<span>Tambah Subjek</span>
			</button>
		{/if}
	</section>
	
	<section>
		<h3 class="mb-4">Daftar Subjek</h3>
		<div class="subject-grid">
			{#each subjects as subject}
				<div class="card subject-card">
					<div class="flex items-center gap-3">
						<div class="subject-icon">
							<Icons name="book" size={16} />
						</div>
						<div>
							<div class="subject-name">{subject.name}</div>
							<div class="subject-id">ID #{subject.id}</div>
						</div>
					</div>
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
				</div>
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
	h3 { font-size: 1.125rem; }

	.subject-grid {
		display: grid;
		grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
		gap: 1rem;
	}

	.subject-card {
		position: relative;
		margin-bottom: 0;
		padding: 1rem;
		display: flex;
		justify-content: space-between;
		align-items: center;
	}
    
	.subject-actions {
		display: flex;
		gap: 0.25rem;
	}

	.subject-icon {
		width: 32px;
		height: 32px;
		background: var(--primary-light);
		color: var(--primary);
		border-radius: 6px;
		display: grid;
		place-items: center;
	}
    
	.subject-name {
		font-weight: 700;
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
		.page-actions .btn {
			width: 100%;
		}
	}
</style>
