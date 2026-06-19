<script lang="ts">
	import { onMount } from 'svelte';
	import Icons from '$lib/components/Icons.svelte';
	import Notice from '$lib/components/Notice.svelte';
	import { api, pageItems, paginationMeta } from '$lib/api';
	import type {
		Enrollment,
		Group,
		GroupSchedule,
		PageData,
		PaginationMeta,
		SubjectMaterial
	} from '$lib/types';

	let enrollments = $state<Enrollment[]>([]);
	let groups = $state<Group[]>([]);
	let schedules = $state<GroupSchedule[]>([]);
	let materials = $state<SubjectMaterial[]>([]);
	let enrollmentMeta = $state<PaginationMeta>({ page: 0, size: 4, totalPages: 1, totalElements: 0 });
	let loading = $state(true);
	let error = $state('');

	onMount(() => void loadDashboard());

	async function loadDashboard() {
		loading = true;
		error = '';
		try {
			const [enrollmentPayload, groupPayload, schedulePayload, materialPayload] = await Promise.all([
				api<PageData<Enrollment> | Enrollment[]>('/api/enrollments/me?page=0&size=4&sort=id,desc'),
				api<PageData<Group> | Group[]>('/api/groups?status=ON_GOING&page=0&size=4&sort=name,asc'),
				api<PageData<GroupSchedule> | GroupSchedule[]>('/api/group-schedules?page=0&size=5&sort=dayOfWeek,asc'),
				api<PageData<SubjectMaterial> | SubjectMaterial[]>('/api/subject-materials?page=0&size=5&sort=meetingNumber,asc')
			]);
			enrollments = pageItems(enrollmentPayload);
			groups = pageItems(groupPayload);
			schedules = pageItems(schedulePayload);
			materials = pageItems(materialPayload);
			enrollmentMeta = paginationMeta(enrollmentPayload, enrollmentMeta);
		} catch (err) {
			error = err instanceof Error ? err.message : 'Gagal memuat dashboard.';
		} finally {
			loading = false;
		}
	}
</script>

<svelte:head><title>Dashboard Pengguna - Divdik Course</title></svelte:head>

<section class="dashboard-heading">
	<div>
		<span class="eyebrow">Learning Overview</span>
		<h1>Dashboard Pengguna</h1>
		<p>Akses cepat ke group, jadwal, materi, dan aktivitas pembelajaran Anda.</p>
	</div>
	<a class="btn btn-primary" href="/app/groups">
		<span>Lihat Group</span>
		<Icons name="chevronRight" size={16} />
	</a>
</section>

<Notice {error} />

