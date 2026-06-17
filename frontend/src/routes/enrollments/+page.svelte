<script lang="ts">
	import { onMount } from 'svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import AccessPanel from '$lib/components/AccessPanel.svelte';
	import Notice from '$lib/components/Notice.svelte';
	import PageTitle from '$lib/components/PageTitle.svelte';
	import Icons from '$lib/components/Icons.svelte';
	import Pagination from '$lib/components/Pagination.svelte';
	import { api, hasAnyAuthority, pageItems, paginationMeta, readSession } from '$lib/api';
	import type { Enrollment, Group, LoginData, PageData, PaginationMeta, Subject } from '$lib/types';

	let enrollments = $state<Enrollment[]>([]);
	let mine = $state<Enrollment[]>([]);
	let enrollmentPage = $state<PaginationMeta>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
	let minePage = $state<PaginationMeta>({ page: 0, size: 6, totalPages: 1, totalElements: 0 });
	let groups = $state<Group[]>([]);
	let subjects = $state<Subject[]>([]);
	let filters = $state({ userId: '', groupId: '', subjectId: '', academicYear: '', role: '' });
	let form = $state({ userId: '', groupId: '', role: 'LEARNER' });
	let patch = $state({ id: '', userId: '', groupId: '', role: '' });
	let error = $state('');
	let success = $state('');
	let session = $state<LoginData | null>(null);
	const canReadAll = $derived(hasAnyAuthority(session, ['enrollment.read', 'enrollment.*']));
	const canManage = $derived(hasAnyAuthority(session, ['enrollment.create', 'enrollment.update', 'enrollment.delete', 'enrollment.*']));

	onMount(() => {
		session = readSession();
		void loadAll();
	});

	async function submit(task: () => Promise<void>) {
		error = ''; success = '';
		try { await task(); } catch (err) { error = err instanceof Error ? err.message : 'Request gagal.'; }
	}

	async function loadAll() {
		const [groupsPayload, subjectsPayload] = await Promise.all([
			api<PageData<Group> | Group[]>('/api/groups?sort=name,asc'),
			api<PageData<Subject> | Subject[]>('/api/subjects?sort=name,asc')
		]);
		groups = pageItems(groupsPayload);
		subjects = pageItems(subjectsPayload);
		if (!form.groupId && groups[0]) form.groupId = String(groups[0].id);
		await Promise.all([canReadAll ? loadEnrollments() : Promise.resolve(), loadMine()]);
	}

	async function loadEnrollments() {
		const query = new URLSearchParams({
			page: String(enrollmentPage.page),
			size: String(enrollmentPage.size),
			sort: 'id,desc'
		});
		for (const [key, value] of Object.entries(filters)) if (value) query.set(key, value);
		const payload = await api<PageData<Enrollment> | Enrollment[]>(`/api/enrollments?${query}`);
		enrollments = pageItems(payload);
		enrollmentPage = paginationMeta(payload, enrollmentPage);
	}

	async function loadMine() {
		const query = new URLSearchParams({
			page: String(minePage.page),
			size: String(minePage.size),
			sort: 'id,desc'
		});
		const payload = await api<PageData<Enrollment> | Enrollment[]>(`/api/enrollments/me?${query}`);
		mine = pageItems(payload);
		minePage = paginationMeta(payload, minePage);
	}

	function applyFilters() {
		enrollmentPage = { ...enrollmentPage, page: 0 };
		void loadEnrollments();
	}

	function changeEnrollmentPage(page: number) {
		enrollmentPage = { ...enrollmentPage, page };
		void loadEnrollments();
	}

	function changeEnrollmentSize(size: number) {
		enrollmentPage = { ...enrollmentPage, page: 0, size };
		void loadEnrollments();
	}

	function changeMinePage(page: number) {
		minePage = { ...minePage, page };
		void loadMine();
	}

	function changeMineSize(size: number) {
		minePage = { ...minePage, page: 0, size };
		void loadMine();
	}
</script>

<svelte:head><title>Enrollments - Divdik Course</title></svelte:head>

<PageTitle eyebrow="Academic Management" title="Enrollments & Peran" description="Manajemen peran user (Instructor/Learner) dalam setiap group kursus." />

<Notice {error} {success} />

