<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import AccessPanel from '$lib/components/AccessPanel.svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import Icons from '$lib/components/Icons.svelte';
	import Notice from '$lib/components/Notice.svelte';
	import PageTitle from '$lib/components/PageTitle.svelte';
	import { api, readSession } from '$lib/api';
	import type { Assessment, AssessmentGrade, GroupDetail, GroupMember, LoginData } from '$lib/types';

	type GradeMap = Record<number, Record<number, AssessmentGrade>>;

	let detail = $state<GroupDetail | null>(null);
	let session = $state<LoginData | null>(null);
	let gradeMap = $state<GradeMap>({});
	let tableType = $state<'ASSIGNMENT' | 'QUIZ'>('ASSIGNMENT');
	let loading = $state(true);
	let error = $state('');
	let success = $state('');

	const groupId = $derived(Number(page.params.id));
	const learners = $derived(detail?.members
		.filter((member) => isLearner(member))
		.sort((a, b) => (a.user?.name ?? '').localeCompare(b.user?.name ?? '')) ?? []
	);
	const tableAssessments = $derived(sortedAssessments(detail?.assessments.filter((item) => item.type === tableType) ?? []));
	const midterm = $derived(detail?.assessments.find((item) => item.type === 'MIDTERM') ?? null);
	const finalExam = $derived(detail?.assessments.find((item) => item.type === 'FINAL_EXAM') ?? null);

	onMount(() => {
		session = readSession();
		void loadGradebook();
	});

	async function loadGradebook() {
		loading = true;
		error = '';
		success = '';
		try {
			const payload = await api<GroupDetail>(`/api/groups/${groupId}`);
			if (!payload.data) throw new Error('Detail group kosong.');
			detail = payload.data;

			const assessments = payload.data.assessments;
			const gradeEntries = await Promise.all(assessments.map(async (assessment) => {
				const gradePayload = await api<AssessmentGrade[]>(`/api/assessments/${assessment.id}/grades`);
				return [assessment.id, gradePayload.data ?? []] as const;
			}));

			const nextMap: GradeMap = {};
			for (const [assessmentId, grades] of gradeEntries) {
				nextMap[assessmentId] = {};
				for (const grade of grades) {
					nextMap[assessmentId][grade.user.id] = grade;
				}
			}
			gradeMap = nextMap;
		} catch (err) {
			error = err instanceof Error ? err.message : 'Gagal memuat gradebook.';
		} finally {
			loading = false;
		}
	}

	function isLearner(member: GroupMember) {
		const role = member.role.toUpperCase();
		return role.includes('PELAJAR') || role.includes('LEARNER');
	}

	function sortedAssessments(assessments: Assessment[]) {
		return [...assessments].sort((a, b) => {
			const aMeeting = a.meetingNumber ?? 999;
			const bMeeting = b.meetingNumber ?? 999;
			if (aMeeting !== bMeeting) return aMeeting - bMeeting;
			return (a.dueAt ?? '').localeCompare(b.dueAt ?? '') || a.title.localeCompare(b.title);
		});
	}

	function gradeFor(assessment: Assessment, userId?: number | null) {
		if (!userId) return null;
		return gradeMap[assessment.id]?.[userId] ?? null;
	}

	function scoreFor(assessment: Assessment, userId?: number | null) {
		const grade = gradeFor(assessment, userId);
		return grade?.score ?? null;
	}

	function scoreLabel(score?: number | null) {
		if (score == null) return '__';
		return Number(score).toLocaleString('id-ID', { maximumFractionDigits: 2 });
	}

	function assessmentAverage(assessment: Assessment) {
		const scores = learners
			.map((member) => scoreFor(assessment, member.user?.id))
			.filter((score): score is number => score != null);
		if (scores.length === 0) return null;
		return scores.reduce((total, score) => total + score, 0) / scores.length;
	}

	function overallAverage(assessments: Assessment[]) {
		const scores = assessments.flatMap((assessment) =>
			learners
				.map((member) => scoreFor(assessment, member.user?.id))
				.filter((score): score is number => score != null)
		);
		if (scores.length === 0) return null;
		return scores.reduce((total, score) => total + score, 0) / scores.length;
	}

	function learnerAverage(member: GroupMember, assessments: Assessment[]) {
		const scores = assessments
			.map((assessment) => scoreFor(assessment, member.user?.id))
			.filter((score): score is number => score != null);
		if (scores.length === 0) return null;
		return scores.reduce((total, score) => total + score, 0) / scores.length;
	}

	function deadlineLabel(value?: string | null) {
		if (!value) return 'Tanpa batas waktu';
		return value.slice(0, 10);
	}

	function fallbackUserName(userId?: number | null) {
		return userId ? `User #${userId}` : 'User belum tersinkron';
	}
