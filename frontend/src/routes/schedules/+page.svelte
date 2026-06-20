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
	import { api, dayLabel, hasAnyAuthority, pageItems, paginationMeta, readSession, timeLabel } from '$lib/api';
	import type { Enrollment, Group, GroupSchedule, LoginData, PageData, PaginationMeta, ScheduleTemplate } from '$lib/types';

	const days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

	let groups = $state<Group[]>([]);
	let templates = $state<ScheduleTemplate[]>([]);
	let schedules = $state<GroupSchedule[]>([]);
	let pageMeta = $state<PaginationMeta>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
	let filterGroup = $state('');
	let filterDay = $state('');
	let form = $state({ groupId: '', dayOfWeek: 'MONDAY', templateId: '' });
	let templateForm = $state({ name: '', startTime: '09:00:00', endTime: '11:00:00' });
	let showScheduleModal = $state(false);
	let showTemplateModal = $state(false);
	let confirmState = $state({
		open: false,
		title: '',
		message: '',
		confirmLabel: 'Ya, lanjutkan'
	});
	let pendingConfirm: (() => Promise<void>) | null = null;
	let error = $state('');
	let success = $state('');
	let loading = $state(true);
	let session = $state<LoginData | null>(null);

	const isUserPortal = $derived(page.url.pathname.startsWith('/app/'));
	const canCreate = $derived(!isUserPortal && hasAnyAuthority(session, ['group-schedule.create', 'group-schedule.*']));
	const canDelete = $derived(!isUserPortal && hasAnyAuthority(session, ['group-schedule.delete', 'group-schedule.*']));

	onMount(() => {
		const nextSession = readSession();
		session = nextSession;
		void loadAll(nextSession);
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

	async function loadAll(currentSession: LoginData | null = session) {
		loading = true;
		try {
			const mayCreate = !isUserPortal && hasAnyAuthority(currentSession, ['group-schedule.create', 'group-schedule.*']);
			if (isUserPortal) {
				const enrollmentPayload = await api<PageData<Enrollment> | Enrollment[]>('/api/enrollments/me?page=0&size=100&sort=id,desc');
				groups = groupsFromEnrollments(pageItems(enrollmentPayload));
			} else {
				const groupPayload = await api<PageData<Group> | Group[]>('/api/groups?status=ON_GOING&sort=name,asc');
				groups = pageItems(groupPayload);
				if (mayCreate) {
					const templatePayload = await api<PageData<ScheduleTemplate> | ScheduleTemplate[]>('/api/schedule-templates?sort=startTime,asc');
					templates = pageItems(templatePayload);
				} else {
					templates = [];
				}
			}
			if (!form.groupId && groups[0]) form.groupId = String(groups[0].id);
			if (!form.templateId && templates[0]) form.templateId = String(templates[0].id);
			await loadSchedules();
		} catch (err) {
			error = err instanceof Error ? err.message : 'Gagal memuat jadwal.';
		} finally {
			loading = false;
		}
	}

	async function loadTemplates() {
		const payload = await api<PageData<ScheduleTemplate> | ScheduleTemplate[]>('/api/schedule-templates?sort=startTime,asc');
		templates = pageItems(payload);
		if (!form.templateId && templates[0]) form.templateId = String(templates[0].id);
	}

	async function loadSchedules() {
		const query = new URLSearchParams({
			page: String(pageMeta.page),
			size: String(pageMeta.size),
			sort: 'dayOfWeek,asc'
		});
		if (filterGroup) query.set('groupId', filterGroup);
		if (filterDay) query.set('dayOfWeek', filterDay);
		const path = isUserPortal ? '/api/group-schedules/me' : '/api/group-schedules';
		const payload = await api<PageData<GroupSchedule> | GroupSchedule[]>(`${path}?${query}`);
		schedules = pageItems(payload);
		pageMeta = paginationMeta(payload, pageMeta);
	}

	async function createTemplate() {
		await submit(async () => {
			const created = await api<ScheduleTemplate>('/api/schedule-templates', {
				method: 'POST',
				body: JSON.stringify(templateForm)
			});
			templateForm = { name: '', startTime: templateForm.startTime, endTime: templateForm.endTime };
			await loadTemplates();
			if (created.data) form.templateId = String(created.data.id);
			showTemplateModal = false;
			success = 'Template waktu berhasil dibuat.';
		});
	}

	async function addSchedule() {
		await submit(async () => {
			await api<GroupSchedule>('/api/group-schedules', {
				method: 'POST',
				body: JSON.stringify({
					groupId: Number(form.groupId),
					dayOfWeek: form.dayOfWeek,
					templateId: Number(form.templateId)
				})
			});
			await loadSchedules();
			showScheduleModal = false;
			success = 'Jadwal berhasil ditambahkan.';
		});
	}

	async function deleteSchedule(scheduleId: number) {
		askConfirm({
			title: 'Hapus jadwal?',
			message: 'Jadwal group ini akan dihapus dari daftar jadwal aktif.',
			confirmLabel: 'Hapus Jadwal'
		}, async () => {
			await submit(async () => {
				await api<null>(`/api/group-schedules/${scheduleId}`, { method: 'DELETE' });
				await loadSchedules();
				success = 'Jadwal berhasil dihapus.';
			});
		});
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

	function applyFilters() {
		pageMeta = { ...pageMeta, page: 0 };
		void loadSchedules();
	}

	function groupsFromEnrollments(items: Enrollment[]) {
		const mapped = new Map<number, Group>();
		for (const item of items) {
			if (!mapped.has(item.groupId)) {
				mapped.set(item.groupId, {
					id: item.groupId,
					name: item.groupName,
					subjectId: item.subjectId,
					subjectName: item.subjectName,
					academicYear: item.academicYear,
					status: 'ON_GOING'
				});
			}
		}
		return [...mapped.values()];
	}

	function changePage(page: number) {
		pageMeta = { ...pageMeta, page };
		void loadSchedules();
	}

	function changePageSize(size: number) {
		pageMeta = { ...pageMeta, page: 0, size };
		void loadSchedules();
	}
</script>

<svelte:head><title>Schedules - Divdik Course</title></svelte:head>

<div class="page-heading">
	<PageTitle eyebrow="Academic Operations" title="Jadwal Group" description="Kelola template waktu dan jadwal aktif untuk group yang sedang berjalan." />

	{#if canCreate}
		<section class="page-actions schedule-actions" aria-label="Aksi jadwal">
			<button class="btn btn-secondary" type="button" onclick={() => showTemplateModal = true}>
				<Icons name="clock" size={17} />
				<span>Buat Template Waktu</span>
			</button>
			<button class="btn btn-primary" type="button" onclick={() => showScheduleModal = true}>
				<Icons name="calendar" size={17} />
				<span>Tambah Jadwal</span>
			</button>
		</section>
	{/if}
</div>

<Notice {error} {success} />

<AccessPanel authorities={['group-schedule.read', 'group-schedule.*']}>
	<section class="schedule-toolbar">
		<form class="schedule-filters" onsubmit={(event) => { event.preventDefault(); applyFilters(); }}>
			<label>
				<span>Group</span>
				<select bind:value={filterGroup}>
					<option value="">Semua group</option>
					{#each groups as group}
						<option value={group.id}>{group.name} - {group.subjectName}</option>
					{/each}
				</select>
			</label>
			<label>
				<span>Hari</span>
				<select bind:value={filterDay}>
					<option value="">Semua hari</option>
					{#each days as day}
						<option value={day}>{dayLabel(day)}</option>
					{/each}
				</select>
			</label>
			<button class="btn btn-primary filter-button filter-submit" type="submit">
				<Icons name="search" size={17} />
				<span>Filter</span>
			</button>
		</form>
	</section>

	<section class="table-wrap">
		{#if loading}
			<div class="card">Memuat data jadwal...</div>
		{:else if schedules.length === 0}
			<EmptyState text="Data jadwal tidak ditemukan." />
		{:else}
			<div class="table-container">
				<table>
					<thead>
						<tr>
							<th>Group</th>
							<th>Subject</th>
							<th>Hari</th>
							<th>Template</th>
							<th>Waktu</th>
							{#if canDelete}<th class="text-right">Aksi</th>{/if}
						</tr>
					</thead>
					<tbody>
						{#each schedules as schedule}
							<tr>
								<td><strong>{schedule.groupName}</strong></td>
								<td>{schedule.subjectName}</td>
								<td>{dayLabel(schedule.dayOfWeek)}</td>
								<td>{schedule.templateName ?? '-'}</td>
								<td><span class="badge badge-blue">{timeLabel(schedule.startTime, schedule.endTime)}</span></td>
								{#if canDelete}
									<td class="text-right">
										<button class="btn btn-ghost danger icon-button" type="button" onclick={() => deleteSchedule(schedule.id)}>
											<Icons name="x" size={16} />
										</button>
									</td>
								{/if}
							</tr>
						{/each}
					</tbody>
				</table>
			</div>
			<Pagination meta={pageMeta} onPage={changePage} onSize={changePageSize} />
		{/if}
	</section>
</AccessPanel>

{#if showScheduleModal && canCreate}
	<div class="modal-backdrop" role="presentation" onclick={() => showScheduleModal = false}>
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
					<h3>Tambah Jadwal</h3>
					<p>Pilih group aktif, hari, dan template waktu.</p>
				</div>
				<button class="btn btn-ghost icon-button" aria-label="Tutup modal" type="button" onclick={() => showScheduleModal = false}>
					<Icons name="x" size={18} />
				</button>
			</div>
			<form class="stack-form modal-form" onsubmit={(event) => { event.preventDefault(); void addSchedule(); }}>
				<label>
					<span>Group</span>
					<select bind:value={form.groupId} required>
						{#each groups as group}
							<option value={group.id}>{group.name} - {group.subjectName}</option>
						{/each}
					</select>
				</label>
				<div class="form-row">
					<label>
						<span>Hari</span>
						<select bind:value={form.dayOfWeek}>
							{#each days as day}
								<option value={day}>{dayLabel(day)}</option>
							{/each}
						</select>
					</label>
					<label>
						<span>Template</span>
						<select bind:value={form.templateId} required>
							{#each templates as template}
								<option value={template.id}>{template.name} - {timeLabel(template.startTime, template.endTime)}</option>
							{/each}
						</select>
					</label>
				</div>
				{#if templates.length === 0}
					<div class="soft-note">Belum ada template waktu. Buat template dulu sebelum menambahkan jadwal.</div>
				{/if}
				<div class="modal-actions">
					<button class="btn btn-ghost" type="button" onclick={() => showScheduleModal = false}>Batal</button>
					<button class="btn btn-primary" type="submit" disabled={!form.groupId || !form.templateId}>Tambah Jadwal</button>
				</div>
			</form>
		</section>
	</div>
{/if}

{#if showTemplateModal && canCreate}
	<div class="modal-backdrop" role="presentation" onclick={() => showTemplateModal = false}>
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
					<h3>Template Waktu</h3>
					<p>Slot reusable untuk semua jadwal group.</p>
				</div>
				<button class="btn btn-ghost icon-button" aria-label="Tutup modal" type="button" onclick={() => showTemplateModal = false}>
					<Icons name="x" size={18} />
				</button>
			</div>
			<form class="stack-form modal-form" onsubmit={(event) => { event.preventDefault(); void createTemplate(); }}>
				<label>
					<span>Nama Template</span>
					<input bind:value={templateForm.name} placeholder="Contoh: Pagi 09-11" required />
				</label>
				<div class="form-row">
					<label>
						<span>Mulai</span>
						<input bind:value={templateForm.startTime} type="time" step="1" required />
					</label>
					<label>
						<span>Selesai</span>
						<input bind:value={templateForm.endTime} type="time" step="1" required />
					</label>
				</div>
				<div class="modal-actions">
					<button class="btn btn-ghost" type="button" onclick={() => showTemplateModal = false}>Batal</button>
					<button class="btn btn-primary" type="submit">Buat Template</button>
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
	.page-heading {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		gap: 1rem;
	}

	.schedule-toolbar {
		margin-bottom: 1rem;
	}

	.schedule-filters {
		display: grid;
		grid-template-columns: minmax(220px, 1fr) minmax(150px, 190px) auto;
		gap: 0.875rem;
		align-items: end;
	}

	.filter-button {
		height: 45px;
		min-width: 118px;
		align-self: end;
		justify-content: center;
		white-space: nowrap;
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

	.schedule-actions {
		margin-top: 0.25rem;
		margin-bottom: 0;
	}

	.stack-form {
		display: grid;
		gap: 0.875rem;
	}

	.form-row {
		display: grid;
		grid-template-columns: repeat(2, minmax(0, 1fr));
		gap: 0.875rem;
	}

	.soft-note {
		background: var(--primary-soft);
		color: var(--primary);
		border: 1px solid var(--primary-border);
		border-radius: var(--radius-sm);
		padding: 0.75rem;
		font-size: 0.85rem;
		font-weight: 700;
	}

	.table-wrap {
		min-width: 0;
	}

	.danger {
		color: var(--error);
	}

	.icon-button {
		width: 38px;
		height: 38px;
		padding: 0;
	}

	@media (max-width: 900px) {
		.page-heading {
			display: grid;
			grid-template-columns: 1fr;
		}

		.schedule-filters {
			grid-template-columns: 1fr;
		}
	}

	@media (max-width: 560px) {
		.form-row {
			grid-template-columns: 1fr;
		}

		.schedule-filters .btn,
		.filter-button {
			width: 100%;
		}
	}
</style>
