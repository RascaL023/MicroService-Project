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
	import type { Batch, LoginData, PageData, PaginationMeta } from '$lib/types';

	let batches = $state<Batch[]>([]);
	let pageMeta = $state<PaginationMeta>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
	let session = $state<LoginData | null>(null);
	let name = $state('');
	let id = $state('');
	let patchId = $state('');
	let patchName = $state('');
	let showCreate = $state(false);
	let showUpdate = $state(false);
	let confirmState = $state({ open: false, title: '', message: '', confirmLabel: 'Ya, lanjutkan' });
	let pendingConfirm: (() => Promise<void>) | null = null;
	let error = $state('');
	let success = $state('');

	const canCreate = $derived(hasAnyAuthority(session, ['batch.create', 'batch.*']));
	const canUpdate = $derived(hasAnyAuthority(session, ['batch.update', 'batch.*']));
	const canDelete = $derived(hasAnyAuthority(session, ['batch.delete', 'batch.*']));

	onMount(() => {
		const nextSession = readSession();
		session = nextSession;
		if (hasAnyAuthority(nextSession, ['batch.read', 'batch.*'])) void load();
	});

	async function load() {
		const query = new URLSearchParams({
			page: String(pageMeta.page),
			size: String(pageMeta.size),
			sort: 'id,asc'
		});
		if (name) query.set('name', name);
		const payload = await api<PageData<Batch> | Batch[]>(`/api/batches?${query}`);
		batches = pageItems(payload);
		pageMeta = paginationMeta(payload, pageMeta);
	}

	async function submit(task: () => Promise<void>) {
		error = ''; success = '';
		try { await task(); } catch (err) { error = err instanceof Error ? err.message : 'Request gagal.'; }
	}

	async function createBatch() {
		await submit(async () => {
			await api<Batch>('/api/batches', { method: 'POST', body: JSON.stringify({ id: Number(id) }) });
			id = '';
			showCreate = false;
			await load();
			success = 'Batch berhasil dibuat.';
		});
	}

	async function updateBatch() {
		await submit(async () => {
			await api<Batch>(`/api/batches/${patchId}`, { method: 'PATCH', body: JSON.stringify({ name: patchName }) });
			patchId = '';
			patchName = '';
			showUpdate = false;
			await load();
			success = 'Batch berhasil diupdate.';
		});
	}

	async function deleteBatch(batch: Batch) {
		askConfirm({
			title: 'Hapus batch?',
			message: `Batch ${batch.name ?? batch.id} akan dihapus dari sistem.`,
			confirmLabel: 'Hapus Batch'
		}, async () => {
			await submit(async () => {
				await api<null>(`/api/batches/${batch.id}`, { method: 'DELETE' });
				await load();
				success = 'Batch dihapus.';
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

<svelte:head><title>Batches - Divdik Course</title></svelte:head>

<PageTitle eyebrow="User Management" title="Angkatan (Batches)" description="Kelola angkatan user untuk pengelompokan peserta berdasarkan periode pendaftaran." />

<Notice {error} {success} />

<AccessPanel authorities={['batch.read', 'batch.*']}>
	{#if canCreate || canUpdate}
		<section class="page-actions" aria-label="Aksi batch">
			{#if canUpdate}
				<button class="btn btn-secondary" type="button" onclick={() => showUpdate = true}>
					<Icons name="edit" size={17} />
					<span>Update Batch</span>
				</button>
			{/if}
			{#if canCreate}
				<button class="btn btn-primary" type="button" onclick={() => showCreate = true}>
					<Icons name="home" size={17} />
					<span>Buat Batch</span>
				</button>
			{/if}
		</section>
	{/if}

	<section>
		<div class="card" style="padding: 1rem; margin-bottom: 1.5rem;">
			<form class="batch-filter" onsubmit={(event) => { event.preventDefault(); applyFilters(); }}>
				<div style="flex: 1;">
					<input bind:value={name} placeholder="Cari nama batch..." />
				</div>
				<button class="btn btn-primary filter-submit" type="submit">
					<Icons name="search" size={18} />
					<span>Cari</span>
				</button>
			</form>
		</div>

		<div class="table-container">
			<table>
				<thead>
					<tr>
						<th>ID</th>
						<th>Nama Batch</th>
						<th>User Count</th>
						{#if canDelete}<th class="text-right">Aksi</th>{/if}
					</tr>
				</thead>
				<tbody>
					{#each batches as batch}
						<tr>
							<td><strong>#{batch.id}</strong></td>
							<td>{batch.name ?? `Batch ${batch.id}`}</td>
							<td><span class="badge badge-blue">{batch.userCount ?? 0} Users</span></td>
							{#if canDelete}
								<td class="text-right">
									<button class="btn btn-ghost danger" type="button" onclick={() => deleteBatch(batch)}>
										<Icons name="x" size={16} />
									</button>
								</td>
							{/if}
						</tr>
					{:else}
						<tr><td colspan={canDelete ? 4 : 3}><EmptyState text="Data batch tidak ditemukan." /></td></tr>
					{/each}
				</tbody>
			</table>
		</div>
		<Pagination meta={pageMeta} onPage={changePage} onSize={changePageSize} />
	</section>
</AccessPanel>

{#if showCreate && canCreate}
	<div class="modal-backdrop" role="presentation" onclick={() => showCreate = false}>
		<section class="modal-panel" role="dialog" aria-modal="true" tabindex="-1" onclick={(event) => event.stopPropagation()} onkeydown={(event) => event.stopPropagation()}>
			<div class="modal-head">
				<div>
					<h3>Buat Batch Baru</h3>
					<p>Tambahkan angkatan berdasarkan ID numerik.</p>
				</div>
				<button class="btn btn-ghost btn-icon" aria-label="Tutup modal" type="button" onclick={() => showCreate = false}>
					<Icons name="x" size={18} />
				</button>
			</div>
			<form class="modal-form" onsubmit={(event) => { event.preventDefault(); void createBatch(); }}>
				<div class="form-group">
					<label for="createBatchId">Batch ID (Numerik)</label>
					<input id="createBatchId" bind:value={id} type="number" placeholder="Contoh: 25" required />
				</div>
				<div class="modal-actions">
					<button class="btn btn-ghost" type="button" onclick={() => showCreate = false}>Batal</button>
					<button class="btn btn-primary" type="submit">Buat</button>
				</div>
			</form>
		</section>
	</div>
{/if}

{#if showUpdate && canUpdate}
	<div class="modal-backdrop" role="presentation" onclick={() => showUpdate = false}>
		<section class="modal-panel" role="dialog" aria-modal="true" tabindex="-1" onclick={(event) => event.stopPropagation()} onkeydown={(event) => event.stopPropagation()}>
			<div class="modal-head">
				<div>
					<h3>Update Nama Batch</h3>
					<p>Ubah alias batch tanpa mengubah ID batch.</p>
				</div>
				<button class="btn btn-ghost btn-icon" aria-label="Tutup modal" type="button" onclick={() => showUpdate = false}>
					<Icons name="x" size={18} />
				</button>
			</div>
			<form class="modal-form" onsubmit={(event) => { event.preventDefault(); void updateBatch(); }}>
				<div class="form-group">
					<label for="patchBatchId">Batch ID</label>
					<input id="patchBatchId" bind:value={patchId} type="number" required />
				</div>
				<div class="form-group">
					<label for="patchBatchName">Nama Alias</label>
					<input id="patchBatchName" bind:value={patchName} placeholder="Contoh: Batch 2026 A" required />
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

	.batch-filter {
		display: flex;
		align-items: center;
		gap: 1rem;
	}

	.batch-filter .btn {
		min-height: 44px;
	}

	.btn-icon {
		width: 38px;
		height: 38px;
		padding: 0;
	}

	.danger {
		color: var(--error);
	}

	@media (max-width: 560px) {
		.batch-filter {
			display: grid;
			grid-template-columns: 1fr;
		}

		.batch-filter .btn,
		.page-actions .btn {
			width: 100%;
		}
	}
</style>