</script>

<svelte:head><title>Gradebook - Divdik Course</title></svelte:head>

<AccessPanel authorities={['group.read', 'group.*']}>
	<div class="back-row">
		<button class="btn btn-ghost" type="button" onclick={() => goto(`/groups/${groupId}`)}>
			<Icons name="chevronLeft" size={16} />
			<span>Kembali ke detail group</span>
		</button>
	</div>

	{#if detail}
		<PageTitle
			eyebrow="Gradebook"
			title={`Nilai ${detail.group.name}`}
			description={`${detail.group.subjectName} • ${detail.group.academicYear}`}
		/>
	{:else}
		<PageTitle eyebrow="Gradebook" title="Nilai Group" description="Memuat rekap penilaian." />
	{/if}

	<Notice {error} {success} />

	{#if loading}
		<div class="card">Memuat gradebook...</div>
	{:else if !detail}
		<EmptyState text="Gradebook group tidak ditemukan." />
	{:else}
		<section class="gradebook-shell">
			<div class="gradebook-toolbar">
				<div class="segmented">
					<button class:active={tableType === 'ASSIGNMENT'} type="button" onclick={() => tableType = 'ASSIGNMENT'}>Tugas</button>
					<button class:active={tableType === 'QUIZ'} type="button" onclick={() => tableType = 'QUIZ'}>Quiz</button>
				</div>
				<button class="btn btn-secondary" type="button" onclick={loadGradebook}>
					<Icons name="search" size={16} />
					<span>Refresh</span>
				</button>
			</div>

			<section class="grade-panel">
				<div class="panel-head">
					<div>
						<h3>{tableType === 'ASSIGNMENT' ? 'Rekap Tugas' : 'Rekap Quiz'}</h3>
						<span>{tableAssessments.length} assessment • {learners.length} learner</span>
					</div>
				</div>

				{#if tableAssessments.length === 0}
					<EmptyState text={tableType === 'ASSIGNMENT' ? 'Belum ada tugas untuk group ini.' : 'Belum ada quiz untuk group ini.'} />
				{:else}
					<div class="grade-table-wrap">
						<table class="grade-table">
							<thead>
								<tr>
									<th class="student-col">Learner</th>
									<th class="avg-col">Rata-rata</th>
									{#each tableAssessments as assessment}
										<th>
											<div class="assessment-head">
												<span>{deadlineLabel(assessment.dueAt)}</span>
												<strong>{assessment.title}</strong>
												<small>{assessment.meetingNumber ? `M${assessment.meetingNumber}` : assessment.typeLabel}</small>
											</div>
										</th>
									{/each}
								</tr>
							</thead>
							<tbody>
								<tr class="class-average">
									<td class="student-col">Rata-rata Kelas</td>
									<td class="avg-col">{scoreLabel(overallAverage(tableAssessments))}</td>
									{#each tableAssessments as assessment}
										<td>{scoreLabel(assessmentAverage(assessment))}</td>
									{/each}
								</tr>

								{#each learners as learner}
									<tr>
										<td class="student-col">
											<strong>{learner.user?.name || fallbackUserName(learner.user?.id)}</strong>
											<span>Batch {learner.user?.batch ?? '-'}</span>
										</td>
										<td class="avg-col">{scoreLabel(learnerAverage(learner, tableAssessments))}</td>
										{#each tableAssessments as assessment}
											<td>
												<span class:empty-score={scoreFor(assessment, learner.user?.id) == null} class="score">
													{scoreLabel(scoreFor(assessment, learner.user?.id))}
												</span>
												<small>/100</small>
											</td>
										{/each}
									</tr>
								{/each}
							</tbody>
						</table>
					</div>
				{/if}
			</section>

			<section class="exam-grid">
				<article class="exam-card">
					<div class="exam-head">
						<div>
							<span>Assessment Akhir</span>
							<h3>UTS</h3>
						</div>
						{#if midterm}
							<strong>{midterm.title}</strong>
						{/if}
					</div>
					{#if midterm}
						<div class="exam-list">
							{#each learners as learner}
								<div class="exam-row">
									<span>{learner.user?.name || fallbackUserName(learner.user?.id)}</span>
									<strong>{scoreLabel(scoreFor(midterm, learner.user?.id))}<small>/100</small></strong>
								</div>
							{/each}
						</div>
					{:else}
						<EmptyState text="UTS belum dibuat untuk group ini." />
					{/if}
				</article>

				<article class="exam-card">
					<div class="exam-head">
						<div>
							<span>Assessment Akhir</span>
							<h3>UAS</h3>
						</div>
						{#if finalExam}
							<strong>{finalExam.title}</strong>
						{/if}
					</div>
					{#if finalExam}
						<div class="exam-list">
							{#each learners as learner}
								<div class="exam-row">
									<span>{learner.user?.name || fallbackUserName(learner.user?.id)}</span>
									<strong>{scoreLabel(scoreFor(finalExam, learner.user?.id))}<small>/100</small></strong>
								</div>
							{/each}
						</div>
					{:else}
						<EmptyState text="UAS belum dibuat untuk group ini." />
					{/if}
				</article>
			</section>
		</section>
	{/if}
</AccessPanel>

<style>
	.back-row {
		margin-bottom: 1rem;
	}

	.gradebook-shell {
		display: grid;
		gap: 1.25rem;
	}

	.gradebook-toolbar {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 1rem;
		flex-wrap: wrap;
	}

	.segmented {
		display: inline-grid;
		grid-template-columns: repeat(2, minmax(96px, 1fr));
		border: 1px solid var(--border);
		border-radius: var(--radius-sm);
		background: var(--bg-surface);
		overflow: hidden;
	}

	.segmented button {
		border: 0;
		background: transparent;
		color: var(--text-muted);
		padding: 0.7rem 1rem;
		font-weight: 800;
		cursor: pointer;
	}

	.segmented button.active {
		background: var(--primary);
		color: var(--text-on-brand);
	}

	.grade-panel,
	.exam-card {
		background: var(--bg-surface);
		border: 1px solid var(--border);
		border-radius: var(--radius);
		box-shadow: var(--shadow-sm);
	}

	.grade-panel {
		padding: 1rem;
		min-width: 0;
	}

	.panel-head,
	.exam-head {
		display: flex;
		justify-content: space-between;
		align-items: flex-start;
		gap: 1rem;
		margin-bottom: 1rem;
	}

	.panel-head h3,
	.exam-head h3 {
		font-size: 1.05rem;
		letter-spacing: 0;
	}

	.panel-head span,
	.exam-head span {
		color: var(--text-muted);
		font-size: 0.78rem;
		font-weight: 800;
	}

	.grade-table-wrap {
		overflow: auto;
		border: 1px solid var(--border-light);
		border-radius: var(--radius-sm);
		max-width: 100%;
	}

	.grade-table {
		width: max-content;
		min-width: 100%;
		border-collapse: collapse;
		background: var(--bg-surface);
	}

	.grade-table th,
	.grade-table td {
		border-right: 1px solid var(--border-light);
		border-bottom: 1px solid var(--border-light);
		padding: 0.75rem;
		text-align: left;
		vertical-align: middle;
		min-width: 142px;
	}

	.grade-table th:last-child,
	.grade-table td:last-child {
		border-right: 0;
	}

	.grade-table tbody tr:last-child td {
		border-bottom: 0;
	}

	.student-col {
		position: sticky;
		left: 0;
		z-index: 2;
		min-width: 220px;
		background: var(--bg-surface);
	}

	thead .student-col {
		z-index: 3;
	}

	.avg-col {
		min-width: 108px;
		font-weight: 800;
	}

	.assessment-head {
		display: grid;
		gap: 0.25rem;
	}

	.assessment-head span,
	.assessment-head small {
		color: var(--text-muted);
		font-size: 0.72rem;
		font-weight: 700;
	}

	.assessment-head strong {
		color: var(--primary);
		font-size: 0.85rem;
		overflow-wrap: anywhere;
	}

	.student-col strong {
		display: block;
		overflow-wrap: anywhere;
	}

	.student-col span {
		color: var(--text-muted);
		font-size: 0.78rem;
		font-weight: 700;
	}

	.class-average td {
		background: var(--bg-app);
		font-weight: 800;
	}

	.score {
		color: #047857;
		font-weight: 900;
	}

	.empty-score {
		color: var(--text-muted);
	}

	td small,
	.exam-row small {
		color: var(--text-muted);
		font-size: 0.72rem;
		margin-left: 0.1rem;
	}

	.exam-grid {
		display: grid;
		grid-template-columns: repeat(2, minmax(0, 1fr));
		gap: 1.25rem;
	}

	.exam-card {
		padding: 1rem;
		min-width: 0;
	}

	.exam-head strong {
		color: var(--primary);
		font-size: 0.9rem;
		text-align: right;
		overflow-wrap: anywhere;
	}

	.exam-list {
		display: grid;
		gap: 0.5rem;
	}

	.exam-row {
		border: 1px solid var(--border-light);
		border-radius: var(--radius-sm);
		padding: 0.75rem;
		display: flex;
		justify-content: space-between;
		gap: 1rem;
		align-items: center;
	}

	.exam-row span {
		overflow-wrap: anywhere;
	}

	.exam-row strong {
		color: #047857;
		white-space: nowrap;
	}

	@media (max-width: 860px) {
		.exam-grid {
			grid-template-columns: 1fr;
		}
	}
</style>
