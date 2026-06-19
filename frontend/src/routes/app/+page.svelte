<script lang="ts">
	import { onMount } from 'svelte';
	import Icons from '$lib/components/Icons.svelte';
	import Notice from '$lib/components/Notice.svelte';
	import { api, dayLabel, pageItems, paginationMeta, timeLabel } from '$lib/api';
	import type {
		Enrollment,
		GroupSchedule,
		PageData,
		PaginationMeta
	} from '$lib/types';

	let enrollments = $state<Enrollment[]>([]);
	let allSchedules = $state<GroupSchedule[]>([]);
	let schedules = $state<GroupSchedule[]>([]);
	let enrollmentMeta = $state<PaginationMeta>({ page: 0, size: 4, totalPages: 1, totalElements: 0 });
	let loading = $state(true);
	let error = $state('');

	onMount(() => void loadDashboard());

	async function loadDashboard() {
		loading = true;
		error = '';
		try {
			const [enrollmentPayload, schedulePayload] = await Promise.all([
				api<PageData<Enrollment> | Enrollment[]>('/api/enrollments/me?page=0&size=4&sort=id,desc'),
				api<PageData<GroupSchedule> | GroupSchedule[]>('/api/group-schedules/me?page=0&size=50&sort=dayOfWeek,asc')
			]);
			enrollments = pageItems(enrollmentPayload);
			allSchedules = nearestSchedules(pageItems(schedulePayload));
			schedules = allSchedules.slice(0, 5);
			enrollmentMeta = paginationMeta(enrollmentPayload, enrollmentMeta);
		} catch (err) {
			error = err instanceof Error ? err.message : 'Gagal memuat dashboard.';
		} finally {
			loading = false;
		}
	}

	function nearestSchedules(items: GroupSchedule[]) {
		const todayIndex = new Date().getDay() || 7;
		return [...items].sort((left, right) => {
			const leftDelta = dayDelta(left.dayOfWeek, todayIndex);
			const rightDelta = dayDelta(right.dayOfWeek, todayIndex);
			if (leftDelta !== rightDelta) return leftDelta - rightDelta;
			return left.startTime.localeCompare(right.startTime);
		});
	}

	function dayDelta(day: string, todayIndex: number) {
		const dayOrder: Record<string, number> = {
			MONDAY: 1,
			TUESDAY: 2,
			WEDNESDAY: 3,
			THURSDAY: 4,
			FRIDAY: 5,
			SATURDAY: 6,
			SUNDAY: 7
		};
		const dayIndex = dayOrder[day] ?? 7;
		return (dayIndex - todayIndex + 7) % 7;
	}

	function schedulesForGroup(groupId: number) {
		return allSchedules.filter((schedule) => schedule.groupId === groupId);
	}

	function nextScheduleForGroup(groupId: number) {
		return schedulesForGroup(groupId)[0];
	}

	function roleLabel(role: string) {
		if (role === 'INSTRUCTOR') return 'Instructor';
		if (role === 'LEARNER') return 'Learner';
		return role;
	}

	function initialFrom(text: string) {
		return text.trim().charAt(0).toUpperCase() || 'K';
	}
</script>

<svelte:head><title>Dashboard Pengguna - Divdik Course</title></svelte:head>

