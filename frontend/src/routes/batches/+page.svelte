<script lang="ts">
	import { onMount } from 'svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import AccessPanel from '$lib/components/AccessPanel.svelte';
	import Notice from '$lib/components/Notice.svelte';
	import PageTitle from '$lib/components/PageTitle.svelte';
	import Icons from '$lib/components/Icons.svelte';
	import Pagination from '$lib/components/Pagination.svelte';
	import { api, hasAnyAuthority, pageItems, paginationMeta, readSession } from '$lib/api';
	import type { Batch, PageData, PaginationMeta } from '$lib/types';

	let batches = $state<Batch[]>([]);
	let pageMeta = $state<PaginationMeta>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
	let name = $state('');
	let id = $state('');
	let patchId = $state('');
	let patchName = $state('');
	let error = $state('');
	let success = $state('');

	onMount(() => {
		if (hasAnyAuthority(readSession(), ['batch.*'])) void load();
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

<AccessPanel authorities={['batch.*']}>
	<div style="display: grid; grid-template-columns: 1fr 350px; gap: 1.5rem; align-items: start;">
		<section>
			<div class="card" style="padding: 1rem; margin-bottom: 1.5rem;">
				<form class="flex gap-4 items-center" onsubmit={(event) => { event.preventDefault(); applyFilters(); }}>
					<div style="flex: 1;">
						<input bind:value={name} placeholder="Cari nama batch..." />
					</div>
					<button class="btn btn-primary" type="submit">
						<Icons name="home" size={18} />
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
							<th class="text-right">Aksi</th>
						</tr>
					</thead>
					<tbody>
						{#each batches as batch}
							<tr>
								<td><strong>#{batch.id}</strong></td>
								<td>{batch.name ?? `Batch ${batch.id}`}</td>
								<td><span class="badge badge-blue">{batch.userCount ?? 0} Users</span></td>
								<td class="text-right">
									<button class="btn btn-ghost" style="color: var(--error);" onclick={() => submit(async () => { if(confirm('Hapus batch?')) { await api<null>(`/api/batches/${batch.id}`, { method: 'DELETE' }); await load(); success = 'Batch dihapus.'; } })}>
										<Icons name="x" size={16} />
									</button>
								</td>
							</tr>
						{:else}
							<tr><td colspan="4"><EmptyState text="Data batch tidak ditemukan." /></td></tr>
						{/each}
					</tbody>
				</table>
			</div>
			<Pagination meta={pageMeta} onPage={changePage} onSize={changePageSize} />
		</section>

		<aside>
			<div class="card">
				<h3 class="mb-4">Buat Batch Baru</h3>
				<form class="flex flex-direction-column gap-4" onsubmit={(event) => { event.preventDefault(); void submit(async () => { await api<Batch>('/api/batches', { method: 'POST', body: JSON.stringify({ id: Number(id) }) }); id = ''; await load(); success = 'Batch berhasil dibuat.'; }); }}>
					<div class="form-group">
						<label for="createBatchId">Batch ID (Numerik)</label>
						<input id="createBatchId" bind:value={id} type="number" placeholder="Contoh: 25" required />
					</div>
					<button class="btn btn-primary w-full" type="submit">Buat</button>
				</form>
			</div>

			<div class="card mt-4">
				<h3 class="mb-4">Update Nama Batch</h3>
				<form class="flex flex-direction-column gap-4" onsubmit={(event) => { event.preventDefault(); void submit(async () => { await api<Batch>(`/api/batches/${patchId}`, { method: 'PATCH', body: JSON.stringify({ name: patchName }) }); patchId = ''; patchName = ''; await load(); success = 'Batch berhasil diupdate.'; }); }}>
					<div class="form-group">
						<label for="patchBatchId">Batch ID</label>
						<input id="patchBatchId" bind:value={patchId} type="number" required />
					</div>
					<div class="form-group">
						<label for="patchBatchName">Nama Alias</label>
						<input id="patchBatchName" bind:value={patchName} placeholder="Contoh: Batch 2026 A" required />
					</div>
					<button class="btn btn-secondary w-full" type="submit">Update</button>
				</form>
			</div>
		</aside>
	</div>
</AccessPanel>

<style>
	h3 { font-size: 1.125rem; }
	.flex-direction-column { flex-direction: column; }
</style>
