<script lang="ts">
	import { onMount } from 'svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import AccessPanel from '$lib/components/AccessPanel.svelte';
	import Notice from '$lib/components/Notice.svelte';
	import PageTitle from '$lib/components/PageTitle.svelte';
	import Icons from '$lib/components/Icons.svelte';
	import Pagination from '$lib/components/Pagination.svelte';
	import { api, hasAnyAuthority, pageItems, paginationMeta, readSession } from '$lib/api';
	import type { PageData, PaginationMeta, Subject } from '$lib/types';

	let subjects = $state<Subject[]>([]);
	let pageMeta = $state<PaginationMeta>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
	let name = $state('');
	let patchId = $state('');
	let patchName = $state('');
	let error = $state('');
	let success = $state('');

	onMount(() => {
		if (hasAnyAuthority(readSession(), ['subject.*'])) void load();
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

<AccessPanel authorities={['subject.*']}>
	<div style="display: grid; grid-template-columns: 1fr 350px; gap: 1.5rem; align-items: start;">
		<section>
			<h3 class="mb-4">Daftar Subjek</h3>
			<div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 1rem;">
				{#each subjects as subject}
					<div class="card" style="margin-bottom: 0; padding: 1rem; position: relative;">
						<div class="flex items-center gap-3">
							<div style="width: 32px; height: 32px; background: var(--primary-light); color: var(--primary); border-radius: 6px; display: grid; place-items: center;">
								<Icons name="book" size={16} />
							</div>
							<div>
								<div style="font-weight: 700;">{subject.name}</div>
								<div style="font-size: 0.75rem; color: var(--text-muted);">ID #{subject.id}</div>
							</div>
						</div>
						<button class="btn btn-ghost btn-sm" style="position: absolute; top: 0.5rem; right: 0.5rem; color: var(--error);" onclick={() => submit(async () => { if(confirm('Hapus subjek?')) { await api<null>(`/api/subjects/${subject.id}`, { method: 'DELETE' }); await load(); success = 'Subjek dihapus.'; } })}>
							<Icons name="x" size={14} />
						</button>
					</div>
				{:else}
					<div style="grid-column: 1 / -1;"><EmptyState text="Belum ada data subjek." /></div>
				{/each}
			</div>
			<Pagination meta={pageMeta} onPage={changePage} onSize={changePageSize} />
		</section>

		<aside>
			<div class="card">
				<h3 class="mb-4">Tambah Subjek</h3>
				<form class="flex flex-direction-column gap-4" onsubmit={(event) => { event.preventDefault(); void submit(async () => { await api<Subject>('/api/subjects', { method: 'POST', body: JSON.stringify({ name }) }); name = ''; await load(); success = 'Subjek berhasil ditambahkan.'; }); }}>
					<div class="form-group">
						<label for="subjectName">Nama Subjek</label>
						<input id="subjectName" bind:value={name} placeholder="Contoh: Pemrograman Java" required />
					</div>
					<button class="btn btn-primary w-full" type="submit">Tambah</button>
				</form>
			</div>

			<div class="card mt-4">
				<h3 class="mb-4">Update Subjek</h3>
				<form class="flex flex-direction-column gap-4" onsubmit={(event) => { event.preventDefault(); void submit(async () => { await api<Subject>(`/api/subjects/${patchId}`, { method: 'PATCH', body: JSON.stringify({ name: patchName }) }); patchId = ''; patchName = ''; await load(); success = 'Subjek berhasil diupdate.'; }); }}>
					<div class="form-group">
						<label for="patchSubjectId">Subject ID</label>
						<input id="patchSubjectId" bind:value={patchId} type="number" required />
					</div>
					<div class="form-group">
						<label for="patchSubjectName">Nama Baru</label>
						<input id="patchSubjectName" bind:value={patchName} required />
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
	.btn-sm { padding: 0.25rem; }
</style>
