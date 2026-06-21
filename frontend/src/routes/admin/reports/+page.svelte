<script lang="ts">
	import { onMount } from 'svelte';
	import AccessPanel from '$lib/components/AccessPanel.svelte';
	import Icons from '$lib/components/Icons.svelte';
	import Notice from '$lib/components/Notice.svelte';
	import PageTitle from '$lib/components/PageTitle.svelte';
	import { api, pageItems, readSession } from '$lib/api';
	import type { PageData, Subject } from '$lib/types';

	let academicYear = $state('');
	let subjectId = $state('');
	let subjects = $state<Subject[]>([]);
	let subjectsLoading = $state(false);
	let loading = $state(false);
	let error = $state('');
	let success = $state('');

	onMount(() => {
		const today = new Date();
		academicYear = defaultAcademicYear(today);
		void loadSubjects();
	});

	async function loadSubjects() {
		subjectsLoading = true;
		error = '';
		try {
			const query = new URLSearchParams({
				page: '0',
				size: '200',
				sort: 'name,asc'
			});
			const payload = await api<PageData<Subject> | Subject[]>(`/api/subjects?${query}`);
			subjects = pageItems(payload);
		} catch (err) {
			error = err instanceof Error ? err.message : 'Gagal memuat subject.';
		} finally {
			subjectsLoading = false;
		}
	}

	async function downloadWeeklyGrades() {
		error = '';
		success = '';

		if (!academicYear.trim()) {
			error = 'Tahun akademik wajib diisi.';
			return;
		}
		if (!subjectId) {
			error = 'Subject wajib dipilih.';
			return;
		}

		loading = true;
		try {
			const query = new URLSearchParams({
				academicYear: academicYear.trim(),
				subjectId
			});

			const session = readSession();
			const headers = new Headers({ Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
			if (session?.sessionId) headers.set('Authorization', `Session ${session.sessionId}`);

			const response = await fetch(`/api/reports/weekly-grades?${query}`, { headers });
			if (!response.ok) throw new Error(await readError(response));

			const blob = await response.blob();
			const subjectName = subjects.find((subject) => String(subject.id) === subjectId)?.name ?? subjectId;
			const filename = filenameFromDisposition(response.headers.get('content-disposition')) ??
				`weekly-grades-${safeFilename(academicYear.trim())}-${safeFilename(subjectName)}.xlsx`;
			const url = URL.createObjectURL(blob);
			const link = document.createElement('a');
			link.href = url;
			link.download = filename;
			document.body.appendChild(link);
			link.click();
			link.remove();
			URL.revokeObjectURL(url);
			success = 'Laporan berhasil dibuat.';
		} catch (err) {
			error = err instanceof Error ? err.message : 'Gagal membuat laporan.';
		} finally {
			loading = false;
		}
	}

	async function readError(response: Response) {
		const payload = await response.json().catch(() => null) as { message?: string; errorCode?: string } | null;
		return payload?.message || payload?.errorCode || 'Gagal membuat laporan.';
	}

	function filenameFromDisposition(value: string | null) {
		if (!value) return null;
		const match = value.match(/filename="?([^"]+)"?/i);
		return match?.[1] ?? null;
	}

	function defaultAcademicYear(date: Date) {
		const year = date.getFullYear();
		const startYear = date.getMonth() >= 6 ? year : year - 1;
		return `${startYear}/${startYear + 1}`;
	}

	function safeFilename(value: string) {
		return value.replace(/[^a-zA-Z0-9._-]+/g, '-');
	}

	function selectedSubjectName() {
		return subjects.find((subject) => String(subject.id) === subjectId)?.name ?? 'Pilih subject';
	}

	function previewFilename() {
		if (!academicYear.trim() || !subjectId) return 'weekly-grades-{periode}-{subject}.xlsx';
		return `weekly-grades-${safeFilename(academicYear.trim())}-${safeFilename(selectedSubjectName())}.xlsx`;
	}
</script>

<svelte:head><title>Laporan - Divdik Course</title></svelte:head>

<AccessPanel authorities={['subject.read', 'subject.*']}>
	<PageTitle
		eyebrow="Reports"
		title="Laporan Mingguan"
		description="Export nilai learner berdasarkan tahun akademik dan subject."
	/>

	<Notice {error} {success} />

	<section class="report-workspace">
		<div class="report-main">
			<div class="report-head">
				<div class="report-icon"><Icons name="file" size={22} /></div>
				<div>
					<h2>Nilai Mingguan Learner</h2>
					<p>Export workbook nilai lintas group berdasarkan subject dan tahun akademik.</p>
				</div>
			</div>

			<form class="report-form" onsubmit={(event) => { event.preventDefault(); void downloadWeeklyGrades(); }}>
				<label>
					<span>Tahun Akademik</span>
					<input bind:value={academicYear} placeholder="2026/2027" required />
				</label>
				<label>
					<span>Subject</span>
					<select bind:value={subjectId} disabled={subjectsLoading} required>
						<option value="">{subjectsLoading ? 'Memuat subject...' : 'Pilih subject'}</option>
						{#each subjects as subject}
							<option value={String(subject.id)}>{subject.name}</option>
						{/each}
					</select>
				</label>
				<button class="btn btn-primary filter-submit" type="submit" disabled={loading}>
					<Icons name="download" size={16} />
					<span>{loading ? 'Membuat...' : 'Download Excel'}</span>
				</button>
			</form>

			<div class="report-meta">
				<div>
					<span>Output</span>
					<strong>{previewFilename()}</strong>
				</div>
				<div>
					<span>Sheet</span>
					<strong>Tugas, Quiz, UTS, UAS</strong>
				</div>
				<div>
					<span>Kolom</span>
					<strong>No, Nama, Nilai, Rata-rata, Grade</strong>
				</div>
			</div>
		</div>

		<aside class="report-preview" aria-label="Preview laporan">
			<div class="preview-top">
				<Icons name="book" size={18} />
				<span>Preview Workbook</span>
			</div>
			<div class="sheet-tabs">
				<span>Tugas</span>
				<span>Quiz</span>
				<span>UTS</span>
				<span>UAS</span>
			</div>
			<div class="mini-sheet">
				<div class="mini-title">Laporan Nilai Divisi Pendidikan</div>
				<div class="mini-row"><span>Periode</span><strong>{academicYear || '-'}</strong></div>
				<div class="mini-row"><span>Subject</span><strong>{selectedSubjectName()}</strong></div>
				<div class="mini-table">
					<span>No</span><span>Nama Peserta</span><span>Tugas 1</span><span>Rata</span><span>Grade</span>
					<span>1</span><span>Muhammad Asep</span><span>98</span><span>89.33</span><span>A</span>
				</div>
			</div>
		</aside>
	</section>
</AccessPanel>

<style>
	.report-workspace {
		display: grid;
		grid-template-columns: minmax(0, 1.35fr) minmax(280px, 0.65fr);
		gap: 1rem;
		align-items: stretch;
	}

	.report-main,
	.report-preview {
		border: 1px solid var(--border);
		background: var(--bg-surface);
		border-radius: var(--radius);
		padding: 1.1rem;
	}

	.report-head {
		display: flex;
		align-items: center;
		gap: 0.8rem;
		margin-bottom: 1rem;
	}

	.report-icon {
		display: grid;
		place-items: center;
		width: 42px;
		height: 42px;
		border-radius: var(--radius-sm);
		background: var(--primary-soft);
		color: var(--primary);
	}

	.report-head h2 {
		margin: 0;
		font-size: 1rem;
	}

	.report-head p {
		margin: 0.2rem 0 0;
		color: var(--text-muted);
		font-size: 0.82rem;
	}

	.report-form {
		display: grid;
		grid-template-columns: minmax(180px, 1fr) minmax(150px, 0.7fr) auto;
		align-items: end;
		gap: 0.75rem;
	}

	.report-form label {
		display: grid;
		gap: 0.35rem;
	}

	.report-form label > span {
		color: var(--text-muted);
		font-size: 0.75rem;
		font-weight: 700;
	}

	.report-meta {
		display: grid;
		grid-template-columns: repeat(3, minmax(0, 1fr));
		gap: 0.7rem;
		margin-top: 1rem;
	}

	.report-meta div {
		border: 1px solid var(--border);
		border-radius: var(--radius-sm);
		padding: 0.75rem;
		background: color-mix(in srgb, var(--bg-surface) 88%, var(--primary) 12%);
		min-width: 0;
	}

	.report-meta span,
	.preview-top span {
		display: block;
		color: var(--text-muted);
		font-size: 0.72rem;
		font-weight: 800;
		text-transform: uppercase;
		letter-spacing: 0.04em;
	}

	.report-meta strong {
		display: block;
		margin-top: 0.3rem;
		font-size: 0.86rem;
		overflow-wrap: anywhere;
	}

	.report-preview {
		display: grid;
		gap: 0.9rem;
		align-content: start;
	}

	.preview-top {
		display: flex;
		align-items: center;
		gap: 0.5rem;
	}

	.sheet-tabs {
		display: grid;
		grid-template-columns: repeat(4, 1fr);
		border: 1px solid var(--border);
		border-radius: var(--radius-sm);
		overflow: hidden;
	}

	.sheet-tabs span {
		padding: 0.55rem 0.4rem;
		text-align: center;
		font-size: 0.74rem;
		font-weight: 800;
		border-right: 1px solid var(--border);
	}

	.sheet-tabs span:first-child {
		background: var(--primary-soft);
		color: var(--primary);
	}

	.sheet-tabs span:last-child {
		border-right: 0;
	}

	.mini-sheet {
		border: 1px solid var(--border);
		background: var(--bg-main);
		border-radius: var(--radius-sm);
		overflow: hidden;
		font-size: 0.76rem;
	}

	.mini-title {
		padding: 0.65rem;
		font-weight: 900;
		border-bottom: 1px solid var(--border);
	}

	.mini-row {
		display: grid;
		grid-template-columns: 90px 1fr;
		border-bottom: 1px solid var(--border);
	}

	.mini-row span,
	.mini-row strong {
		padding: 0.45rem 0.6rem;
	}

	.mini-row span {
		font-weight: 800;
		border-right: 1px solid var(--border);
	}

	.mini-table {
		display: grid;
		grid-template-columns: 42px minmax(92px, 1fr) 70px 66px 54px;
		overflow-x: auto;
	}

	.mini-table span {
		padding: 0.48rem;
		border-right: 1px solid var(--border);
		border-bottom: 1px solid var(--border);
		white-space: nowrap;
	}

	.mini-table span:nth-child(-n + 5) {
		background: var(--primary);
		color: white;
		font-weight: 800;
	}

	@media (max-width: 820px) {
		.report-workspace,
		.report-form {
			grid-template-columns: 1fr;
		}

		.report-meta {
			grid-template-columns: 1fr;
		}
	}
</style>
