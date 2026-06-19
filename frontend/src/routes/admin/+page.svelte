<script lang="ts">
	import { onMount } from 'svelte';
	import Icons from '$lib/components/Icons.svelte';
	import Notice from '$lib/components/Notice.svelte';
	import { api, hasAnyAuthority, readSession } from '$lib/api';
	import type {
		AuthDashboardSummary,
		CourseDashboardSummary,
		LoginData,
		UserDashboardSummary
	} from '$lib/types';

	let session = $state<LoginData | null>(null);
	let authSummary = $state<AuthDashboardSummary | null>(null);
	let userSummary = $state<UserDashboardSummary | null>(null);
	let courseSummary = $state<CourseDashboardSummary | null>(null);
	let loading = $state(true);
	let error = $state('');
	let unavailable = $state<string[]>([]);

	const canManageUsers = $derived(hasAnyAuthority(session, ['user.*']));
	const canReadGroups = $derived(hasAnyAuthority(session, ['group.read', 'group.*']));
	const canCreateUsers = $derived(hasAnyAuthority(session, ['user.create', 'user.*']));
	const canCreateBatches = $derived(hasAnyAuthority(session, ['batch.create', 'batch.*']));
	const canCreateGroups = $derived(hasAnyAuthority(session, ['group.create', 'group.*']));
	const canManageSchedules = $derived(hasAnyAuthority(session, ['group-schedule.create', 'group-schedule.*']));
	const canManageEnrollments = $derived(hasAnyAuthority(session, ['enrollment.create', 'enrollment.*']));

	onMount(() => {
		session = readSession();
		void loadDashboard();
	});

	async function loadDashboard() {
		loading = true;
		error = '';
		unavailable = [];

		const tasks: Promise<void>[] = [];
		if (canManageUsers) {
			tasks.push(loadSummary<AuthDashboardSummary>(
				'/api/auths/dashboard-summary',
				'status akun',
				(value) => authSummary = value
			));
			tasks.push(loadSummary<UserDashboardSummary>(
				'/api/users/dashboard-summary',
				'data user',
				(value) => userSummary = value
			));
		}
		if (canReadGroups) {
			tasks.push(loadSummary<CourseDashboardSummary>(
				'/api/courses/dashboard-summary',
				'operasional akademik',
				(value) => courseSummary = value
			));
		}

		await Promise.all(tasks);
		if (unavailable.length) {
			error = `Sebagian ringkasan belum tersedia: ${unavailable.join(', ')}.`;
		}
		loading = false;
	}

	async function loadSummary<T>(path: string, label: string, assign: (value: T) => void) {
		try {
			const payload = await api<T>(path);
			if (payload.data) assign(payload.data);
		} catch {
			unavailable = [...unavailable, label];
		}
	}

	function accountHealth() {
		if (!authSummary || authSummary.totalUsers === 0) return 0;
		return Math.round((authSummary.activeUsers / authSummary.totalUsers) * 100);
	}
</script>

<svelte:head><title>Dashboard Admin - Divdik Course</title></svelte:head>

