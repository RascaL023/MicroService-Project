<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import AccessPanel from '$lib/components/AccessPanel.svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import Icons from '$lib/components/Icons.svelte';
	import Notice from '$lib/components/Notice.svelte';
	import PageTitle from '$lib/components/PageTitle.svelte';
	import Pagination from '$lib/components/Pagination.svelte';
	import { api, hasAnyAuthority, pageItems, paginationMeta, readSession } from '$lib/api';
	import type { Group, GroupCompleteResult, LoginData, PageData, PaginationMeta, Subject } from '$lib/types';

	let subjects = $state<Subject[]>([]);
	let groups = $state<Group[]>([]);
	let pageMeta = $state<PaginationMeta>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
	let session = $state<LoginData | null>(null);
	let filters = $state({ name: '', subjectId: '', academicYear: '', status: 'ON_GOING' });
	let form = $state({ name: '', subjectId: '', academicYear: '2026/2027' });
	let completeForm = $state({ subjectId: '', academicYear: '2026/2027' });
	let showCreate = $state(false);
	let showComplete = $state(false);
	let error = $state('');
	let success = $state('');
	let loading = $state(true);
	let completing = $state(false);

	const canManage = $derived(hasAnyAuthority(session, ['group.create', 'group.update', 'group.delete', 'group.*']));

	onMount(() => {
		session = readSession();
		void loadPage();
	});

	async function submit(task: () => Promise<void>) {
		error = '';
		success = '';
		try {
			await task();
		} catch (err) {
			error = err instanceof Error ? err.message : 'Request gagal.';
		}
	}

	async function loadPage() {
		loading = true;
		try {
			await Promise.all([loadSubjects(), loadGroups()]);
		} catch (err) {
			error = err instanceof Error ? err.message : 'Gagal memuat group.';
		} finally {
			loading = false;
		}
	}

	async function loadSubjects() {
		const payload = await api<PageData<Subject> | Subject[]>('/api/subjects?sort=name,asc');
		subjects = pageItems(payload);
		if (!form.subjectId && subjects[0]) form.subjectId = String(subjects[0].id);
		if (!completeForm.subjectId && subjects[0]) completeForm.subjectId = String(subjects[0].id);
	}

	async function loadGroups() {
		const query = new URLSearchParams({
			page: String(pageMeta.page),
			size: String(pageMeta.size),
			sort: 'name,asc'
		});
		if (filters.name.trim()) query.set('name', filters.name.trim());
		if (filters.subjectId) query.set('subjectId', filters.subjectId);
		if (filters.academicYear.trim()) query.set('academicYear', filters.academicYear.trim());
		if (filters.status) query.set('status', filters.status);

		const payload = await api<PageData<Group> | Group[]>(`/api/groups?${query}`);
		groups = pageItems(payload);
		pageMeta = paginationMeta(payload, pageMeta);
	}

	async function createGroup() {
		await submit(async () => {
			await api<Group>('/api/groups', {
				method: 'POST',
				body: JSON.stringify({
					name: form.name,
					subjectId: Number(form.subjectId),
					academicYear: form.academicYear
				})
			});
			form = { name: '', subjectId: form.subjectId, academicYear: form.academicYear };
			showCreate = false;
			await loadGroups();
			success = 'Group berhasil dibuat.';
		});
	}

	function openComplete() {
		completeForm = {
			subjectId: filters.subjectId || form.subjectId || (subjects[0] ? String(subjects[0].id) : ''),
			academicYear: filters.academicYear || form.academicYear
		};
		showComplete = true;
	}

	async function completeGroups() {
		await submit(async () => {
			completing = true;
			try {
				const payload = await api<GroupCompleteResult>('/api/groups/complete', {
					method: 'PATCH',
					body: JSON.stringify({
						subjectId: Number(completeForm.subjectId),
						academicYear: completeForm.academicYear
					})
				});

				showComplete = false;
				await loadGroups();
				const result = payload.data;
				success = result
					? `${result.completedGroups} group ditandai passed, ${result.deletedSchedules} jadwal aktif dihapus.`
					: 'Periode group berhasil diselesaikan.';
			} finally {
				completing = false;
			}
		});
	}

	async function deleteGroup(group: Group) {
		if (!confirm(`Hapus ${group.name}?`)) return;
		await submit(async () => {
			await api<null>(`/api/groups/${group.id}`, { method: 'DELETE' });
			await loadGroups();
			success = 'Group berhasil dihapus.';
		});
	}

	function statusClass(status: string) {
		return status.toLowerCase().includes('jalan') || status === 'ON_GOING' ? 'badge-blue' : 'badge-green';
	}

	function applyFilters() {
		pageMeta = { ...pageMeta, page: 0 };
		void loadGroups();
	}

	function changePage(page: number) {
		pageMeta = { ...pageMeta, page };
		void loadGroups();
	}

	function changePageSize(size: number) {
		pageMeta = { ...pageMeta, page: 0, size };
		void loadGroups();
	}
