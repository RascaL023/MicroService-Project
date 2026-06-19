<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import AccessPanel from '$lib/components/AccessPanel.svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import Icons from '$lib/components/Icons.svelte';
	import Notice from '$lib/components/Notice.svelte';
	import PageTitle from '$lib/components/PageTitle.svelte';
	import { api, dayLabel, hasAnyAuthority, pageItems, readSession, timeLabel } from '$lib/api';
	import type { Assessment, AssessmentGrade, Enrollment, Group, GroupDetail, GroupMeeting, GroupMember, LoginData, PageData, ScheduleTemplate, Subject, User } from '$lib/types';

	const days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
	const assessmentTypes = [
		{ value: 'ASSIGNMENT', label: 'Tugas' },
		{ value: 'QUIZ', label: 'Quiz' },
		{ value: 'MIDTERM', label: 'UTS' },
		{ value: 'FINAL_EXAM', label: 'UAS' }
	];

	let detail = $state<GroupDetail | null>(null);
	let subjects = $state<Subject[]>([]);
	let users = $state<User[]>([]);
	let templates = $state<ScheduleTemplate[]>([]);
	let session = $state<LoginData | null>(null);
	let editForm = $state({ name: '', subjectId: '', academicYear: '', isDone: '' });
	let memberForm = $state({ query: '', userId: '', role: 'LEARNER' });
	let memberPatchForm = $state({ enrollmentId: '', query: '', userId: '', role: 'LEARNER' });
	let scheduleForm = $state({ dayOfWeek: 'MONDAY', templateId: '' });
	let meetingForm = $state({ action: 'START', meetingId: '', subjectMaterialId: '', title: '', meetingDate: '', note: '' });
	let assessmentForm = $state({
		id: '',
		type: 'ASSIGNMENT',
		title: '',
		description: '',
		groupMeetingId: '',
		dueAt: ''
	});
	let assessmentFile = $state<File | null>(null);
	let gradeAssessment = $state<Assessment | null>(null);
	let gradeRows = $state<{ userId: number; name: string; batch: number | null; score: string; feedback: string; saved: boolean }[]>([]);
	let showEdit = $state(false);
	let showMemberModal = $state(false);
	let showMemberEditModal = $state(false);
	let showScheduleModal = $state(false);
	let showMeetingModal = $state(false);
	let showAssessmentModal = $state(false);
	let showGradeModal = $state(false);
	let loadingGrades = $state(false);
	let loading = $state(true);
	let searchingUsers = $state(false);
	let activeDetailTab = $state<'meetings' | 'assessments' | 'schedules' | 'members'>('meetings');
	let error = $state('');
	let success = $state('');

	const groupId = $derived(Number(page.params.id));
	const portalPrefix = $derived(
		page.url.pathname.startsWith('/admin/') ? '/admin' :
		page.url.pathname.startsWith('/app/') ? '/app' : ''
	);
	const canEdit = $derived(hasAnyAuthority(session, ['group.update', 'group.*']));
	const canManageMembers = $derived(hasAnyAuthority(session, ['enrollment.create', 'enrollment.update', 'enrollment.delete', 'enrollment.*']));
	const canManageSchedules = $derived(hasAnyAuthority(session, ['group-schedule.create', 'group-schedule.delete', 'group-schedule.*']));
	const isGroupInstructor = $derived(Boolean(detail?.members.some((member) =>
		member.user?.id === session?.userId && member.role.toUpperCase().includes('INSTRUK')
	)));
	const canManageMeetings = $derived(isGroupInstructor || hasAnyAuthority(session, ['course.*', 'group.*', 'enrollment.*']));
	const canManageAssessments = $derived(canManageMeetings);
	const learners = $derived(detail?.members.filter((member) => member.role.toUpperCase().includes('PELAJAR') || member.role.toUpperCase().includes('LEARNER')) ?? []);
	const doneMeetingCount = $derived(detail?.meetings.filter((meeting) => meeting.status === 'DONE').length ?? 0);
	const meetingOptions = $derived(detail?.meetings.filter((meeting) => meeting.id !== null) ?? []);

	onMount(() => {
		session = readSession();
		void loadDetail();
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

	async function loadDetail() {
		if (!groupId) {
			error = 'Group tidak valid.';
			loading = false;
			return;
		}

		loading = true;
		try {
			const payload = await api<GroupDetail>(`/api/groups/${groupId}`);
			if (!payload.data) throw new Error('Detail group kosong.');
			detail = payload.data;
		} catch (err) {
			error = err instanceof Error ? err.message : 'Gagal memuat detail group.';
		} finally {
			loading = false;
		}
	}

	async function loadSubjects() {
		if (subjects.length > 0) return;
		const payload = await api<PageData<Subject> | Subject[]>('/api/subjects?sort=name,asc');
		subjects = pageItems(payload);
	}

	async function loadTemplates() {
		if (templates.length > 0) return;
		const payload = await api<PageData<ScheduleTemplate> | ScheduleTemplate[]>('/api/schedule-templates?sort=startTime,asc');
		templates = pageItems(payload);
		if (!scheduleForm.templateId && templates[0]) scheduleForm.templateId = String(templates[0].id);
	}

	async function openEdit() {
		const current = detail;
		if (!current) return;
		await submit(async () => {
			await loadSubjects();
			editForm = {
				name: current.group.name,
				subjectId: String(current.group.subjectId),
				academicYear: current.group.academicYear,
				isDone: statusIsDone(current.group.status) ? 'true' : 'false'
			};
			showEdit = true;
		});
	}

	async function updateGroup() {
		const current = detail;
		if (!current) return;

		await submit(async () => {
			const body: Record<string, unknown> = {
				name: editForm.name,
				subjectId: Number(editForm.subjectId),
				academicYear: editForm.academicYear,
				isDone: editForm.isDone === 'true'
			};
			await api<Group>(`/api/groups/${current.group.id}`, {
				method: 'PATCH',
				body: JSON.stringify(body)
			});
			showEdit = false;
			await loadDetail();
			success = 'Group berhasil diupdate.';
		});
	}

	async function openMemberModal() {
		memberForm = { query: '', userId: '', role: 'LEARNER' };
		users = [];
		showMemberModal = true;
	}

	async function searchUsers() {
		await submit(async () => {
			if (!memberForm.query.trim()) {
				users = [];
				return;
			}

			searchingUsers = true;
			try {
				const query = new URLSearchParams({
					name: memberForm.query.trim(),
					size: '8',
					sort: 'name,asc'
				});
				const payload = await api<PageData<User> | User[]>(`/api/users?${query}`);
				users = pageItems(payload);
			} finally {
				searchingUsers = false;
			}
		});
	}

	async function searchPatchUsers() {
		await submit(async () => {
			if (!memberPatchForm.query.trim()) {
				users = [];
				return;
			}

			searchingUsers = true;
			try {
				const query = new URLSearchParams({
					name: memberPatchForm.query.trim(),
					size: '8',
					sort: 'name,asc'
				});
				const payload = await api<PageData<User> | User[]>(`/api/users?${query}`);
				users = pageItems(payload);
			} finally {
				searchingUsers = false;
			}
		});
	}

	function selectUser(user: User) {
		memberForm.userId = String(user.id);
		memberForm.query = user.name;
	}

	function selectPatchUser(user: User) {
		memberPatchForm.userId = String(user.id);
		memberPatchForm.query = user.name;
	}

	async function addMember() {
		const current = detail;
		if (!current) return;

		await submit(async () => {
			await api('/api/enrollments', {
				method: 'POST',
				body: JSON.stringify({
					userId: Number(memberForm.userId),
					groupId: current.group.id,
					role: memberForm.role
				})
			});
			showMemberModal = false;
			await loadDetail();
			success = 'Member berhasil ditambahkan.';
		});
	}

	function openMemberEditModal(member: GroupMember) {
		memberPatchForm = {
			enrollmentId: String(member.enrollmentId),
			query: member.user?.name || fallbackUserName(member.user?.id),
			userId: member.user?.id ? String(member.user.id) : '',
			role: member.role.toUpperCase().includes('INSTR') ? 'INSTRUCTOR' : 'LEARNER'
		};
		users = [];
		showMemberEditModal = true;
	}

	async function updateMember() {
		await submit(async () => {
			const body: Record<string, unknown> = {
				role: memberPatchForm.role
			};
			if (memberPatchForm.userId) body.userId = Number(memberPatchForm.userId);

			await api<Enrollment>(`/api/enrollments/${memberPatchForm.enrollmentId}`, {
				method: 'PATCH',
				body: JSON.stringify(body)
			});
			showMemberEditModal = false;
			await loadDetail();
			success = 'Data member berhasil diupdate.';
		});
	}

	async function removeMember(enrollmentId: number) {
		if (!confirm('Hapus member dari group ini?')) return;
		await submit(async () => {
			await api<null>(`/api/enrollments/${enrollmentId}`, { method: 'DELETE' });
			await loadDetail();
			success = 'Member berhasil dihapus.';
		});
	}

	async function openScheduleModal() {
		await submit(async () => {
			await loadTemplates();
			scheduleForm = {
				dayOfWeek: 'MONDAY',
				templateId: templates[0] ? String(templates[0].id) : ''
			};
			showScheduleModal = true;
		});
	}

	async function addSchedule() {
		const current = detail;
		if (!current) return;

		await submit(async () => {
			await api('/api/group-schedules', {
				method: 'POST',
				body: JSON.stringify({
					groupId: current.group.id,
					dayOfWeek: scheduleForm.dayOfWeek,
					templateId: Number(scheduleForm.templateId)
				})
			});
			showScheduleModal = false;
			await loadDetail();
			success = 'Jadwal berhasil ditambahkan.';
		});
	}

	async function removeSchedule(scheduleId: number) {
		if (!confirm('Hapus jadwal group ini?')) return;
		await submit(async () => {
			await api<null>(`/api/group-schedules/${scheduleId}`, { method: 'DELETE' });
			await loadDetail();
			success = 'Jadwal berhasil dihapus.';
		});
	}

	function openStartMeeting(meeting: GroupMeeting) {
		meetingForm = {
			action: 'START',
			meetingId: '',
			subjectMaterialId: String(meeting.subjectMaterialId),
			title: `Pertemuan ${meeting.meetingNumber} • ${meeting.title}`,
			meetingDate: todayInputValue(),
			note: ''
		};
		showMeetingModal = true;
	}

	function openDoneMeeting(meeting: GroupMeeting) {
		if (!meeting.id) return;

		meetingForm = {
			action: 'DONE',
			meetingId: String(meeting.id),
			subjectMaterialId: String(meeting.subjectMaterialId),
			title: `Pertemuan ${meeting.meetingNumber} • ${meeting.title}`,
			meetingDate: meeting.meetingDate ?? '',
			note: meeting.note ?? ''
		};
		showMeetingModal = true;
	}

	async function saveMeetingProgress() {
		const current = detail;
		if (!current) return;

		await submit(async () => {
			if (meetingForm.action === 'START') {
				await api<GroupMeeting>('/api/group-meetings/start', {
					method: 'POST',
					body: JSON.stringify({
						groupId: current.group.id,
						subjectMaterialId: Number(meetingForm.subjectMaterialId),
						meetingDate: meetingForm.meetingDate || null,
						note: meetingForm.note || null
					})
				});
				success = 'Meeting berhasil dimulai.';
			} else {
				await api<GroupMeeting>(`/api/group-meetings/${meetingForm.meetingId}/done`, {
					method: 'PATCH',
					body: JSON.stringify({ note: meetingForm.note || null })
				});
				success = 'Meeting berhasil ditandai selesai.';
			}

			showMeetingModal = false;
			await loadDetail();
		});
	}

	function openAssessmentModal(assessment?: Assessment) {
		assessmentForm = assessment ? {
			id: String(assessment.id),
			type: assessment.type,
			title: assessment.title,
			description: assessment.description ?? '',
			groupMeetingId: assessment.groupMeetingId ? String(assessment.groupMeetingId) : '',
			dueAt: assessment.dueAt ? assessment.dueAt.slice(0, 16) : ''
		} : {
			id: '',
			type: 'ASSIGNMENT',
			title: '',
			description: '',
			groupMeetingId: meetingOptions[0]?.id ? String(meetingOptions[0].id) : '',
			dueAt: ''
		};
		assessmentFile = null;
		showAssessmentModal = true;
	}

	async function saveAssessment() {
		const current = detail;
		if (!current) return;

		await submit(async () => {
			const formData = new FormData();
			formData.set('groupId', String(current.group.id));
			formData.set('type', assessmentForm.type);
			formData.set('title', assessmentForm.title);
			if (assessmentForm.description.trim()) formData.set('description', assessmentForm.description.trim());
			if (assessmentForm.groupMeetingId) formData.set('groupMeetingId', assessmentForm.groupMeetingId);
			if (assessmentForm.dueAt) formData.set('dueAt', assessmentForm.dueAt);
			if (assessmentFile) formData.set('file', assessmentFile);

			if (assessmentForm.id) {
				await api<Assessment>(`/api/assessments/${assessmentForm.id}`, {
					method: 'PATCH',
					body: formData
				});
				success = 'Assessment berhasil diupdate.';
			} else {
				await api<Assessment>('/api/assessments', {
					method: 'POST',
					body: formData
				});
				success = 'Assessment berhasil dibuat.';
			}

			showAssessmentModal = false;
			await loadDetail();
		});
	}

	async function deleteAssessment(assessmentId: number) {
		if (!confirm('Hapus assessment ini?')) return;

		await submit(async () => {
			await api<null>(`/api/assessments/${assessmentId}`, { method: 'DELETE' });
			await loadDetail();
			success = 'Assessment berhasil dihapus.';
		});
	}

	async function downloadAssessment(assessment: Assessment) {
		if (!assessment.hasFile) return;

		await submit(async () => {
			const response = await fetch(`/api/assessments/${assessment.id}/download`, {
				headers: session?.sessionId ? { Authorization: `Session ${session.sessionId}` } : undefined
			});
			if (!response.ok) throw new Error('Gagal download lampiran assessment.');

			const blob = await response.blob();
			const url = URL.createObjectURL(blob);
			const anchor = document.createElement('a');
			anchor.href = url;
			anchor.download = assessment.originalFilename || `assessment-${assessment.id}`;
			anchor.click();
			URL.revokeObjectURL(url);
		});
	}

	async function openGradeModal(assessment: Assessment) {
		const current = detail;
		if (!current) return;

		gradeAssessment = assessment;
		showGradeModal = true;
		loadingGrades = true;
		error = '';
		success = '';

		try {
			const payload = await api<AssessmentGrade[]>(`/api/assessments/${assessment.id}/grades`);
			const grades = payload.data ?? [];
			const gradesByUserId = new Map(grades.map((grade) => [grade.user.id, grade]));

			gradeRows = learners.map((member) => {
				const userId = member.user?.id ?? 0;
				const grade = gradesByUserId.get(userId);
				return {
					userId,
					name: member.user?.name || fallbackUserName(userId),
					batch: member.user?.batch ?? null,
					score: grade?.score != null ? String(grade.score) : '',
					feedback: grade?.feedback ?? '',
					saved: Boolean(grade)
				};
			}).filter((row) => row.userId > 0);
		} catch (err) {
			error = err instanceof Error ? err.message : 'Gagal memuat nilai assessment.';
		} finally {
			loadingGrades = false;
		}
	}

	async function saveGrades() {
		const assessment = gradeAssessment;
		if (!assessment) return;

		await submit(async () => {
			const grades = gradeRows
				.filter((row) => hasScore(row.score))
				.map((row) => ({
					userId: row.userId,
					score: scoreValue(row.score),
					feedback: row.feedback.trim() || null
				}));

			if (grades.length === 0) throw new Error('Isi minimal satu nilai.');

			await api<AssessmentGrade[]>(`/api/assessments/${assessment.id}/grades`, {
				method: 'PATCH',
				body: JSON.stringify({ grades })
			});

			showGradeModal = false;
			success = 'Nilai berhasil disimpan.';
		});
	}

	function statusIsDone(status: string) {
		const normalized = status.toLowerCase();
		return normalized.includes('passed') || normalized.includes('selesai') || normalized.includes('lulus') || status === 'DONE';
	}

	function fallbackUserName(userId?: number) {
		return userId ? `User #${userId}` : 'User belum tersinkron';
	}

	function todayInputValue() {
		const today = new Date();
		const offset = today.getTimezoneOffset() * 60000;
		return new Date(today.getTime() - offset).toISOString().slice(0, 10);
	}

	function meetingStatusClass(status: string) {
		if (status === 'DONE') return 'badge-green';
		if (status === 'STARTED') return 'badge-blue';
		return '';
	}

	function isMeetingAssessmentType(type: string) {
		return type === 'ASSIGNMENT' || type === 'QUIZ';
	}

	function formatDateTime(value?: string | null) {
		if (!value) return 'Tanpa deadline';

		return value.slice(0, 16).replace('T', ' ');
	}

	function fileSizeLabel(size?: number | null) {
		if (!size) return '';
		if (size < 1024 * 1024) return `${Math.ceil(size / 1024)} KB`;

		return `${(size / 1024 / 1024).toFixed(1)} MB`;
	}

	function gradeAverage() {
		const scores = gradeRows
			.map((row) => scoreValue(row.score))
			.filter((score) => Number.isFinite(score));
		if (scores.length === 0) return '-';

		return (scores.reduce((total, score) => total + score, 0) / scores.length).toFixed(1);
	}

	function hasScore(score: string | number) {
		return String(score).trim() !== '';
	}

	function scoreValue(score: string | number) {
		return Number(score);
	}
</script>

<svelte:head><title>Group Detail - Divdik Course</title></svelte:head>

<AccessPanel authorities={['group.read', 'group.*']}>
	<div class="back-row">
		<button class="btn btn-ghost" type="button" onclick={() => goto(`${portalPrefix}/groups`)}>
			<Icons name="chevronLeft" size={16} />
			<span>Kembali ke group</span>
		</button>
	</div>

	{#if detail}
		<PageTitle
			eyebrow="Group Detail"
			title={detail.group.name}
			description={`${detail.group.subjectName} • ${detail.group.academicYear}`}
		/>
	{:else}
		<PageTitle eyebrow="Group Detail" title="Detail Group" description="Memuat informasi group." />
	{/if}

	<Notice {error} {success} />

	{#if loading}
		<div class="card">Memuat detail group...</div>
	{:else if !detail}
		<EmptyState text="Detail group tidak ditemukan." />
	{:else}
		<section class="detail-shell">
			<div class="summary-card">
				<div class="summary-meta">
					<div>
						<span>Academic Year</span>
						<strong>{detail.group.academicYear}</strong>
					</div>
					<div>
						<span>Status</span>
						<strong>{detail.group.status}</strong>
					</div>
					<div>
						<span>Members</span>
						<strong>{detail.members.length}</strong>
					</div>
					<div>
						<span>Schedules</span>
						<strong>{detail.schedules.length}</strong>
					</div>
					<div>
						<span>Meetings</span>
						<strong>{doneMeetingCount}/{detail.meetings.length}</strong>
					</div>
					<div>
						<span>Assessments</span>
						<strong>{detail.assessments.length}</strong>
					</div>
				</div>

				<div class="summary-actions">
					<button class="btn btn-secondary" type="button" onclick={() => goto(`${portalPrefix}/groups/${groupId}/grades`)}>
						<Icons name="checkCircle" size={17} />
						<span>Gradebook</span>
					</button>
					{#if canEdit}
						<button class="btn btn-primary edit-button" type="button" onclick={openEdit}>
							<Icons name="layers" size={17} />
							<span>Edit Group</span>
						</button>
					{/if}
				</div>
			</div>

			<div class="detail-tabs" role="tablist" aria-label="Group detail">
				<button class:active={activeDetailTab === 'meetings'} type="button" onclick={() => activeDetailTab = 'meetings'}>
					<Icons name="calendar" size={16} />
					<span>Pertemuan</span>
					<strong>{doneMeetingCount}/{detail.meetings.length}</strong>
				</button>
				<button class:active={activeDetailTab === 'assessments'} type="button" onclick={() => activeDetailTab = 'assessments'}>
					<Icons name="file" size={16} />
					<span>Assessment</span>
					<strong>{detail.assessments.length}</strong>
				</button>
				<button class:active={activeDetailTab === 'schedules'} type="button" onclick={() => activeDetailTab = 'schedules'}>
					<Icons name="calendar" size={16} />
					<span>Jadwal</span>
					<strong>{detail.schedules.length}</strong>
				</button>
				<button class:active={activeDetailTab === 'members'} type="button" onclick={() => activeDetailTab = 'members'}>
					<Icons name="users" size={16} />
					<span>Member</span>
					<strong>{detail.members.length}</strong>
				</button>
			</div>

			{#if activeDetailTab === 'meetings'}
			<section class="panel meeting-panel">
				<div class="panel-head">
					<div>
						<h3>Pertemuan</h3>
						<span>{doneMeetingCount} dari {detail.meetings.length} selesai</span>
					</div>
					{#if isGroupInstructor}
						<span class="badge badge-blue">Instruktur group</span>
					{/if}
				</div>

				{#if detail.meetings.length === 0}
					<EmptyState text="Subject ini belum punya materi pertemuan." />
				{:else}
					<div class="meeting-list">
						{#each detail.meetings as meeting}
							<div class:meeting-done={meeting.status === 'DONE'} class:meeting-started={meeting.status === 'STARTED'} class="meeting-item">
								<div class="meeting-number">M{meeting.meetingNumber}</div>
								<div class="item-main">
									<strong>{meeting.title}</strong>
									<p>{meeting.description || 'Tidak ada deskripsi materi.'}</p>
									<div class="meeting-meta">
										<span>{meeting.meetingDate ? meeting.meetingDate : 'Tanggal belum ada'}</span>
										{#if meeting.note}
											<span>{meeting.note}</span>
										{/if}
									</div>
								</div>
								<span class={`badge ${meetingStatusClass(meeting.status)}`}>{meeting.displayStatus}</span>
								{#if canManageMeetings}
									<div class="item-actions">
										{#if meeting.status === 'NOT_STARTED'}
											<button class="btn btn-secondary compact" type="button" onclick={() => openStartMeeting(meeting)}>
												<Icons name="calendar" size={16} />
												<span>Start</span>
											</button>
										{:else if meeting.status === 'STARTED'}
											<button class="btn btn-primary compact" type="button" onclick={() => openDoneMeeting(meeting)}>
												<Icons name="checkCircle" size={16} />
												<span>Done</span>
											</button>
										{/if}
									</div>
								{/if}
							</div>
						{/each}
					</div>
				{/if}
			</section>
			{/if}

			{#if activeDetailTab === 'assessments'}
			<section class="panel assessment-panel">
				<div class="panel-head">
					<div>
						<h3>Assessment</h3>
						<span>{detail.assessments.length} item penilaian</span>
					</div>
					{#if canManageAssessments}
						<button class="btn btn-secondary compact" type="button" onclick={() => openAssessmentModal()}>
							<Icons name="file" size={16} />
							<span>Tambah</span>
						</button>
					{/if}
				</div>

				{#if detail.assessments.length === 0}
					<EmptyState text="Belum ada assessment untuk group ini." />
				{:else}
					<div class="assessment-list">
						{#each detail.assessments as assessment}
							<div class="assessment-item">
								<div class="assessment-icon">
									<Icons name={assessment.hasFile ? 'file' : 'book'} size={18} />
								</div>
								<div class="item-main">
									<div class="assessment-title-row">
										<strong>{assessment.title}</strong>
										<span class="badge badge-blue">{assessment.typeLabel}</span>
									</div>
									<p>{assessment.description || 'Tidak ada deskripsi.'}</p>
									<div class="assessment-meta">
										<span>{formatDateTime(assessment.dueAt)}</span>
										{#if assessment.meetingNumber}
											<span>M{assessment.meetingNumber} • {assessment.meetingTitle}</span>
										{/if}
										{#if assessment.originalFilename}
											<span>{assessment.originalFilename} {fileSizeLabel(assessment.fileSize)}</span>
										{/if}
									</div>
								</div>
								<div class="item-actions assessment-actions">
									{#if assessment.hasFile}
										<button class="btn btn-ghost icon-btn" type="button" aria-label="Download assessment" onclick={() => downloadAssessment(assessment)}>
											<Icons name="download" size={16} />
										</button>
									{/if}
									{#if canManageAssessments}
										<button class="btn btn-ghost icon-btn" type="button" aria-label="Beri nilai" onclick={() => openGradeModal(assessment)}>
											<Icons name="checkCircle" size={16} />
										</button>
										<button class="btn btn-ghost icon-btn" type="button" aria-label="Edit assessment" onclick={() => openAssessmentModal(assessment)}>
											<Icons name="edit" size={16} />
										</button>
										<button class="btn btn-ghost icon-btn danger" type="button" aria-label="Hapus assessment" onclick={() => deleteAssessment(assessment.id)}>
											<Icons name="x" size={16} />
										</button>
									{/if}
								</div>
							</div>
						{/each}
					</div>
				{/if}
			</section>
			{/if}

			{#if activeDetailTab === 'schedules'}
				<section class="panel">
					<div class="panel-head">
						<div>
							<h3>Jadwal</h3>
							<span>{detail.schedules.length} slot</span>
						</div>
						{#if canManageSchedules}
							<button class="btn btn-secondary compact" type="button" onclick={openScheduleModal}>
								<Icons name="calendar" size={16} />
								<span>Tambah</span>
							</button>
						{/if}
					</div>

					{#if detail.schedules.length === 0}
						<EmptyState text="Group ini belum punya jadwal." />
					{:else}
						<div class="schedule-list">
							{#each detail.schedules as schedule}
								<div class="schedule-item">
									<div class="schedule-day">{dayLabel(schedule.dayOfWeek)}</div>
									<div class="item-main">
										<strong>{timeLabel(schedule.startTime, schedule.endTime)}</strong>
										<p>{schedule.templateName || 'Template lama belum terhubung'}</p>
									</div>
									{#if canManageSchedules}
										<button class="btn btn-ghost icon-btn danger" type="button" onclick={() => removeSchedule(schedule.id)}>
											<Icons name="x" size={16} />
										</button>
									{/if}
								</div>
							{/each}
						</div>
					{/if}
				</section>
			{/if}

			{#if activeDetailTab === 'members'}
				<section class="panel">
					<div class="panel-head">
						<div>
							<h3>Member</h3>
							<span>{detail.members.length} orang</span>
						</div>
						{#if canManageMembers}
							<button class="btn btn-secondary compact" type="button" onclick={openMemberModal}>
								<Icons name="users" size={16} />
								<span>Tambah</span>
							</button>
						{/if}
					</div>

					{#if detail.members.length === 0}
						<EmptyState text="Belum ada member di group ini." />
					{:else}
						<div class="member-list">
							{#each detail.members as member}
								<div class="member-item">
									<div class="avatar">{member.user?.name?.slice(0, 1).toUpperCase() || '#'}</div>
									<div class="item-main">
										<strong>{member.user?.name || fallbackUserName(member.user?.id)}</strong>
										<p>Batch {member.user?.batch ?? '-'} • {member.user?.gender ?? '-'}</p>
									</div>
									<span class="badge badge-blue">{member.role}</span>
									{#if canManageMembers}
										<div class="item-actions">
											<button class="btn btn-ghost icon-btn" type="button" aria-label="Edit member" onclick={() => openMemberEditModal(member)}>
												<Icons name="edit" size={16} />
											</button>
											<button class="btn btn-ghost icon-btn danger" type="button" aria-label="Hapus member" onclick={() => removeMember(member.enrollmentId)}>
												<Icons name="x" size={16} />
											</button>
										</div>
									{/if}
								</div>
							{/each}
						</div>
					{/if}
				</section>
			{/if}
		</section>
	{/if}
</AccessPanel>

{#if showMeetingModal && detail}
	<div class="modal-backdrop" role="presentation" onclick={() => showMeetingModal = false}>
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
					<span class="nav-label flush">Pertemuan</span>
					<h3>{meetingForm.action === 'START' ? 'Mulai Meeting' : 'Selesaikan Meeting'}</h3>
				</div>
				<button class="btn btn-ghost icon-btn" type="button" onclick={() => showMeetingModal = false}>
					<Icons name="x" size={18} />
				</button>
			</div>

			<form class="modal-form" onsubmit={(event) => { event.preventDefault(); void saveMeetingProgress(); }}>
				<div class="form-note">
					<strong>{meetingForm.title}</strong>
					<p>{meetingForm.action === 'START' ? 'Meeting akan tercatat sebagai sedang berjalan.' : 'Meeting akan dikunci sebagai selesai.'}</p>
				</div>

				{#if meetingForm.action === 'START'}
					<label>
						<span>Tanggal Meeting</span>
						<input type="date" bind:value={meetingForm.meetingDate} required />
					</label>
				{/if}

				<label>
					<span>Catatan</span>
					<textarea bind:value={meetingForm.note} rows="3" placeholder="Opsional"></textarea>
				</label>

				<div class="modal-actions">
					<button class="btn btn-ghost" type="button" onclick={() => showMeetingModal = false}>Batal</button>
					<button class="btn btn-primary" type="submit">
						{meetingForm.action === 'START' ? 'Mulai' : 'Tandai Selesai'}
					</button>
				</div>
			</form>
		</section>
	</div>
{/if}

{#if showGradeModal && gradeAssessment}
	<div class="modal-backdrop" role="presentation" onclick={() => showGradeModal = false}>
		<section
			class="modal-panel grade-modal"
			role="dialog"
			aria-modal="true"
			tabindex="-1"
			onclick={(event) => event.stopPropagation()}
			onkeydown={(event) => event.stopPropagation()}
		>
			<div class="modal-head">
				<div>
					<span class="nav-label flush">Nilai Assessment</span>
					<h3>{gradeAssessment.title}</h3>
				</div>
				<button class="btn btn-ghost icon-btn" type="button" onclick={() => showGradeModal = false}>
					<Icons name="x" size={18} />
				</button>
			</div>

			<div class="grade-summary">
				<div>
					<span>Jenis</span>
					<strong>{gradeAssessment.typeLabel}</strong>
				</div>
				<div>
					<span>Dinilai</span>
					<strong>{gradeRows.filter((row) => hasScore(row.score)).length}/{gradeRows.length}</strong>
				</div>
				<div>
					<span>Rata-rata</span>
					<strong>{gradeAverage()}</strong>
				</div>
			</div>

			{#if loadingGrades}
				<div class="picker-empty">Memuat daftar nilai...</div>
			{:else if gradeRows.length === 0}
				<EmptyState text="Belum ada learner yang bisa dinilai di group ini." />
			{:else}
				<form class="gradebook" onsubmit={(event) => { event.preventDefault(); void saveGrades(); }}>
					<div class="grade-row grade-head">
						<span>Learner</span>
						<span>Nilai</span>
						<span>Feedback</span>
						<span>Status</span>
					</div>

					{#each gradeRows as row}
						<div class="grade-row">
							<div class="grade-user">
								<div class="avatar">{row.name.slice(0, 1).toUpperCase()}</div>
								<div>
									<strong>{row.name}</strong>
									<p>Batch {row.batch ?? '-'}</p>
								</div>
							</div>
							<input
								class="grade-score"
								type="number"
								min="0"
								max="100"
								step="0.01"
								placeholder="0-100"
								bind:value={row.score}
							/>
							<input
								class="grade-feedback"
								placeholder="Catatan singkat"
								bind:value={row.feedback}
							/>
							<span class:badge-green={row.saved} class="badge">{row.saved ? 'Tersimpan' : 'Baru'}</span>
						</div>
					{/each}

					<div class="modal-actions">
						<button class="btn btn-ghost" type="button" onclick={() => showGradeModal = false}>Batal</button>
						<button class="btn btn-primary" type="submit">Simpan Nilai</button>
					</div>
				</form>
			{/if}
		</section>
	</div>
{/if}

{#if showAssessmentModal && detail}
	<div class="modal-backdrop" role="presentation" onclick={() => showAssessmentModal = false}>
		<section
			class="modal-panel wide"
			role="dialog"
			aria-modal="true"
			tabindex="-1"
			onclick={(event) => event.stopPropagation()}
			onkeydown={(event) => event.stopPropagation()}
		>
			<div class="modal-head">
				<div>
					<span class="nav-label flush">Assessment</span>
					<h3>{assessmentForm.id ? 'Edit Assessment' : 'Tambah Assessment'}</h3>
				</div>
				<button class="btn btn-ghost icon-btn" type="button" onclick={() => showAssessmentModal = false}>
					<Icons name="x" size={18} />
				</button>
			</div>

			<form class="modal-form" onsubmit={(event) => { event.preventDefault(); void saveAssessment(); }}>
				<div class="form-grid">
					<label>
						<span>Jenis</span>
						<select
							bind:value={assessmentForm.type}
							onchange={() => {
								if (!isMeetingAssessmentType(assessmentForm.type)) assessmentForm.groupMeetingId = '';
								else if (!assessmentForm.groupMeetingId && meetingOptions[0]?.id) assessmentForm.groupMeetingId = String(meetingOptions[0].id);
							}}
						>
							{#each assessmentTypes as type}
								<option value={type.value}>{type.label}</option>
							{/each}
						</select>
					</label>

					<label>
						<span>Deadline</span>
						<input type="datetime-local" bind:value={assessmentForm.dueAt} />
					</label>
				</div>

				{#if isMeetingAssessmentType(assessmentForm.type)}
					<label>
						<span>Meeting</span>
						<select bind:value={assessmentForm.groupMeetingId} required>
							{#each meetingOptions as meeting}
								<option value={meeting.id}>M{meeting.meetingNumber} • {meeting.title}</option>
							{/each}
						</select>
					</label>

					{#if meetingOptions.length === 0}
						<div class="picker-empty">Meeting harus dimulai dulu sebelum membuat tugas atau quiz.</div>
					{/if}
				{/if}

				<label>
					<span>Judul</span>
					<input bind:value={assessmentForm.title} placeholder="Contoh: Quiz percabangan" required />
				</label>

				<label>
					<span>Deskripsi</span>
					<textarea bind:value={assessmentForm.description} rows="3" placeholder="Instruksi singkat, aturan pengerjaan, atau catatan untuk peserta."></textarea>
				</label>

				<label>
					<span>Lampiran</span>
					<input
						type="file"
						accept=".pdf,.doc,.docx,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
						onchange={(event) => assessmentFile = event.currentTarget.files?.[0] ?? null}
					/>
				</label>

				<div class="modal-actions">
					<button class="btn btn-ghost" type="button" onclick={() => showAssessmentModal = false}>Batal</button>
					<button
						class="btn btn-primary"
						type="submit"
						disabled={isMeetingAssessmentType(assessmentForm.type) && !assessmentForm.groupMeetingId}
					>
						{assessmentForm.id ? 'Simpan' : 'Tambah Assessment'}
					</button>
				</div>
			</form>
		</section>
	</div>
{/if}

{#if showEdit && detail}
	<div class="modal-backdrop" role="presentation" onclick={() => showEdit = false}>
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
					<span class="nav-label flush">Edit</span>
					<h3>{detail.group.name}</h3>
				</div>
				<button class="btn btn-ghost icon-btn" type="button" onclick={() => showEdit = false}>
					<Icons name="x" size={18} />
				</button>
			</div>

			<form class="modal-form" onsubmit={(event) => { event.preventDefault(); void updateGroup(); }}>
				<label>
					<span>Nama Group</span>
					<input bind:value={editForm.name}/>
				</label>
				<label>
					<span>Subject</span>
					<select bind:value={editForm.subjectId}>
						{#each subjects as subject}
							<option value={subject.id}>{subject.name}</option>
						{/each}
					</select>
				</label>
				<label>
					<span>Academic Year</span>
					<input bind:value={editForm.academicYear}/>
				</label>
				<label>
					<span>Status</span>
					<select bind:value={editForm.isDone}>
						<option value="false">On Going</option>
						<option value="true">Passed</option>
					</select>
				</label>

				<div class="modal-actions">
					<button class="btn btn-ghost" type="button" onclick={() => showEdit = false}>Batal</button>
					<button class="btn btn-primary" type="submit">Simpan</button>
				</div>
			</form>
		</section>
	</div>
{/if}

{#if showMemberModal && detail}
	<div class="modal-backdrop" role="presentation" onclick={() => showMemberModal = false}>
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
					<span class="nav-label flush">Member</span>
					<h3>Tambah Member</h3>
				</div>
				<button class="btn btn-ghost icon-btn" type="button" onclick={() => showMemberModal = false}>
					<Icons name="x" size={18} />
				</button>
			</div>

			<form class="modal-form" onsubmit={(event) => { event.preventDefault(); void addMember(); }}>
				<label>
					<span>Cari User</span>
					<div class="search-row">
						<input bind:value={memberForm.query} placeholder="Ketik nama user..." />
						<button class="btn btn-secondary" type="button" onclick={searchUsers}>
							<Icons name="search" size={16} />
							<span>Cari</span>
						</button>
					</div>
				</label>

				<div class="picker-list">
					{#if searchingUsers}
						<div class="picker-empty">Mencari user...</div>
					{:else if users.length === 0}
						<div class="picker-empty">Cari nama user untuk memilih member.</div>
					{:else}
						{#each users as user}
							<button
								class:selected={memberForm.userId === String(user.id)}
								class="picker-item"
								type="button"
								onclick={() => selectUser(user)}
							>
								<div class="avatar small">{user.name.slice(0, 1).toUpperCase()}</div>
								<div>
									<strong>{user.name}</strong>
									<p>{user.email} • Batch {user.batch}</p>
								</div>
							</button>
						{/each}
					{/if}
				</div>

				<label>
					<span>Role</span>
					<select bind:value={memberForm.role}>
						<option value="LEARNER">Learner</option>
						<option value="INSTRUCTOR">Instructor</option>
					</select>
				</label>

				<div class="modal-actions">
					<button class="btn btn-ghost" type="button" onclick={() => showMemberModal = false}>Batal</button>
					<button class="btn btn-primary" type="submit" disabled={!memberForm.userId}>Tambah Member</button>
				</div>
			</form>
		</section>
	</div>
{/if}

{#if showMemberEditModal && detail}
	<div class="modal-backdrop" role="presentation" onclick={() => showMemberEditModal = false}>
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
					<span class="nav-label flush">Member</span>
					<h3>Edit Member</h3>
				</div>
				<button class="btn btn-ghost icon-btn" type="button" onclick={() => showMemberEditModal = false}>
					<Icons name="x" size={18} />
				</button>
			</div>

			<form class="modal-form" onsubmit={(event) => { event.preventDefault(); void updateMember(); }}>
				<label>
					<span>User</span>
					<div class="search-row">
						<input bind:value={memberPatchForm.query} placeholder="Cari user pengganti..." />
						<button class="btn btn-secondary" type="button" onclick={searchPatchUsers}>
							<Icons name="search" size={16} />
							<span>Cari</span>
						</button>
					</div>
				</label>

				<div class="picker-list">
					{#if searchingUsers}
						<div class="picker-empty">Mencari user...</div>
					{:else if users.length === 0}
						<div class="picker-empty">Biarkan pilihan saat ini jika hanya ingin mengubah role.</div>
					{:else}
						{#each users as user}
							<button
								class:selected={memberPatchForm.userId === String(user.id)}
								class="picker-item"
								type="button"
								onclick={() => selectPatchUser(user)}
							>
								<div class="avatar small">{user.name.slice(0, 1).toUpperCase()}</div>
								<div>
									<strong>{user.name}</strong>
									<p>{user.email} • Batch {user.batch}</p>
								</div>
							</button>
						{/each}
					{/if}
				</div>

				<label>
					<span>Role</span>
					<select bind:value={memberPatchForm.role}>
						<option value="LEARNER">Learner</option>
						<option value="INSTRUCTOR">Instructor</option>
					</select>
				</label>

				<div class="modal-actions">
					<button class="btn btn-ghost" type="button" onclick={() => showMemberEditModal = false}>Batal</button>
					<button class="btn btn-primary" type="submit" disabled={!memberPatchForm.enrollmentId}>Simpan</button>
				</div>
			</form>
		</section>
	</div>
{/if}

{#if showScheduleModal && detail}
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
					<span class="nav-label flush">Schedule</span>
					<h3>Tambah Jadwal</h3>
				</div>
				<button class="btn btn-ghost icon-btn" type="button" onclick={() => showScheduleModal = false}>
					<Icons name="x" size={18} />
				</button>
			</div>

			<form class="modal-form" onsubmit={(event) => { event.preventDefault(); void addSchedule(); }}>
				<label>
					<span>Hari</span>
					<select bind:value={scheduleForm.dayOfWeek}>
						{#each days as day}
							<option value={day}>{dayLabel(day)}</option>
						{/each}
					</select>
				</label>

				<label>
					<span>Template Waktu</span>
					<select bind:value={scheduleForm.templateId} required>
						{#each templates as template}
							<option value={template.id}>{template.name} • {timeLabel(template.startTime, template.endTime)}</option>
						{/each}
					</select>
				</label>

				{#if templates.length === 0}
					<div class="picker-empty">Belum ada template jadwal. Buat template terlebih dahulu di menu jadwal.</div>
				{/if}

				<div class="modal-actions">
					<button class="btn btn-ghost" type="button" onclick={() => showScheduleModal = false}>Batal</button>
					<button class="btn btn-primary" type="submit" disabled={!scheduleForm.templateId}>Tambah Jadwal</button>
				</div>
			</form>
		</section>
	</div>
{/if}

<style>
	.back-row {
		margin-bottom: 1rem;
	}

	.detail-shell {
		display: grid;
		gap: 1.5rem;
	}

	.summary-card,
	.panel {
		background: var(--bg-surface);
		border: 1px solid var(--border);
		border-radius: var(--radius);
		box-shadow: var(--shadow-sm);
		min-width: 0;
	}

	.summary-card {
		padding: 1.5rem;
		display: grid;
		grid-template-columns: minmax(0, 1fr) auto;
		gap: 1.25rem;
		align-items: center;
	}

	.summary-meta {
		display: grid;
		grid-template-columns: repeat(6, minmax(90px, 1fr));
		gap: 0.75rem;
		min-width: 0;
	}

	.summary-meta div {
		border-left: 1px solid var(--border);
		padding-left: 1rem;
		display: grid;
		gap: 0.25rem;
	}

	.summary-meta span,
	label span {
		font-size: 0.75rem;
		font-weight: 800;
		color: var(--text-muted);
	}

	.summary-meta strong {
		font-size: 0.95rem;
	}

	.summary-actions {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		flex-wrap: wrap;
		align-self: center;
		justify-content: flex-end;
		min-width: 0;
	}

	.edit-button {
		align-self: center;
	}

	.detail-grid {
		display: grid;
		grid-template-columns: minmax(300px, 0.9fr) minmax(360px, 1.1fr);
		gap: 1.5rem;
		align-items: start;
	}

	.detail-tabs {
		display: grid;
		grid-template-columns: repeat(4, minmax(0, 1fr));
		gap: 0.75rem;
	}

	.detail-tabs button {
		border: 1px solid var(--border);
		background: var(--bg-surface);
		color: var(--text-main);
		border-radius: var(--radius-sm);
		padding: 0.8rem;
		display: grid;
		grid-template-columns: auto minmax(0, 1fr) auto;
		align-items: center;
		gap: 0.55rem;
		cursor: pointer;
		box-shadow: var(--shadow-sm);
		text-align: left;
		min-width: 0;
	}

	.detail-tabs button.active {
		border-color: var(--primary);
		background: var(--primary-soft);
		color: var(--primary);
	}

	.detail-tabs span {
		font-weight: 800;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.detail-tabs strong {
		font-size: 0.78rem;
		color: var(--text-muted);
	}

	.panel {
		padding: 1.25rem;
	}

	.panel-head {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 1rem;
		margin-bottom: 1rem;
	}

	.panel-head > div {
		display: grid;
		gap: 0.15rem;
	}

	.panel-head h3 {
		font-size: 1.05rem;
		letter-spacing: 0;
	}

	.panel-head span {
		color: var(--text-muted);
		font-size: 0.8rem;
		font-weight: 700;
	}

	.schedule-list,
	.member-list,
	.meeting-list,
	.assessment-list {
		display: grid;
		gap: 0.75rem;
	}

	.schedule-item,
	.member-item,
	.meeting-item,
	.assessment-item {
		border: 1px solid var(--border-light);
		border-radius: var(--radius-sm);
		padding: 0.875rem;
		display: flex;
		align-items: center;
		gap: 0.875rem;
		min-width: 0;
	}

	.meeting-item {
		align-items: flex-start;
		background: var(--bg-surface);
	}

	.assessment-item {
		align-items: flex-start;
	}

	.meeting-done {
		border-color: rgba(34, 197, 94, 0.28);
		background: rgba(34, 197, 94, 0.05);
	}

	.meeting-started {
		border-color: rgba(37, 99, 235, 0.28);
		background: rgba(37, 99, 235, 0.05);
	}

	.item-main {
		min-width: 0;
		flex: 1;
	}

	.item-main strong,
	.item-main p {
		overflow-wrap: anywhere;
	}

	.schedule-day {
		min-width: 92px;
		padding: 0.45rem 0.7rem;
		border-radius: var(--radius-sm);
		background: var(--primary-soft);
		color: var(--primary);
		font-size: 0.78rem;
		font-weight: 800;
		text-align: center;
	}

	.meeting-number {
		width: 46px;
		height: 46px;
		border-radius: var(--radius-sm);
		display: grid;
		place-items: center;
		background: var(--primary-soft);
		color: var(--primary);
		font-size: 0.78rem;
		font-weight: 900;
		flex: 0 0 auto;
	}

	.assessment-icon {
		width: 46px;
		height: 46px;
		border-radius: var(--radius-sm);
		display: grid;
		place-items: center;
		background: var(--primary-soft);
		color: var(--primary);
		flex: 0 0 auto;
	}

	.assessment-title-row {
		display: flex;
		align-items: center;
		flex-wrap: wrap;
		gap: 0.5rem;
	}

	.meeting-meta {
		display: flex;
		flex-wrap: wrap;
		gap: 0.4rem;
		margin-top: 0.5rem;
	}

	.meeting-meta span {
		border: 1px solid var(--border-light);
		border-radius: var(--radius-sm);
		padding: 0.25rem 0.45rem;
		color: var(--text-muted);
		font-size: 0.76rem;
		font-weight: 700;
	}

	.assessment-meta {
		display: flex;
		flex-wrap: wrap;
		gap: 0.4rem;
		margin-top: 0.5rem;
	}

	.assessment-meta span {
		border: 1px solid var(--border-light);
		border-radius: var(--radius-sm);
		padding: 0.25rem 0.45rem;
		color: var(--text-muted);
		font-size: 0.76rem;
		font-weight: 700;
	}

	.schedule-item p,
	.member-item p,
	.assessment-item p {
		font-size: 0.8rem;
		margin-top: 0.15rem;
	}

	.avatar {
		width: 38px;
		height: 38px;
		border-radius: 999px;
		display: grid;
		place-items: center;
		background: var(--primary-light);
		color: var(--primary);
		font-weight: 800;
		flex: 0 0 auto;
	}

	.member-item .badge {
		flex: 0 0 auto;
	}

	.item-actions {
		display: flex;
		align-items: center;
		gap: 0.25rem;
		flex: 0 0 auto;
	}

	.compact {
		padding: 0.5rem 0.75rem;
		font-size: 0.8rem;
	}

	.danger {
		color: var(--error);
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
		width: min(560px, 100%);
		background: var(--bg-surface);
		border: 1px solid var(--border);
		border-radius: var(--radius);
		box-shadow: var(--shadow-lg);
		padding: 1.5rem;
	}

	.modal-panel.wide {
		width: min(720px, 100%);
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

	.form-grid {
		display: grid;
		grid-template-columns: repeat(2, minmax(0, 1fr));
		gap: 1rem;
	}

	.search-row {
		display: grid;
		grid-template-columns: 1fr auto;
		gap: 0.75rem;
	}

	.picker-list {
		border: 1px solid var(--border-light);
		border-radius: var(--radius-sm);
		display: grid;
		overflow: hidden;
	}

	.picker-empty {
		padding: 0.875rem;
		color: var(--text-muted);
		font-size: 0.85rem;
		background: var(--bg-app);
	}

	.form-note {
		border: 1px solid var(--border-light);
		border-radius: var(--radius-sm);
		background: var(--bg-app);
		padding: 0.875rem;
		display: grid;
		gap: 0.25rem;
	}

	.form-note p {
		font-size: 0.85rem;
		color: var(--text-muted);
	}

	.picker-item {
		border: 0;
		border-bottom: 1px solid var(--border-light);
		background: var(--bg-surface);
		display: flex;
		align-items: center;
		gap: 0.75rem;
		padding: 0.75rem;
		text-align: left;
		cursor: pointer;
		color: var(--text-main);
	}

	.picker-item:last-child {
		border-bottom: 0;
	}

	.picker-item:hover,
	.picker-item.selected {
		background: var(--primary-soft);
	}

	.picker-item p {
		font-size: 0.78rem;
		margin-top: 0.1rem;
	}

	.small {
		width: 32px;
		height: 32px;
		font-size: 0.8rem;
	}

	label {
		display: grid;
		gap: 0.375rem;
	}

	.icon-btn {
		width: 40px;
		height: 40px;
		padding: 0;
	}

	.flush {
		padding: 0;
		margin: 0 0 0.25rem;
	}

	.grade-modal {
		width: min(980px, 100%);
		max-height: min(88vh, 820px);
		overflow: auto;
	}

	.grade-summary {
		display: grid;
		grid-template-columns: repeat(3, minmax(0, 1fr));
		gap: 0.75rem;
		margin-top: 1.25rem;
	}

	.grade-summary div {
		border: 1px solid var(--border-light);
		border-radius: var(--radius-sm);
		background: var(--bg-app);
		padding: 0.875rem;
		display: grid;
		gap: 0.2rem;
	}

	.grade-summary span {
		color: var(--text-muted);
		font-size: 0.75rem;
		font-weight: 800;
	}

	.grade-summary strong {
		font-size: 1.15rem;
	}

	.gradebook {
		display: grid;
		gap: 0.5rem;
		margin-top: 1.25rem;
	}

	.grade-row {
		display: grid;
		grid-template-columns: minmax(220px, 1.3fr) minmax(96px, 0.45fr) minmax(180px, 1fr) minmax(88px, auto);
		gap: 0.75rem;
		align-items: center;
		border: 1px solid var(--border-light);
		border-radius: var(--radius-sm);
		padding: 0.75rem;
		background: var(--bg-surface);
	}

	.grade-head {
		background: var(--bg-app);
		color: var(--text-muted);
		font-size: 0.75rem;
		font-weight: 800;
	}

	.grade-user {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		min-width: 0;
	}

	.grade-user strong,
	.grade-user p {
		overflow-wrap: anywhere;
	}

	.grade-user p {
		color: var(--text-muted);
		font-size: 0.8rem;
		margin-top: 0.1rem;
	}

	.grade-score,
	.grade-feedback {
		width: 100%;
		min-width: 0;
	}

	@media (max-width: 1100px) {
		.summary-card,
		.detail-grid {
			grid-template-columns: 1fr;
		}

		.summary-meta {
			grid-template-columns: repeat(3, minmax(120px, 1fr));
		}

		.summary-actions {
			justify-content: flex-start;
		}
	}

	@media (max-width: 560px) {
		.summary-meta {
			grid-template-columns: 1fr;
		}

		.detail-tabs {
			grid-template-columns: repeat(2, minmax(0, 1fr));
		}

		.grade-summary,
		.grade-row {
			grid-template-columns: 1fr;
		}

		.grade-head {
			display: none;
		}

		.member-item,
		.schedule-item,
		.meeting-item,
		.assessment-item {
			align-items: flex-start;
			flex-wrap: wrap;
		}

		.item-actions {
			width: 100%;
			justify-content: flex-end;
		}

		.schedule-day {
			min-width: 0;
		}

		.search-row {
			grid-template-columns: 1fr;
		}

		.form-grid {
			grid-template-columns: 1fr;
		}
	}
</style>
