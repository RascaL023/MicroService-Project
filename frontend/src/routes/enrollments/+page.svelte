<script lang="ts">
	import { onMount } from 'svelte';
	import { page } from '$app/state';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import AccessPanel from '$lib/components/AccessPanel.svelte';
	import ConfirmModal from '$lib/components/ConfirmModal.svelte';
	import Notice from '$lib/components/Notice.svelte';
	import PageTitle from '$lib/components/PageTitle.svelte';
	import Icons from '$lib/components/Icons.svelte';
	import Pagination from '$lib/components/Pagination.svelte';
	import { api, hasAnyAuthority, pageItems, paginationMeta, readSession } from '$lib/api';
	import type { Enrollment, Group, LoginData, PageData, PaginationMeta, Subject } from '$lib/types';

	let { portal = '' }: { portal?: 'admin' | 'app' | '' } = $props();

	let enrollments = $state<Enrollment[]>([]);
	let mine = $state<Enrollment[]>([]);
	let enrollmentPage = $state<PaginationMeta>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
	let minePage = $state<PaginationMeta>({ page: 0, size: 6, totalPages: 1, totalElements: 0 });
	let groups = $state<Group[]>([]);
	let subjects = $state<Subject[]>([]);
	let filters = $state({
		userId: '',
		groupId: '',
		subjectId: '',
		academicYear: '',
		role: '',
		sortBy: 'id',
		sortDirection: 'desc'
	});
	let patch = $state({ id: '', userId: '', groupId: '', role: '' });
	let showUpdate = $state(false);
	let confirmState = $state({
		open: false,
		title: '',
		message: '',
		confirmLabel: 'Ya, lanjutkan'
	});
	let pendingConfirm: (() => Promise<void>) | null = null;
	let error = $state('');
	let success = $state('');
	let session = $state<LoginData | null>(null);
	const isUserPortal = $derived(portal === 'app' || (!portal && page.url.pathname.startsWith('/app/')));
	const canReadAll = $derived(!isUserPortal && hasAnyAuthority(session, ['enrollment.read', 'enrollment.*']));
	const canUpdate = $derived(!isUserPortal && hasAnyAuthority(session, ['enrollment.update', 'enrollment.*']));
	const canDelete = $derived(!isUserPortal && hasAnyAuthority(session, ['enrollment.delete', 'enrollment.*']));

	onMount(() => {
		session = readSession();
		void loadAll();
	});

	async function submit(task: () => Promise<void>) {
		error = ''; success = '';
		try { await task(); } catch (err) { error = err instanceof Error ? err.message : 'Request gagal.'; }
	}

	async function updateEnrollment() {
		await submit(async () => {
			const body: Record<string, unknown> = {};
			if (patch.userId) body.userId = Number(patch.userId);
			if (patch.groupId) body.groupId = Number(patch.groupId);
			if (patch.role) body.role = patch.role;
			await api<Enrollment>(`/api/enrollments/${patch.id}`, { method: 'PATCH', body: JSON.stringify(body) });
			patch = { id: '', userId: '', groupId: '', role: '' };
			showUpdate = false;
			await loadEnrollments();
			success = 'Enrollment diupdate.';
		});
	}

	async function deleteEnrollment(item: Enrollment) {
		askConfirm({
			title: 'Hapus enrollment?',
			message: `Member ${item.user?.name || `Enrollment #${item.id}`} akan dihapus dari ${item.groupName}.`,
			confirmLabel: 'Hapus Enrollment'
		}, async () => {
			await submit(async () => {
				await api<null>(`/api/enrollments/${item.id}`, { method: 'DELETE' });
				await loadEnrollments();
				success = 'Enrollment dihapus.';
			});
		});
	}

	function openUpdateEnrollment(item: Enrollment) {
		patch = {
			id: String(item.id),
			userId: '',
			groupId: '',
			role: item.userRole
		};
		showUpdate = true;
	}

	function askConfirm(
		config: { title: string; message: string; confirmLabel?: string },
		action: () => Promise<void>
	) {
		confirmState = {
			open: true,
			title: config.title,
			message: config.message,
			confirmLabel: config.confirmLabel ?? 'Ya, lanjutkan'
		};
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

	async function loadAll() {
		if (isUserPortal) {
			await loadMine();
			return;
		}

		const [groupsPayload, subjectsPayload] = await Promise.all([
			api<PageData<Group> | Group[]>('/api/groups?sort=name,asc'),
			api<PageData<Subject> | Subject[]>('/api/subjects?sort=name,asc')
		]);
		groups = pageItems(groupsPayload);
		subjects = pageItems(subjectsPayload);
		if (canReadAll) await loadEnrollments();
	}

	async function loadEnrollments() {
		const query = new URLSearchParams({
			page: String(enrollmentPage.page),
			size: String(enrollmentPage.size),
			sort: `${filters.sortBy},${filters.sortDirection}`
		});
		for (const [key, value] of Object.entries(filters)) {
			if (key !== 'sortBy' && key !== 'sortDirection' && value) query.set(key, value);
		}
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

<PageTitle
	eyebrow={isUserPortal ? 'Learning Access' : 'Academic Management'}
	title={isUserPortal ? 'Kursus Saya' : 'Enrollments & Peran'}
	description={isUserPortal
		? 'Daftar group dan peran Anda dalam kegiatan pembelajaran.'
		: 'Manajemen peran user (Instructor/Learner) dalam setiap group kursus.'}
/>

<Notice {error} {success} />

<AccessPanel authorities={['course.read', 'enrollment.read', 'enrollment.*']}>
	<!-- Session Enrollments -->
	{#if isUserPortal}
	<section class="mb-4">
		{#if mine.length}
			<div class="mine-grid">
				{#each mine as item}
					<a class="mine-card" href={`/app/groups/${item.groupId}`}>
						<div class="mine-card-head">
							<div>
								<div class="mine-title">{item.groupName}</div>
								<div class="mine-subtitle">{item.subjectName}</div>
							</div>
							<span class="mine-arrow"><Icons name="chevronRight" size={18} /></span>
						</div>
						<div class="flex justify-between items-center mt-4">
							<span class="badge" class:badge-blue={item.userRole === 'INSTRUCTOR'} class:badge-green={item.userRole === 'LEARNER'}>
								{item.userRole}
							</span>
							<small>{item.academicYear}</small>
						</div>
					</a>
				{/each}
			</div>
			<Pagination meta={minePage} onPage={changeMinePage} onSize={changeMineSize} sizes={[6, 12, 24, 48]} />
		{:else}
			<div class="card" style="text-align: center; color: var(--text-muted);">
				Anda belum terdaftar di group manapun.
			</div>
		{/if}
	</section>
	{/if}

	{#if canReadAll}
		<section>
			<div class="card" style="padding: 1rem; margin-bottom: 1.5rem;">
				<form class="enrollment-filters" onsubmit={(event) => { event.preventDefault(); applyFilters(); }}>
					<label>
						<span>User ID</span>
						<input bind:value={filters.userId} placeholder="Contoh: 24" />
					</label>
					<label>
						<span>Group</span>
						<select bind:value={filters.groupId}>
							<option value="">Semua group</option>
							{#each groups as group}<option value={group.id}>{group.name}</option>{/each}
						</select>
					</label>
					<label>
						<span>Subject</span>
						<select bind:value={filters.subjectId}>
							<option value="">Semua subject</option>
							{#each subjects as subject}<option value={subject.id}>{subject.name}</option>{/each}
						</select>
					</label>
					<label>
						<span>Tahun</span>
						<input bind:value={filters.academicYear} placeholder="2026/2027" />
					</label>
					<label>
						<span>Peran</span>
						<select bind:value={filters.role}>
							<option value="">Semua Peran</option>
							<option value="INSTRUCTOR">Instructor</option>
							<option value="LEARNER">Learner</option>
						</select>
					</label>
					<label>
						<span>Urutkan</span>
						<select bind:value={filters.sortBy}>
							<option value="id">Terbaru</option>
							<option value="academicYear">Tahun akademik</option>
							<option value="role">Peran</option>
						</select>
					</label>
					<label>
						<span>Arah</span>
						<select bind:value={filters.sortDirection}>
							<option value="desc">Turun</option>
							<option value="asc">Naik</option>
						</select>
					</label>
					<button class="btn btn-primary" type="submit">
						<Icons name="search" size={18} />
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
							{#if canUpdate || canDelete}<th class="text-right">Aksi</th>{/if}
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
								{#if canUpdate || canDelete}
									<td class="text-right">
										<div class="row-actions">
											{#if canUpdate}
												<button class="btn btn-ghost btn-icon" aria-label="Update enrollment" type="button" onclick={() => openUpdateEnrollment(item)}>
													<Icons name="edit" size={16} />
												</button>
											{/if}
											{#if canDelete}
												<button class="btn btn-ghost btn-icon danger" aria-label="Hapus enrollment" type="button" onclick={() => deleteEnrollment(item)}>
													<Icons name="x" size={16} />
												</button>
											{/if}
										</div>
									</td>
								{/if}
							</tr>
						{:else}
							<tr><td colspan={canUpdate || canDelete ? 4 : 3}><EmptyState text="Data enrollment tidak ditemukan." /></td></tr>
						{/each}
					</tbody>
				</table>
			</div>
			<Pagination meta={enrollmentPage} onPage={changeEnrollmentPage} onSize={changeEnrollmentSize} />
		</section>
	{/if}
</AccessPanel>

{#if showUpdate && canUpdate}
	<div class="modal-backdrop" role="presentation" onclick={() => showUpdate = false}>
		<section class="modal-panel" role="dialog" aria-modal="true" tabindex="-1" onclick={(event) => event.stopPropagation()} onkeydown={(event) => event.stopPropagation()}>
			<div class="modal-head">
				<div>
					<h3>Update Enrollment</h3>
					<p>Update peran enrollment #{patch.id}.</p>
				</div>
				<button class="btn btn-ghost btn-icon" aria-label="Tutup modal" type="button" onclick={() => showUpdate = false}>
					<Icons name="x" size={18} />
				</button>
			</div>
			<form class="modal-form" onsubmit={(event) => { event.preventDefault(); void updateEnrollment(); }}>
				<div class="form-group">
					<label for="patchId">Enrollment ID</label>
					<input id="patchId" bind:value={patch.id} type="number" disabled required />
				</div>
				<div class="form-group">
					<label for="patchRole">Ganti Peran (Optional)</label>
					<select id="patchRole" bind:value={patch.role}>
						<option value="">- Biarkan tetap -</option>
						<option value="LEARNER">Learner</option>
						<option value="INSTRUCTOR">Instructor</option>
					</select>
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

	.mine-grid {
		display: grid;
		grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
		gap: 1rem;
	}

	.mine-card {
		display: block;
		margin-bottom: 0;
		padding: 1.25rem;
		border-top: 3px solid var(--primary);
		color: inherit;
		text-decoration: none;
		background: var(--bg-surface);
		border-right: 1px solid var(--border);
		border-bottom: 1px solid var(--border);
		border-left: 1px solid var(--border);
		border-radius: var(--radius);
		box-shadow: var(--shadow-sm);
		transition: border-color 0.2s, box-shadow 0.2s, transform 0.2s;
	}

	.mine-card:hover {
		border-color: var(--primary-border);
		box-shadow: var(--shadow-md);
		transform: translateY(-1px);
	}

	.mine-card-head {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		gap: 1rem;
		color: var(--text-main);
	}

	.mine-arrow {
		display: inline-flex;
		color: var(--text-light);
		flex: 0 0 auto;
	}

	.mine-title {
		font-weight: 700;
		font-size: 1.125rem;
	}

	.mine-subtitle {
		color: var(--text-muted);
		font-size: 0.875rem;
		margin-top: 0.25rem;
	}

	.enrollment-filters {
		display: grid;
		grid-template-columns: repeat(4, minmax(130px, 1fr));
		gap: 0.75rem;
		align-items: end;
	}

	.enrollment-filters label {
		display: grid;
		gap: 0.35rem;
	}

	.enrollment-filters label span {
		color: var(--text-muted);
		font-size: 0.72rem;
		font-weight: 800;
		text-transform: uppercase;
	}

	.btn-icon {
		width: 38px;
		height: 38px;
		padding: 0;
	}

	.danger {
		color: var(--error);
	}

	.row-actions {
		display: inline-flex;
		justify-content: flex-end;
		gap: 0.35rem;
	}

	@media (max-width: 900px) {
		.enrollment-filters {
			grid-template-columns: repeat(2, minmax(0, 1fr));
		}
	}

	@media (max-width: 560px) {
		.enrollment-filters {
			grid-template-columns: 1fr;
		}

		.enrollment-filters .btn {
			width: 100%;
		}
	}
</style>