<section class="dashboard-heading">
	<div>
		<span class="eyebrow">Administration Overview</span>
		<h1>Dashboard Admin</h1>
		<p>Pantau kesehatan akun, kapasitas peserta, dan operasional akademik dari satu tempat.</p>
	</div>
	<div class="heading-actions">
		{#if canCreateUsers}
			<a class="btn btn-secondary" href="/admin/users">
				<Icons name="upload" size={16} />
				<span>Import User</span>
			</a>
		{/if}
		{#if canCreateGroups}
			<a class="btn btn-primary" href="/admin/groups">
				<Icons name="layers" size={16} />
				<span>Buat Group</span>
			</a>
		{/if}
	</div>
</section>

<Notice {error} />

{#if loading}
	<section class="loading-grid">
		{#each Array(4) as _}<div class="metric-skeleton"></div>{/each}
	</section>
{:else}
	{#if authSummary || userSummary}
		<section class="section-block">
			<div class="section-title">
				<div>
					<span>Account Health</span>
					<h2>Status Pengguna</h2>
				</div>
				{#if authSummary}
					<div class="health-score">
						<strong>{accountHealth()}%</strong>
						<span>akun aktif</span>
					</div>
				{/if}
			</div>

			<div class="metric-grid account-metrics">
				<article class="metric-card">
					<div class="metric-icon blue"><Icons name="users" size={20} /></div>
					<div><span>Total User</span><strong>{userSummary?.totalUsers ?? authSummary?.totalUsers ?? '-'}</strong></div>
				</article>
				<article class="metric-card">
					<div class="metric-icon green"><Icons name="checkCircle" size={20} /></div>
					<div><span>Aktif</span><strong>{authSummary?.activeUsers ?? '-'}</strong></div>
				</article>
				<a class="metric-card actionable warning" href="/admin/auth">
					<div class="metric-icon amber"><Icons name="clock" size={20} /></div>
					<div><span>Menunggu Aktivasi</span><strong>{authSummary?.pendingActivation ?? '-'}</strong></div>
					<Icons name="chevronRight" size={16} />
				</a>
				<a class="metric-card actionable danger" href="/admin/auth">
					<div class="metric-icon red"><Icons name="shield" size={20} /></div>
					<div><span>Banned</span><strong>{authSummary?.bannedUsers ?? '-'}</strong></div>
					<Icons name="chevronRight" size={16} />
				</a>
			</div>
		</section>
	{/if}

	{#if courseSummary || userSummary}
		<section class="section-block">
			<div class="section-title">
				<div>
					<span>Academic Operations</span>
					<h2>Ringkasan Akademik</h2>
				</div>
			</div>

			<div class="metric-grid academic-metrics">
				<article class="metric-card">
					<div class="metric-icon blue"><Icons name="layers" size={20} /></div>
					<div><span>Group Aktif</span><strong>{courseSummary?.activeGroups ?? '-'}</strong></div>
				</article>
				<article class="metric-card">
					<div class="metric-icon violet"><Icons name="graduationCap" size={20} /></div>
					<div><span>Instruktur</span><strong>{courseSummary?.instructors ?? '-'}</strong></div>
				</article>
				<article class="metric-card">
					<div class="metric-icon amber"><Icons name="book" size={20} /></div>
					<div><span>Subject</span><strong>{courseSummary?.subjects ?? '-'}</strong></div>
				</article>
				<article class="metric-card">
					<div class="metric-icon blue"><Icons name="layers" size={20} /></div>
					<div><span>Batch</span><strong>{userSummary?.totalBatches ?? '-'}</strong></div>
				</article>
			</div>
		</section>
	{/if}

	<section class="content-grid">
		<div class="panel attention-panel">
			<div class="panel-head">
				<div><span>Needs Attention</span><h2>Perlu Ditindaklanjuti</h2></div>
			</div>
			<div class="attention-list">
				<a href="/admin/groups">
					<div class="attention-icon red"><Icons name="users" size={18} /></div>
					<div><strong>Group tanpa instruktur</strong><span>Group aktif belum memiliki pengajar.</span></div>
					<b>{courseSummary?.groupsWithoutInstructor ?? '-'}</b>
				</a>
				<a href="/admin/schedules">
					<div class="attention-icon amber"><Icons name="calendar" size={18} /></div>
					<div><strong>Group tanpa jadwal</strong><span>Group aktif belum mempunyai jadwal.</span></div>
					<b>{courseSummary?.groupsWithoutSchedule ?? '-'}</b>
				</a>
			</div>
		</div>

		<div class="panel">
			<div class="panel-head">
				<div><span>Quick Actions</span><h2>Aksi Cepat</h2></div>
			</div>
			<div class="quick-actions">
				{#if canCreateUsers}
					<a href="/admin/users"><Icons name="users" size={19} /><div><strong>Tambah User</strong><span>Manual atau import Excel</span></div></a>
				{/if}
				{#if canCreateBatches}
					<a href="/admin/batches"><Icons name="layers" size={19} /><div><strong>Buat Batch</strong><span>Siapkan angkatan baru</span></div></a>
				{/if}
				{#if canCreateGroups}
					<a href="/admin/groups"><Icons name="graduationCap" size={19} /><div><strong>Buat Group</strong><span>Atur subject dan periode</span></div></a>
				{/if}
				{#if canManageEnrollments}
					<a href="/admin/enrollments"><Icons name="users" size={19} /><div><strong>Tambah Member</strong><span>Atur learner dan instructor</span></div></a>
				{/if}
				{#if canManageSchedules}
					<a href="/admin/schedules"><Icons name="calendar" size={19} /><div><strong>Atur Jadwal</strong><span>Template dan jadwal group</span></div></a>
				{/if}
			</div>
		</div>
	</section>

{/if}

<style>
	.dashboard-heading,
	.section-title,
	.panel-head {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		gap: 1rem;
	}

	.dashboard-heading {
		margin-bottom: 1.75rem;
	}

	.dashboard-heading p {
		margin-top: 0.45rem;
	}

	.eyebrow,
	.section-title > div > span,
	.panel-head > div > span {
		color: var(--primary);
		font-size: 0.7rem;
		font-weight: 800;
		text-transform: uppercase;
		letter-spacing: 0.08em;
	}

	.heading-actions {
		display: flex;
		gap: 0.65rem;
		flex-wrap: wrap;
	}

	.section-block {
		margin-bottom: 1.5rem;
	}

	.section-title {
		align-items: center;
		margin-bottom: 0.8rem;
	}

	.section-title h2,
	.panel-head h2 {
		font-size: 1rem;
		margin-top: 0.1rem;
	}

	.health-score {
		display: flex;
		align-items: baseline;
		gap: 0.35rem;
	}

	.health-score strong {
		color: var(--success);
		font-size: 1.25rem;
	}

	.health-score span {
		color: var(--text-muted);
		font-size: 0.75rem;
	}

	.metric-grid {
		display: grid;
		grid-template-columns: repeat(4, minmax(0, 1fr));
		gap: 0.8rem;
	}

	.academic-metrics {
		grid-template-columns: repeat(6, minmax(0, 1fr));
	}

	.metric-card,
	.panel {
		background: var(--bg-surface);
		border: 1px solid var(--border);
		border-radius: var(--radius-sm);
		box-shadow: var(--shadow-sm);
	}

	.metric-card {
		display: grid;
		grid-template-columns: auto 1fr auto;
		align-items: center;
		gap: 0.75rem;
		min-height: 84px;
		padding: 0.9rem;
		color: inherit;
		text-decoration: none;
	}

	.metric-card > div:nth-child(2) {
		display: grid;
		min-width: 0;
	}

	.metric-card span {
		color: var(--text-muted);
		font-size: 0.72rem;
		font-weight: 700;
	}

	.metric-card strong {
		font-size: 1.35rem;
	}

	.metric-card.actionable:hover {
		border-color: var(--primary-border);
		transform: translateY(-1px);
	}

	.metric-icon,
	.attention-icon {
		display: grid;
		place-items: center;
		border-radius: var(--radius-sm);
	}

	.metric-icon {
		width: 38px;
		height: 38px;
	}

	.blue { color: var(--primary); background: var(--primary-soft); }
	.green { color: var(--success); background: var(--success-soft); }
	.amber { color: var(--warning); background: color-mix(in srgb, var(--warning) 13%, transparent); }
	.red { color: var(--error); background: var(--error-soft); }
	.violet { color: #8b5cf6; background: rgb(139 92 246 / 0.12); }

	.content-grid {
		display: grid;
		grid-template-columns: minmax(0, 1.3fr) minmax(300px, 0.7fr);
		gap: 1rem;
		margin-bottom: 1rem;
	}

	.panel {
		padding: 1.1rem;
	}

	.panel-head {
		align-items: center;
		margin-bottom: 0.75rem;
	}

	.attention-list,
	.quick-actions {
		display: grid;
	}

	.attention-list a {
		display: grid;
		grid-template-columns: auto 1fr auto;
		align-items: center;
		gap: 0.75rem;
		padding: 0.75rem 0;
		border-top: 1px solid var(--border-light);
		color: inherit;
		text-decoration: none;
	}

	.attention-list a:first-child {
		border-top: 0;
	}

	.attention-icon {
		width: 34px;
		height: 34px;
	}

	.attention-list a > div:nth-child(2),
	.quick-actions a > div {
		display: grid;
		min-width: 0;
	}

	.attention-list strong,
	.quick-actions strong {
		font-size: 0.82rem;
	}

	.attention-list span,
	.quick-actions span {
		color: var(--text-muted);
		font-size: 0.72rem;
	}

	.attention-list b {
		font-size: 1rem;
	}

	.quick-actions {
		grid-template-columns: repeat(2, minmax(0, 1fr));
		gap: 0.6rem;
	}

	.quick-actions a {
		display: flex;
		align-items: center;
		gap: 0.65rem;
		min-height: 64px;
		padding: 0.7rem;
		border: 1px solid var(--border-light);
		border-radius: var(--radius-sm);
		color: var(--text-main);
		text-decoration: none;
	}

	.quick-actions a:hover {
		border-color: var(--primary-border);
		background: var(--primary-soft);
		color: var(--primary);
	}

	.loading-grid {
		display: grid;
		grid-template-columns: repeat(4, 1fr);
		gap: 0.8rem;
	}

	.metric-skeleton {
		height: 84px;
		border-radius: var(--radius-sm);
		background: var(--border-light);
		animation: pulse 1.2s ease-in-out infinite alternate;
	}

	@keyframes pulse {
		to { opacity: 0.55; }
	}

	@media (max-width: 1200px) {
		.academic-metrics {
			grid-template-columns: repeat(3, minmax(0, 1fr));
		}
	}

	@media (max-width: 900px) {
		.metric-grid,
		.loading-grid {
			grid-template-columns: repeat(2, minmax(0, 1fr));
		}

		.content-grid {
			grid-template-columns: 1fr;
		}
	}

	@media (max-width: 620px) {
		.dashboard-heading,
		.metric-grid,
		.loading-grid,
		.quick-actions {
			display: grid;
			grid-template-columns: 1fr;
		}

		.heading-actions {
			display: grid;
		}

	}
</style>