</script>

<svelte:head><title>Groups - Divdik Course</title></svelte:head>

<div class="page-heading">
	<PageTitle
		eyebrow="Academic Operations"
		title="Group"
		description="Lihat kelompok belajar aktif secara ringkas. Buka detail untuk melihat jadwal dan member group."
	/>

	{#if canManage}
		<div class="page-actions">
			<button class="btn btn-secondary" type="button" onclick={openComplete}>
				<Icons name="checkCircle" size={17} />
				<span>Selesaikan Periode</span>
			</button>
			<button class="btn btn-primary" type="button" onclick={() => showCreate = true}>
				<Icons name="layers" size={17} />
				<span>Buat Group</span>
			</button>
		</div>
	{/if}
</div>

<Notice {error} {success} />

<AccessPanel authorities={['group.read', 'group.*']}>
	<section class="toolbar">
		<form class="filters" onsubmit={(event) => { event.preventDefault(); applyFilters(); }}>
			<label>
				<span>Nama</span>
				<input bind:value={filters.name} placeholder="Cari group..." />
			</label>
			<label>
				<span>Subject</span>
				<select bind:value={filters.subjectId}>
					<option value="">Semua subject</option>
					{#each subjects as subject}
						<option value={subject.id}>{subject.name}</option>
					{/each}
				</select>
			</label>
			<label>
				<span>Tahun</span>
				<input bind:value={filters.academicYear} placeholder="2026/2027" />
			</label>
			<label>
				<span>Status</span>
				<select bind:value={filters.status}>
					<option value="ON_GOING">On Going</option>
					<option value="PASSED">Passed</option>
					<option value="">Semua status</option>
				</select>
			</label>
			<button class="btn btn-primary" type="submit">
				<Icons name="search" size={17} />
				<span>Filter</span>
			</button>
		</form>
	</section>

	{#if loading}
		<div class="card">Memuat data group...</div>
	{:else if groups.length === 0}
		<EmptyState text="Belum ada group yang sesuai filter." />
	{:else}
		<section class="group-grid">
			{#each groups as group}
				<article class="group-card">
					<div class="group-card-main">
						<div class="group-icon"><Icons name="layers" size={21} /></div>
						<div class="group-text">
							<h3>{group.name}</h3>
							<p>{group.subjectName}</p>
						</div>
					</div>

					<div class="group-meta">
						<span>{group.academicYear}</span>
						<span class={`badge ${statusClass(group.status)}`}>{group.status}</span>
					</div>

					<div class="group-actions">
						<button class="btn btn-secondary" type="button" onclick={() => goto(`/groups/${group.id}`)}>
							<span>Detail</span>
							<Icons name="chevronRight" size={16} />
						</button>
						{#if canManage}
							<button class="btn btn-ghost danger" type="button" onclick={() => deleteGroup(group)}>
								<Icons name="x" size={17} />
							</button>
						{/if}
					</div>
				</article>
			{/each}
		</section>
		<Pagination meta={pageMeta} onPage={changePage} onSize={changePageSize} />
	{/if}
</AccessPanel>

{#if showCreate}
	<div class="modal-backdrop" role="presentation" onclick={() => showCreate = false}>
		<section
			class="modal-panel"
			role="dialog"
			aria-modal="true"
			tabindex="-1"
			onclick={(event) => event.stopPropagation()}
			onkeydown={(event) => event.stopPropagation()}
		>
			<div class="modal-head">
				<div>
					<span class="nav-label flush">Create</span>
					<h3>Buat Group</h3>
				</div>
				<button class="btn btn-ghost icon-btn" type="button" onclick={() => showCreate = false}>
					<Icons name="x" size={18} />
				</button>
			</div>

			<form class="modal-form" onsubmit={(event) => { event.preventDefault(); void createGroup(); }}>
				<label>
					<span>Nama Group</span>
					<input bind:value={form.name} placeholder="Contoh: Java A" required />
				</label>
				<label>
					<span>Subject</span>
					<select bind:value={form.subjectId} required>
						{#each subjects as subject}
							<option value={subject.id}>{subject.name}</option>
						{/each}
					</select>
				</label>
				<label>
					<span>Academic Year</span>
					<input bind:value={form.academicYear} placeholder="2026/2027" required />
				</label>
				<div class="modal-actions">
					<button class="btn btn-ghost" type="button" onclick={() => showCreate = false}>Batal</button>
					<button class="btn btn-primary" type="submit">Simpan</button>
				</div>
			</form>
		</section>
	</div>
{/if}

{#if showComplete}
	<div class="modal-backdrop" role="presentation" onclick={() => showComplete = false}>
		<section
			class="modal-panel complete-panel"
			role="dialog"
			aria-modal="true"
			tabindex="-1"
			onclick={(event) => event.stopPropagation()}
			onkeydown={(event) => event.stopPropagation()}
		>
			<div class="modal-head">
				<div>
					<span class="nav-label flush">Bulk Action</span>
					<h3>Selesaikan Periode Group</h3>
				</div>
				<button class="btn btn-ghost icon-btn" type="button" onclick={() => showComplete = false}>
					<Icons name="x" size={18} />
				</button>
			</div>

			<form class="modal-form" onsubmit={(event) => { event.preventDefault(); void completeGroups(); }}>
				<div class="warning-box">
					<Icons name="alertTriangle" size={18} />
					<p>
						Semua group aktif pada subject dan tahun yang dipilih akan berubah menjadi Passed.
						Jadwal aktif group tersebut akan dihapus permanen, sedangkan data member tetap disimpan.
					</p>
				</div>

				<label>
					<span>Subject</span>
					<select bind:value={completeForm.subjectId} required>
						{#each subjects as subject}
							<option value={subject.id}>{subject.name}</option>
						{/each}
					</select>
				</label>
				<label>
					<span>Academic Year</span>
					<input bind:value={completeForm.academicYear} placeholder="2026/2027" required />
				</label>

				<div class="modal-actions">
					<button class="btn btn-ghost" type="button" onclick={() => showComplete = false}>Batal</button>
					<button class="btn btn-primary" type="submit" disabled={completing || !completeForm.subjectId}>
						{completing ? 'Memproses...' : 'Selesaikan'}
					</button>
				</div>
			</form>
		</section>
	</div>
{/if}

<style>
	.toolbar {
		display: flex;
		align-items: end;
		justify-content: space-between;
		gap: 1rem;
		margin-bottom: 1.5rem;
	}

	.page-heading {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		gap: 1rem;
		margin-bottom: 1.25rem;
	}

	.page-heading :global(.page-header-container) {
		margin-bottom: 0;
		min-width: 0;
	}

	.filters {
		display: grid;
		grid-template-columns: minmax(180px, 1fr) minmax(170px, 220px) minmax(145px, 170px) minmax(145px, 170px) auto;
		gap: 0.875rem;
		align-items: end;
		flex: 1;
	}

	.page-actions {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		flex-wrap: wrap;
		justify-content: flex-end;
		padding-top: 0.35rem;
	}

	label {
		display: grid;
		gap: 0.375rem;
	}

	label span {
		font-size: 0.75rem;
		font-weight: 800;
		color: var(--text-muted);
	}

	.group-grid {
		display: grid;
		grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
		gap: 1rem;
	}

	.group-card {
		background: var(--bg-surface);
		border: 1px solid var(--border);
		border-radius: var(--radius);
		padding: 1.25rem;
		box-shadow: var(--shadow-sm);
		display: grid;
		gap: 1.25rem;
		min-height: 190px;
		min-width: 0;
	}

	.group-card-main {
		display: flex;
		gap: 0.875rem;
		align-items: flex-start;
	}

	.group-icon {
		width: 44px;
		height: 44px;
		border-radius: var(--radius-sm);
		background: var(--primary-soft);
		color: var(--primary);
		display: grid;
		place-items: center;
		flex: 0 0 auto;
	}

	.group-text h3 {
		font-size: 1.05rem;
		margin-bottom: 0.25rem;
		letter-spacing: 0;
		overflow-wrap: anywhere;
	}

	.group-text p {
		font-size: 0.875rem;
	}

	.group-meta,
	.group-actions {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 0.75rem;
		min-width: 0;
	}

	.group-meta span:first-child {
		color: var(--text-muted);
		font-size: 0.85rem;
		font-weight: 700;
	}

	.group-actions {
		margin-top: auto;
	}

	.danger {
		color: var(--error);
	}

	.icon-btn {
		width: 40px;
		height: 40px;
		padding: 0;
	}

	.modal-backdrop {
		position: fixed;
		inset: 0;
		background: var(--bg-overlay);
		display: grid;
		place-items: center;
		z-index: 100;
		padding: 1rem;
	}

	.modal-panel {
		width: min(520px, 100%);
		background: var(--bg-surface);
		border: 1px solid var(--border);
		border-radius: var(--radius);
		box-shadow: var(--shadow-lg);
		padding: 1.5rem;
	}

	.modal-head,
	.modal-actions {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 1rem;
	}

	.modal-form {
		display: grid;
		gap: 1rem;
		margin-top: 1.25rem;
	}

	.complete-panel {
		width: min(560px, 100%);
	}

	.warning-box {
		display: grid;
		grid-template-columns: auto 1fr;
		gap: 0.75rem;
		align-items: start;
		padding: 0.875rem;
		border: 1px solid color-mix(in srgb, var(--warning) 35%, var(--border));
		background: color-mix(in srgb, var(--warning) 10%, var(--bg-surface));
		border-radius: var(--radius-sm);
		color: var(--text-main);
	}

	.warning-box :global(svg) {
		color: var(--warning);
		margin-top: 0.125rem;
	}

	.warning-box p {
		font-size: 0.875rem;
		line-height: 1.5;
		margin: 0;
	}

	.flush {
		padding: 0;
		margin: 0 0 0.25rem;
	}

	@media (max-width: 900px) {
		.page-heading {
			display: grid;
		}

		.page-actions {
			justify-content: stretch;
		}

		.page-actions .btn {
			flex: 1;
		}

		.toolbar {
			display: grid;
			align-items: stretch;
		}

		.filters {
			grid-template-columns: repeat(2, minmax(0, 1fr));
		}
	}

	@media (max-width: 560px) {
		.page-actions,
		.filters {
			grid-template-columns: 1fr;
			display: grid;
		}

		.group-grid {
			grid-template-columns: minmax(0, 1fr);
		}

		.group-actions {
			display: grid;
			grid-template-columns: 1fr auto;
		}

		.page-actions .btn,
		.filters .btn {
			width: 100%;
		}
	}
</style>