<AccessPanel authorities={['course.read', 'enrollment.read', 'enrollment.*']}>
	<!-- Session Enrollments -->
	<section class="mb-4">
		<h3 class="mb-4">Kursus Saya</h3>
		{#if mine.length}
			<div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 1rem;">
				{#each mine as item}
					<div class="card" style="margin-bottom: 0; padding: 1.25rem; border-top: 3px solid var(--primary);">
						<div style="font-weight: 700; font-size: 1.125rem;">{item.groupName}</div>
						<div style="color: var(--text-muted); font-size: 0.875rem; margin-top: 0.25rem;">{item.subjectName}</div>
						<div class="flex justify-between items-center mt-4">
							<span class="badge" class:badge-blue={item.userRole === 'INSTRUCTOR'} class:badge-green={item.userRole === 'LEARNER'}>
								{item.userRole}
							</span>
							<small>{item.academicYear}</small>
						</div>
					</div>
				{/each}
			</div>
			<Pagination meta={minePage} onPage={changeMinePage} onSize={changeMineSize} sizes={[6, 12, 24, 48]} />
		{:else}
			<div class="card" style="text-align: center; color: var(--text-muted);">
				Anda belum terdaftar di group manapun.
			</div>
		{/if}
	</section>

	<hr style="border: none; border-bottom: 1px solid var(--border); margin: 2rem 0;" />

	{#if canReadAll}
		<div style="display: grid; grid-template-columns: 1fr 350px; gap: 1.5rem; align-items: start;">
			<!-- List & Filters -->
			<section>
				<div class="card" style="padding: 1rem; margin-bottom: 1.5rem;">
					<form class="flex gap-4 items-center" onsubmit={(event) => { event.preventDefault(); applyFilters(); }}>
						<div style="flex: 1;">
							<input bind:value={filters.userId} placeholder="User ID..." />
						</div>
						<div style="width: 150px;">
							<select bind:value={filters.role}>
								<option value="">Semua Peran</option>
								<option value="INSTRUCTOR">Instructor</option>
								<option value="LEARNER">Learner</option>
							</select>
						</div>
						<button class="btn btn-primary" type="submit">
							<Icons name="home" size={18} />
							<span>Filter</span>
						</button>
					</form>
				</div>

				<div class="table-container">
					<table>
						<thead>
							<tr>
								<th>User</th>
								<th>Peran</th>
								<th>Group / Subjek</th>
								{#if canManage}<th class="text-right">Aksi</th>{/if}
							</tr>
						</thead>
						<tbody>
							{#each enrollments as item}
								<tr>
									<td>
										<div style="font-weight: 600;">{item.user?.name || `User #${item.user?.id}`}</div>
										<div style="font-size: 0.75rem; color: var(--text-muted);">Batch {item.user?.batch || '-'}</div>
									</td>
									<td>
										<span class="badge" class:badge-blue={item.userRole === 'INSTRUCTOR'} class:badge-green={item.userRole === 'LEARNER'}>
											{item.userRole}
										</span>
									</td>
									<td>
										<div style="font-weight: 600;">{item.groupName}</div>
										<div style="font-size: 0.75rem; color: var(--text-muted);">{item.subjectName}</div>
									</td>
									{#if canManage}
										<td class="text-right">
											<button class="btn btn-ghost" style="color: var(--error);" onclick={() => submit(async () => { if(confirm('Hapus enrollment?')) { await api<null>(`/api/enrollments/${item.id}`, { method: 'DELETE' }); await loadEnrollments(); success = 'Enrollment dihapus.'; } })}>
												<Icons name="x" size={16} />
											</button>
										</td>
									{/if}
								</tr>
							{:else}
								<tr><td colspan={canManage ? 4 : 3}><EmptyState text="Data enrollment tidak ditemukan." /></td></tr>
							{/each}
						</tbody>
					</table>
				</div>
				<Pagination meta={enrollmentPage} onPage={changeEnrollmentPage} onSize={changeEnrollmentSize} />
			</section>

			{#if canManage}
				<aside>
					<div class="card">
						<h3 class="mb-4">Tambah Member</h3>
						<form class="flex flex-direction-column gap-4" onsubmit={(event) => { event.preventDefault(); void submit(async () => { await api<Enrollment>('/api/enrollments', { method: 'POST', body: JSON.stringify({ userId: Number(form.userId), groupId: Number(form.groupId), role: form.role }) }); form.userId = ''; await loadEnrollments(); await loadMine(); success = 'Member ditambahkan.'; }); }}>
							<div class="form-group">
								<label for="createUserId">User ID</label>
								<input id="createUserId" bind:value={form.userId} type="number" required />
							</div>
							<div class="form-group">
								<label for="createGroupId">Group</label>
								<select id="createGroupId" bind:value={form.groupId}>
									{#each groups as g}<option value={g.id}>{g.name}</option>{/each}
								</select>
							</div>
							<div class="form-group">
								<label for="createRole">Peran</label>
								<select id="createRole" bind:value={form.role}>
									<option value="LEARNER">Learner</option>
									<option value="INSTRUCTOR">Instructor</option>
								</select>
							</div>
							<button class="btn btn-primary w-full" type="submit">Tambah</button>
						</form>
					</div>

					<div class="card mt-4">
						<h3 class="mb-4">Update Enrollment</h3>
						<form class="flex flex-direction-column gap-4" onsubmit={(event) => { event.preventDefault(); void submit(async () => { const body: Record<string, unknown> = {}; if (patch.userId) body.userId = Number(patch.userId); if (patch.groupId) body.groupId = Number(patch.groupId); if (patch.role) body.role = patch.role; await api<Enrollment>(`/api/enrollments/${patch.id}`, { method: 'PATCH', body: JSON.stringify(body) }); await loadEnrollments(); success = 'Enrollment diupdate.'; }); }}>
							<div class="form-group">
								<label for="patchId">Enrollment ID</label>
								<input id="patchId" bind:value={patch.id} type="number" required />
							</div>
							<div class="form-group">
								<label for="patchRole">Ganti Peran (Optional)</label>
								<select id="patchRole" bind:value={patch.role}>
									<option value="">- Biarkan tetap -</option>
									<option value="LEARNER">Learner</option>
									<option value="INSTRUCTOR">Instructor</option>
								</select>
							</div>
							<button class="btn btn-secondary w-full" type="submit">Update</button>
						</form>
					</div>

				</aside>
			{/if}
		</div>
	{/if}
</AccessPanel>

<style>
	h3 { font-size: 1.125rem; }
	.flex-direction-column { flex-direction: column; }
</style>
