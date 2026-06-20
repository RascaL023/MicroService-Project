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
</script>

<svelte:head><title>Laporan - Divdik Course</title></svelte:head>

<AccessPanel authorities={['subject.read', 'subject.*']}>
	<PageTitle
		eyebrow="Reports"
		title="Laporan Mingguan"
		description="Export nilai learner berdasarkan tahun akademik dan subject."
	/>

	<Notice {error} {success} />

	<section class="report-card">
		<div class="report-head">
			<div class="report-icon"><Icons name="download" size={22} /></div>
			<div>
				<h2>Nilai Mingguan Learner</h2>
				<p>Workbook berisi sheet Tugas, Quiz, UTS, dan UAS dengan format tabel nilai per peserta.</p>
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
	</section>
</AccessPanel>

<style>
	.report-card {
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
		font-size: 0.78rem;
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

	.report-form span {
		color: var(--text-muted);
		font-size: 0.75rem;
		font-weight: 700;
	}

	@media (max-width: 820px) {
		.report-form {
			grid-template-columns: 1fr;
		}
	}
</style>