<section class="dashboard-heading">
	<div>
		<span class="eyebrow">Learning Overview</span>
		<h1>Dashboard Pengguna</h1>
		<p>Akses cepat ke kursus dan jadwal group yang Anda ikuti.</p>
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
			<div class="metric-icon amber"><Icons name="calendar" size={20} /></div>
			<div><span>Jadwal Aktif</span><strong>{allSchedules.length}</strong></div>
		</article>
	</section>

	<section class="dashboard-grid">
		<div class="dashboard-section">
			<div class="section-head">
				<div><span>Enrolled</span><h2>Kursus Saya</h2></div>
				<a href="/app/enrollments">Lihat semua</a>
			</div>
			<div class="course-list">
				{#each enrollments as item}
					{@const nextSchedule = nextScheduleForGroup(item.groupId)}
					{@const groupScheduleCount = schedulesForGroup(item.groupId).length}
					<a class="course-card" href={`/app/groups/${item.groupId}`}>
						<div class="course-mark">{initialFrom(item.groupName)}</div>
						<div class="course-main">
							<div class="course-title-row">
								<strong>{item.groupName}</strong>
								<span class="badge" class:badge-blue={item.userRole === 'INSTRUCTOR'} class:badge-green={item.userRole === 'LEARNER'}>
									{roleLabel(item.userRole)}
								</span>
							</div>
							<span class="course-subtitle">{item.subjectName} · {item.academicYear}</span>
							<div class="course-meta">
								<span>
									<Icons name="clock" size={14} />
									{#if nextSchedule}
										{dayLabel(nextSchedule.dayOfWeek)} · {timeLabel(nextSchedule.startTime, nextSchedule.endTime)}
									{:else}
										Belum ada jadwal
									{/if}
								</span>
								<span>{groupScheduleCount} jadwal aktif</span>
							</div>
						</div>
						<span class="course-arrow"><Icons name="chevronRight" size={18} /></span>
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
							<span>{dayLabel(schedule.dayOfWeek)} · {timeLabel(schedule.startTime, schedule.endTime)}</span>
						</div>
					</div>
				{:else}
					<div class="empty-inline">Belum ada jadwal aktif.</div>
				{/each}
			</div>
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
	.section-head span {
		color: var(--primary);
		font-size: 0.72rem;
		font-weight: 800;
		text-transform: uppercase;
		letter-spacing: 0.08em;
	}

	.metric-grid {
		display: grid;
		grid-template-columns: repeat(2, minmax(0, 1fr));
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

	.amber { color: var(--warning); background: color-mix(in srgb, var(--warning) 12%, transparent); }

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

	.course-list {
		display: grid;
		gap: 0.8rem;
	}

	.course-card {
		display: grid;
		grid-template-columns: auto minmax(0, 1fr) auto;
		align-items: center;
		gap: 0.9rem;
		padding: 1rem;
		border: 1px solid var(--border-light);
		border-radius: var(--radius);
		background:
			linear-gradient(135deg, var(--primary-soft), transparent 52%),
			var(--bg-surface);
		color: inherit;
		text-decoration: none;
		transition: border-color 0.2s, box-shadow 0.2s, transform 0.2s;
	}

	.course-card:hover {
		border-color: var(--primary-border);
		box-shadow: var(--shadow-md);
		transform: translateY(-1px);
	}

	.course-mark {
		width: 44px;
		height: 44px;
		display: grid;
		place-items: center;
		border-radius: 14px;
		background: var(--primary);
		color: var(--text-on-primary);
		font-weight: 900;
		box-shadow: 0 8px 18px var(--primary-shadow);
	}

	.course-main {
		min-width: 0;
		display: grid;
		gap: 0.35rem;
	}

	.course-title-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 0.75rem;
		min-width: 0;
	}

	.course-title-row strong,
	.course-subtitle {
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.course-subtitle,
	.course-meta {
		color: var(--text-muted);
		font-size: 0.76rem;
	}

	.course-meta {
		display: flex;
		flex-wrap: wrap;
		gap: 0.45rem 0.75rem;
	}

	.course-meta span {
		display: inline-flex;
		align-items: center;
		gap: 0.35rem;
	}

	.course-arrow {
		display: inline-flex;
		color: var(--text-light);
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

	.empty-inline {
		padding: 1rem 0;
		color: var(--text-muted);
		font-size: 0.85rem;
	}

	@media (max-width: 1050px) {
		.metric-grid {
			grid-template-columns: repeat(2, minmax(0, 1fr));
		}
	}

	@media (max-width: 720px) {
		.dashboard-heading,
		.dashboard-grid {
			display: grid;
			grid-template-columns: 1fr;
		}

		.metric-grid {
			grid-template-columns: 1fr;
		}

		.course-card {
			grid-template-columns: auto minmax(0, 1fr);
		}

		.course-arrow {
			display: none;
		}

		.course-title-row {
			display: grid;
			justify-content: stretch;
		}
	}
</style>