{#if loading}
	<div class="card">Memuat ringkasan pembelajaran...</div>
{:else}
	<section class="metric-grid">
		<article class="metric">
			<div class="metric-icon"><Icons name="graduationCap" size={20} /></div>
			<div><span>Kursus Saya</span><strong>{enrollmentMeta.totalElements}</strong></div>
		</article>
		<article class="metric">
			<div class="metric-icon green"><Icons name="layers" size={20} /></div>
			<div><span>Group Aktif</span><strong>{groups.length}</strong></div>
		</article>
		<article class="metric">
			<div class="metric-icon amber"><Icons name="calendar" size={20} /></div>
			<div><span>Jadwal Tampil</span><strong>{schedules.length}</strong></div>
		</article>
		<article class="metric">
			<div class="metric-icon rose"><Icons name="book" size={20} /></div>
			<div><span>Materi Terbaru</span><strong>{materials.length}</strong></div>
		</article>
	</section>

	<section class="dashboard-grid">
		<div class="dashboard-section">
			<div class="section-head">
				<div><span>Enrolled</span><h2>Kursus Saya</h2></div>
				<a href="/app/enrollments">Lihat semua</a>
			</div>
			<div class="item-list">
				{#each enrollments as item}
					<a class="list-item" href={`/app/groups/${item.groupId}`}>
						<div class="list-icon"><Icons name="graduationCap" size={18} /></div>
						<div>
							<strong>{item.groupName}</strong>
							<span>{item.subjectName} · {item.academicYear}</span>
						</div>
						<span class="badge badge-blue">{item.userRole}</span>
					</a>
				{:else}
					<div class="empty-inline">Belum ada enrollment aktif.</div>
				{/each}
			</div>
		</div>

		<div class="dashboard-section">
			<div class="section-head">
				<div><span>Schedule</span><h2>Jadwal Terdekat</h2></div>
				<a href="/app/schedules">Buka jadwal</a>
			</div>
			<div class="item-list">
				{#each schedules as schedule}
					<div class="list-item">
						<div class="list-icon amber"><Icons name="clock" size={18} /></div>
						<div>
							<strong>{schedule.groupName}</strong>
							<span>{schedule.dayOfWeek} · {schedule.startTime.slice(0, 5)}-{schedule.endTime.slice(0, 5)}</span>
						</div>
					</div>
				{:else}
					<div class="empty-inline">Belum ada jadwal aktif.</div>
				{/each}
			</div>
		</div>
	</section>

	<section class="dashboard-section">
		<div class="section-head">
			<div><span>Reference</span><h2>Materi Acuan</h2></div>
			<a href="/app/materials">Buka materi</a>
		</div>
		<div class="material-grid">
			{#each materials as material}
				<article>
					<span>Pertemuan {material.meetingNumber}</span>
					<strong>{material.title}</strong>
					<p>{material.subjectName || 'Subject'} </p>
				</article>
			{:else}
				<div class="empty-inline">Belum ada materi.</div>
			{/each}
		</div>
	</section>
{/if}

<style>
	.dashboard-heading,
	.section-head {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		gap: 1rem;
	}

	.dashboard-heading {
		margin-bottom: 1.75rem;
	}

	.dashboard-heading p {
		margin-top: 0.55rem;
	}

	.eyebrow,
	.section-head span,
	.material-grid article > span {
		color: var(--primary);
		font-size: 0.72rem;
		font-weight: 800;
		text-transform: uppercase;
		letter-spacing: 0.08em;
	}

	.metric-grid {
		display: grid;
		grid-template-columns: repeat(4, minmax(0, 1fr));
		gap: 1rem;
		margin-bottom: 1.25rem;
	}

	.metric,
	.dashboard-section {
		background: var(--bg-surface);
		border: 1px solid var(--border);
		border-radius: var(--radius-sm);
		box-shadow: var(--shadow-sm);
	}

	.metric {
		display: flex;
		align-items: center;
		gap: 0.9rem;
		padding: 1.1rem;
	}

	.metric > div:last-child {
		display: grid;
	}

	.metric span {
		color: var(--text-muted);
		font-size: 0.78rem;
		font-weight: 700;
	}

	.metric strong {
		font-size: 1.6rem;
	}

	.metric-icon,
	.list-icon {
		display: grid;
		place-items: center;
		border-radius: var(--radius-sm);
		background: var(--primary-soft);
		color: var(--primary);
	}

	.metric-icon {
		width: 42px;
		height: 42px;
	}

	.green { color: var(--success); background: var(--success-soft); }
	.amber { color: var(--warning); background: color-mix(in srgb, var(--warning) 12%, transparent); }
	.rose { color: var(--error); background: var(--error-soft); }

	.dashboard-grid {
		display: grid;
		grid-template-columns: repeat(2, minmax(0, 1fr));
		gap: 1.25rem;
		margin-bottom: 1.25rem;
	}

	.dashboard-section {
		padding: 1.25rem;
	}

	.section-head {
		align-items: center;
		margin-bottom: 1rem;
	}

	.section-head h2 {
		font-size: 1.05rem;
		margin-top: 0.15rem;
	}

	.section-head a {
		color: var(--primary);
		text-decoration: none;
		font-size: 0.8rem;
		font-weight: 800;
	}

	.item-list {
		display: grid;
	}

	.list-item {
		display: grid;
		grid-template-columns: auto 1fr auto;
		align-items: center;
		gap: 0.8rem;
		padding: 0.85rem 0;
		border-top: 1px solid var(--border-light);
		color: inherit;
		text-decoration: none;
	}

	.list-item:first-child {
		border-top: 0;
	}

	.list-icon {
		width: 36px;
		height: 36px;
	}

	.list-item > div:nth-child(2) {
		display: grid;
		min-width: 0;
	}

	.list-item strong {
		font-size: 0.88rem;
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.list-item span {
		color: var(--text-muted);
		font-size: 0.76rem;
	}

	.material-grid {
		display: grid;
		grid-template-columns: repeat(5, minmax(0, 1fr));
		gap: 0.75rem;
	}

	.material-grid article {
		display: grid;
		gap: 0.35rem;
		padding: 1rem;
		border: 1px solid var(--border-light);
		border-radius: var(--radius-sm);
		background: var(--bg-table-head);
	}

	.material-grid strong {
		font-size: 0.86rem;
	}

	.material-grid p {
		font-size: 0.75rem;
	}

	.empty-inline {
		padding: 1rem 0;
		color: var(--text-muted);
		font-size: 0.85rem;
	}

	@media (max-width: 1050px) {
		.metric-grid,
		.material-grid {
			grid-template-columns: repeat(2, minmax(0, 1fr));
		}
	}

	@media (max-width: 720px) {
		.dashboard-heading,
		.dashboard-grid {
			display: grid;
			grid-template-columns: 1fr;
		}

		.metric-grid,
		.material-grid {
			grid-template-columns: 1fr;
		}
	}
</style>
