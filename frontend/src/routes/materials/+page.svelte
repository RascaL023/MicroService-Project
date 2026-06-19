<script lang="ts">
	import { onMount } from 'svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import AccessPanel from '$lib/components/AccessPanel.svelte';
	import ConfirmModal from '$lib/components/ConfirmModal.svelte';
	import Notice from '$lib/components/Notice.svelte';
	import PageTitle from '$lib/components/PageTitle.svelte';
	import Icons from '$lib/components/Icons.svelte';
	import Pagination from '$lib/components/Pagination.svelte';
	import { api, hasAnyAuthority, pageItems, paginationMeta, readSession } from '$lib/api';
	import type { LoginData, PageData, PaginationMeta, Subject, SubjectMaterial, SubjectModule } from '$lib/types';

	const allowedExtensions = ['pdf', 'ppt', 'pptx', 'doc', 'docx'];
	const maxFileSize = 5 * 1024 * 1024;

	let activeTab = $state<'materials' | 'modules'>('materials');
	let subjects = $state<Subject[]>([]);
	let materials = $state<SubjectMaterial[]>([]);
	let modules = $state<SubjectModule[]>([]);
	let session = $state<LoginData | null>(null);
	let materialPage = $state<PaginationMeta>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
	let modulePage = $state<PaginationMeta>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
	let filters = $state({ subjectId: '', title: '', filename: '' });
	let materialForm = $state({ subjectId: '', meetingNumber: '1', title: '', description: '' });
	let materialPatchForm = $state({ id: '', subjectId: '', meetingNumber: '', title: '', description: '' });
	let uploadSubjectId = $state('');
	let selectedFile = $state<File | null>(null);
	let showMaterialEdit = $state(false);
	let showModuleUpload = $state(false);
	let confirmState = $state({ open: false, title: '', message: '', confirmLabel: 'Ya, lanjutkan' });
	let pendingConfirm: (() => Promise<void>) | null = null;
	let busy = $state<'materials' | 'modules' | 'download' | ''>('');
	let error = $state('');
	let success = $state('');

	const canCreateMaterial = $derived(hasAnyAuthority(session, ['subject.*', 'subject-material.*', 'subject-material.create']));
	const canUpdateMaterial = $derived(hasAnyAuthority(session, ['subject.*', 'subject-material.*', 'subject-material.update']));
	const canDeleteMaterial = $derived(hasAnyAuthority(session, ['subject.*', 'subject-material.*', 'subject-material.delete']));
	const canCreateModule = $derived(hasAnyAuthority(session, ['subject.*', 'subject-module.*', 'subject-module.create']));
	const canDeleteModule = $derived(hasAnyAuthority(session, ['subject.*', 'subject-module.*', 'subject-module.delete']));
	const selectedSubjectName = $derived(subjects.find((subject) => String(subject.id) === filters.subjectId)?.name ?? 'Semua subjek');
	const selectedFileValid = $derived(selectedFile ? validateSelectedFile(selectedFile) : '');

	onMount(() => {
		session = readSession();
		void loadAll();
	});

	async function loadAll() {
		try {
			const payload = await api<PageData<Subject> | Subject[]>('/api/subjects?sort=name,asc');
			subjects = pageItems(payload);
			if (!filters.subjectId && subjects[0]) {
				filters.subjectId = String(subjects[0].id);
				materialForm.subjectId = String(subjects[0].id);
				uploadSubjectId = String(subjects[0].id);
			}
			await Promise.all([loadMaterials(), loadModules()]);
		} catch (err) {
			error = err instanceof Error ? err.message : 'Gagal memuat data materi.';
		}
	}

	async function applySubjectFilter(subjectId: string) {
		filters.subjectId = subjectId;
		materialForm.subjectId = subjectId || materialForm.subjectId;
		uploadSubjectId = subjectId || uploadSubjectId;
		materialPage = { ...materialPage, page: 0 };
		modulePage = { ...modulePage, page: 0 };
		await Promise.all([loadMaterials(), loadModules()]);
	}

	async function loadMaterials() {
		const query = new URLSearchParams({
			page: String(materialPage.page),
			size: String(materialPage.size),
			sort: 'meetingNumber,asc'
		});
		if (filters.subjectId) query.set('subjectId', filters.subjectId);
		if (filters.title.trim()) query.set('title', filters.title.trim());

		const payload = await api<PageData<SubjectMaterial> | SubjectMaterial[]>(`/api/subject-materials?${query}`);
		materials = pageItems(payload);
		materialPage = paginationMeta(payload, materialPage);
	}

	async function loadModules() {
		const query = new URLSearchParams({
			page: String(modulePage.page),
			size: String(modulePage.size),
			sort: 'id,desc'
		});
		if (filters.subjectId) query.set('subjectId', filters.subjectId);
		if (filters.filename.trim()) query.set('filename', filters.filename.trim());

		const payload = await api<PageData<SubjectModule> | SubjectModule[]>(`/api/subject-modules?${query}`);
		modules = pageItems(payload);
		modulePage = paginationMeta(payload, modulePage);
	}

	async function createMaterial() {
		busy = 'materials';
		error = '';
		success = '';
		try {
			await api<SubjectMaterial>('/api/subject-materials', {
				method: 'POST',
				body: JSON.stringify({
					subjectId: Number(materialForm.subjectId),
					meetingNumber: Number(materialForm.meetingNumber),
					title: materialForm.title,
					description: materialForm.description || null
				})
			});
			success = 'Rencana pertemuan berhasil ditambahkan.';
			materialForm = {
				subjectId: materialForm.subjectId,
				meetingNumber: String(Number(materialForm.meetingNumber) + 1),
				title: '',
				description: ''
			};
			await loadMaterials();
		} catch (err) {
			error = err instanceof Error ? err.message : 'Gagal membuat rencana pertemuan.';
		} finally {
			busy = '';
		}
	}

	async function deleteMaterial(id: number) {
		askConfirm({
			title: 'Hapus rencana pertemuan?',
			message: 'Rencana pertemuan ini akan dihapus dari materi subjek.',
			confirmLabel: 'Hapus Rencana'
		}, async () => {
			error = '';
			success = '';
			try {
				await api<null>(`/api/subject-materials/${id}`, { method: 'DELETE' });
				success = 'Rencana pertemuan dihapus.';
				await loadMaterials();
			} catch (err) {
				error = err instanceof Error ? err.message : 'Gagal menghapus rencana pertemuan.';
			}
		});
	}

	function openMaterialEdit(item: SubjectMaterial) {
		materialPatchForm = {
			id: String(item.id),
			subjectId: String(item.subjectId),
			meetingNumber: String(item.meetingNumber),
			title: item.title,
			description: item.description ?? ''
		};
		showMaterialEdit = true;
	}

	async function updateMaterial() {
		busy = 'materials';
		error = '';
		success = '';
		try {
			await api<SubjectMaterial>(`/api/subject-materials/${materialPatchForm.id}`, {
				method: 'PATCH',
				body: JSON.stringify({
					subjectId: Number(materialPatchForm.subjectId),
					meetingNumber: Number(materialPatchForm.meetingNumber),
					title: materialPatchForm.title,
					description: materialPatchForm.description || null
				})
			});
			showMaterialEdit = false;
			success = 'Rencana pertemuan berhasil diupdate.';
			await loadMaterials();
		} catch (err) {
			error = err instanceof Error ? err.message : 'Gagal mengupdate rencana pertemuan.';
		} finally {
			busy = '';
		}
	}

	function handleFileChange(event: Event) {
		const input = event.currentTarget as HTMLInputElement;
		selectedFile = input.files?.[0] ?? null;
		error = selectedFile ? validateSelectedFile(selectedFile) : '';
	}

	async function uploadModule() {
		error = '';
		success = '';
		if (!selectedFile) {
			error = 'Pilih file modul dulu.';
			return;
		}
		const validation = validateSelectedFile(selectedFile);
		if (validation) {
			error = validation;
			return;
		}

		busy = 'modules';
		try {
			const formData = new FormData();
			formData.set('subjectId', uploadSubjectId);
			formData.set('file', selectedFile);
			await api<SubjectModule>('/api/subject-modules', { method: 'POST', body: formData });
			success = 'File modul berhasil diupload.';
			closeModuleUpload();
			await loadModules();
		} catch (err) {
			error = err instanceof Error ? err.message : 'Gagal upload file modul.';
		} finally {
			busy = '';
		}
	}

	async function deleteModule(id: number) {
		askConfirm({
			title: 'Hapus file modul?',
			message: 'File modul dan file fisiknya akan dihapus.',
			confirmLabel: 'Hapus File'
		}, async () => {
			error = '';
			success = '';
			try {
				await api<null>(`/api/subject-modules/${id}`, { method: 'DELETE' });
				success = 'File modul dihapus.';
				await loadModules();
			} catch (err) {
				error = err instanceof Error ? err.message : 'Gagal menghapus file modul.';
			}
		});
	}

	function askConfirm(config: { title: string; message: string; confirmLabel?: string }, action: () => Promise<void>) {
		confirmState = { open: true, title: config.title, message: config.message, confirmLabel: config.confirmLabel ?? 'Ya, lanjutkan' };
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

	async function downloadModule(module: SubjectModule) {
		const session = readSession();
		if (!session?.sessionId) {
			error = 'Session tidak ditemukan. Silakan login ulang.';
			return;
		}

		busy = 'download';
		error = '';
		success = '';
		try {
			const response = await fetch(`/api/subject-modules/${module.id}/download`, {
				headers: { Authorization: `Session ${session.sessionId}` }
			});
			if (!response.ok) throw new Error('Download gagal.');
			const blob = await response.blob();
			const url = URL.createObjectURL(blob);
			const anchor = document.createElement('a');
			anchor.href = url;
			anchor.download = module.originalFilename;
			document.body.appendChild(anchor);
			anchor.click();
			anchor.remove();
			URL.revokeObjectURL(url);
		} catch (err) {
			error = err instanceof Error ? err.message : 'Download gagal.';
		} finally {
			busy = '';
		}
	}

	function validateSelectedFile(file: File) {
		const extension = file.name.split('.').pop()?.toLowerCase() ?? '';
		if (!allowedExtensions.includes(extension)) return 'Format file harus PDF, PowerPoint, atau Microsoft Word.';
		if (file.size > maxFileSize) return 'Ukuran file maksimal 5MB.';
		return '';
	}

	function clearFileInput() {
		const input = document.getElementById('module-file') as HTMLInputElement | null;
		if (input) input.value = '';
	}

	function closeModuleUpload() {
		selectedFile = null;
		clearFileInput();
		showModuleUpload = false;
	}

	function fileSizeLabel(size: number) {
		if (size < 1024 * 1024) return `${Math.max(1, Math.round(size / 1024))} KB`;
		return `${(size / (1024 * 1024)).toFixed(1)} MB`;
	}

	function fileTypeLabel(mimeType: string, filename: string) {
		const extension = filename.split('.').pop()?.toUpperCase();
		if (extension) return extension;
		if (mimeType.includes('pdf')) return 'PDF';
		if (mimeType.includes('word')) return 'DOC';
		if (mimeType.includes('presentation') || mimeType.includes('powerpoint')) return 'PPT';
		return 'FILE';
	}

	function applyMaterialFilter() {
		materialPage = { ...materialPage, page: 0 };
		void loadMaterials();
	}

	function applyModuleFilter() {
		modulePage = { ...modulePage, page: 0 };
		void loadModules();
	}

	function changeMaterialPage(page: number) {
		materialPage = { ...materialPage, page };
		void loadMaterials();
	}

	function changeMaterialSize(size: number) {
		materialPage = { ...materialPage, page: 0, size };
		void loadMaterials();
	}

	function changeModulePage(page: number) {
		modulePage = { ...modulePage, page };
		void loadModules();
	}

	function changeModuleSize(size: number) {
		modulePage = { ...modulePage, page: 0, size };
		void loadModules();
	}
</script>

<svelte:head><title>Course Materials - Divdik Course</title></svelte:head>

<PageTitle eyebrow="Academic Resources" title="Materi Kursus" description="Kelola rencana pertemuan dan file modul per subjek agar alur belajar tiap kelompok tetap konsisten." />

<Notice {error} {success} />

<AccessPanel authorities={['subject.read', 'subject-material.read', 'subject-material.*', 'subject-module.read', 'subject-module.*']}>
	<section class="materials-shell">
		<div class="toolbar card">
			<div>
				<label for="subject-filter">Subjek Aktif</label>
				<select id="subject-filter" value={filters.subjectId} onchange={(event) => void applySubjectFilter((event.currentTarget as HTMLSelectElement).value)}>
					<option value="">Semua Subjek</option>
					{#each subjects as subject}
						<option value={subject.id}>{subject.name}</option>
					{/each}
				</select>
			</div>
			<div class="toolbar-summary">
				<span class="summary-label">Sedang melihat</span>
				<strong>{selectedSubjectName}</strong>
				<small>{materialPage.totalElements} rencana pertemuan • {modulePage.totalElements} file modul</small>
			</div>
		</div>

		<div class="tabs" role="tablist" aria-label="Jenis materi">
			<button class:active={activeTab === 'materials'} type="button" onclick={() => (activeTab = 'materials')}>
				Rencana Pertemuan
			</button>
			<button class:active={activeTab === 'modules'} type="button" onclick={() => (activeTab = 'modules')}>
				File Modul
			</button>
		</div>

		{#if activeTab === 'materials'}
			<div class="content-grid">
				<section>
					<div class="card compact-filter">
						<form class="flex gap-4" onsubmit={(event) => { event.preventDefault(); applyMaterialFilter(); }}>
							<input bind:value={filters.title} placeholder="Cari judul rencana pertemuan..." />
							<button class="btn btn-primary" type="submit">
								<Icons name="search" size={16} />
								Filter
							</button>
						</form>
					</div>

					<div class="timeline-list">
						{#each materials as item}
							<article class="material-item">
								<div class="meeting-number">{item.meetingNumber}</div>
								<div class="material-body">
									<div class="flex justify-between gap-4">
										<div>
											<h3>{item.title}</h3>
											<p>{item.subjectName || `Subjek #${item.subjectId}`}</p>
										</div>
										{#if canUpdateMaterial || canDeleteMaterial}
											<div class="item-actions">
												{#if canUpdateMaterial}
													<button class="btn btn-ghost btn-icon" aria-label="Edit rencana pertemuan" type="button" onclick={() => openMaterialEdit(item)}>
														<Icons name="edit" size={16} />
													</button>
												{/if}
												{#if canDeleteMaterial}
													<button class="btn btn-ghost btn-icon danger" aria-label="Hapus rencana pertemuan" type="button" onclick={() => deleteMaterial(item.id)}>
														<Icons name="x" size={16} />
													</button>
												{/if}
											</div>
										{/if}
									</div>
									{#if item.description}
										<div class="description-box">{item.description}</div>
									{/if}
								</div>
							</article>
						{:else}
							<EmptyState text="Belum ada rencana pertemuan untuk filter ini." />
						{/each}
					</div>
					<Pagination meta={materialPage} onPage={changeMaterialPage} onSize={changeMaterialSize} />
				</section>

				{#if canCreateMaterial}
				<aside class="card sticky-card">
					<h3>Tambah Rencana</h3>
					<p>Rencana pertemuan adalah acuan materi untuk semua grup dengan subjek yang sama.</p>
					<form class="form-stack" onsubmit={(event) => { event.preventDefault(); void createMaterial(); }}>
						<div class="form-group">
							<label for="material-subject">Subjek</label>
							<select id="material-subject" bind:value={materialForm.subjectId} required>
								<option value="">Pilih Subjek</option>
								{#each subjects as subject}
									<option value={subject.id}>{subject.name}</option>
								{/each}
							</select>
						</div>
						<div class="form-row">
							<div class="form-group">
								<label for="meeting-number">Pertemuan</label>
								<input id="meeting-number" bind:value={materialForm.meetingNumber} min="1" type="number" required />
							</div>
							<div class="form-group">
								<label for="material-title">Judul</label>
								<input id="material-title" bind:value={materialForm.title} placeholder="Contoh: Pengenalan OOP" required />
							</div>
						</div>
						<div class="form-group">
							<label for="material-description">Deskripsi</label>
							<textarea id="material-description" bind:value={materialForm.description} rows="4" placeholder="Ringkasan pembahasan pertemuan ini."></textarea>
						</div>
						<button class="btn btn-primary w-full" disabled={busy === 'materials'} type="submit">
							{busy === 'materials' ? 'Menyimpan...' : 'Simpan Rencana'}
						</button>
					</form>
				</aside>
				{/if}
			</div>
		{:else}
			{#if canCreateModule}
				<section class="page-actions module-page-actions" aria-label="Aksi file modul">
					<button class="btn btn-primary" type="button" onclick={() => showModuleUpload = true}>
						<Icons name="upload" size={17} />
						<span>Upload File Modul</span>
					</button>
				</section>
			{/if}

			<div class="content-grid">
				<section>
					<div class="card compact-filter">
						<form class="flex gap-4" onsubmit={(event) => { event.preventDefault(); applyModuleFilter(); }}>
							<input bind:value={filters.filename} placeholder="Cari nama file modul..." />
							<button class="btn btn-primary" type="submit">
								<Icons name="search" size={16} />
								Filter
							</button>
						</form>
					</div>

					<div class="module-list">
						{#each modules as module}
							<article class="module-card">
								<div class="file-mark">
									<Icons name="file" size={22} />
									<span>{fileTypeLabel(module.mimeType, module.originalFilename)}</span>
								</div>
								<div class="module-main">
									<h3>{module.originalFilename}</h3>
									<p>{module.subjectName || `Subjek #${module.subjectId}`}</p>
									<div class="module-meta">
										<span>{fileSizeLabel(module.fileSize)}</span>
										<span>{module.mimeType}</span>
									</div>
								</div>
								<div class="module-actions">
									<button class="btn btn-secondary" disabled={busy === 'download'} type="button" onclick={() => downloadModule(module)}>
										<Icons name="download" size={16} />
										Download
									</button>
									{#if canDeleteModule}
										<button class="btn btn-ghost btn-icon danger" aria-label="Hapus file modul" type="button" onclick={() => deleteModule(module.id)}>
											<Icons name="x" size={16} />
										</button>
									{/if}
								</div>
							</article>
						{:else}
							<EmptyState text="Belum ada file modul untuk filter ini." />
						{/each}
					</div>
					<Pagination meta={modulePage} onPage={changeModulePage} onSize={changeModuleSize} />
				</section>
			</div>
		{/if}
	</section>
</AccessPanel>

{#if showMaterialEdit && canUpdateMaterial}
	<div class="modal-backdrop" role="presentation" onclick={() => showMaterialEdit = false}>
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
					<span class="summary-label">Rencana Pertemuan</span>
					<h3>Edit Materi Pertemuan</h3>
				</div>
				<button class="btn btn-ghost btn-icon" type="button" onclick={() => showMaterialEdit = false}>
					<Icons name="x" size={18} />
				</button>
			</div>

			<form class="form-stack modal-form" onsubmit={(event) => { event.preventDefault(); void updateMaterial(); }}>
				<div class="form-group">
					<label for="patch-material-subject">Subjek</label>
					<select id="patch-material-subject" bind:value={materialPatchForm.subjectId} required>
						{#each subjects as subject}
							<option value={subject.id}>{subject.name}</option>
						{/each}
					</select>
				</div>
				<div class="form-row">
					<div class="form-group">
						<label for="patch-meeting-number">Pertemuan</label>
						<input id="patch-meeting-number" bind:value={materialPatchForm.meetingNumber} min="1" type="number" required />
					</div>
					<div class="form-group">
						<label for="patch-material-title">Judul</label>
						<input id="patch-material-title" bind:value={materialPatchForm.title} required />
					</div>
				</div>
				<div class="form-group">
					<label for="patch-material-description">Deskripsi</label>
					<textarea id="patch-material-description" bind:value={materialPatchForm.description} rows="4"></textarea>
				</div>
				<div class="modal-actions">
					<button class="btn btn-ghost" type="button" onclick={() => showMaterialEdit = false}>Batal</button>
					<button class="btn btn-primary" disabled={busy === 'materials'} type="submit">
						{busy === 'materials' ? 'Menyimpan...' : 'Simpan Perubahan'}
					</button>
				</div>
			</form>
		</section>
	</div>
{/if}

{#if showModuleUpload && canCreateModule}
	<div class="modal-backdrop" role="presentation" onclick={closeModuleUpload}>
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
					<span class="summary-label">File Modul</span>
					<h3>Upload File Modul</h3>
					<p>Gunakan PDF, PowerPoint, atau Microsoft Word. Maksimal 5MB per file.</p>
				</div>
				<button class="btn btn-ghost btn-icon" type="button" onclick={closeModuleUpload}>
					<Icons name="x" size={18} />
				</button>
			</div>

			<form class="form-stack modal-form" onsubmit={(event) => { event.preventDefault(); void uploadModule(); }}>
				<div class="form-group">
					<label for="upload-subject">Subjek</label>
					<select id="upload-subject" bind:value={uploadSubjectId} required>
						<option value="">Pilih Subjek</option>
						{#each subjects as subject}
							<option value={subject.id}>{subject.name}</option>
						{/each}
					</select>
				</div>

				<label class="drop-zone" for="module-file" class:invalid={!!selectedFileValid}>
					<input id="module-file" accept=".pdf,.ppt,.pptx,.doc,.docx" type="file" onchange={handleFileChange} />
					<Icons name="upload" size={28} />
					{#if selectedFile}
						<strong>{selectedFile.name}</strong>
						<span>{fileSizeLabel(selectedFile.size)}</span>
					{:else}
						<strong>Pilih file modul</strong>
						<span>PDF, PPT, PPTX, DOC, atau DOCX sampai 5MB</span>
					{/if}
				</label>

				{#if selectedFileValid}
					<div class="inline-error">{selectedFileValid}</div>
				{/if}

				<div class="modal-actions">
					<button class="btn btn-ghost" type="button" onclick={closeModuleUpload}>Batal</button>
					<button class="btn btn-primary" disabled={busy === 'modules' || !selectedFile || !!selectedFileValid} type="submit">
						<Icons name="upload" size={16} />
						{busy === 'modules' ? 'Mengupload...' : 'Upload Modul'}
					</button>
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
	.materials-shell {
		display: grid;
		gap: 1rem;
	}

	.toolbar {
		display: grid;
		grid-template-columns: minmax(240px, 380px) 1fr;
		gap: 1.5rem;
		align-items: end;
		margin-bottom: 0;
	}

	.toolbar-summary {
		display: grid;
		gap: 0.15rem;
		justify-items: end;
		text-align: right;
	}

	.summary-label {
		font-size: 0.72rem;
		font-weight: 800;
		text-transform: uppercase;
		color: var(--text-light);
	}

	.tabs {
		display: inline-flex;
		width: fit-content;
		padding: 0.25rem;
		border: 1px solid var(--border);
		border-radius: var(--radius-sm);
		background: var(--bg-surface);
		box-shadow: var(--shadow-sm);
	}

	.tabs button {
		border: 0;
		background: transparent;
		color: var(--text-muted);
		border-radius: 6px;
		padding: 0.65rem 1rem;
		font-weight: 800;
		cursor: pointer;
	}

	.tabs button.active {
		background: var(--primary);
		color: white;
	}

	.content-grid {
		display: grid;
		grid-template-columns: minmax(0, 1fr) 360px;
		gap: 1.5rem;
		align-items: start;
	}

	.content-grid:has(> section:only-child) {
		grid-template-columns: 1fr;
	}

	.compact-filter {
		padding: 1rem;
		margin-bottom: 1rem;
	}

	.timeline-list,
	.module-list,
	.form-stack {
		display: grid;
		gap: 1rem;
	}

	.material-item {
		display: grid;
		grid-template-columns: 54px minmax(0, 1fr);
		gap: 1rem;
	}

	.meeting-number {
		width: 54px;
		height: 54px;
		display: grid;
		place-items: center;
		border-radius: 12px;
		background: var(--primary-soft);
		color: var(--primary);
		font-weight: 900;
		border: 1px solid var(--primary-border);
	}

	.material-body,
	.module-card {
		background: var(--bg-surface);
		border: 1px solid var(--border);
		border-radius: var(--radius);
		box-shadow: var(--shadow-sm);
		padding: 1.25rem;
	}

	.description-box {
		margin-top: 1rem;
		padding: 0.8rem 0.9rem;
		border-radius: var(--radius-sm);
		background: var(--bg-app);
		color: var(--text-main);
		font-size: 0.9rem;
	}

	.module-card {
		display: grid;
		grid-template-columns: 76px minmax(0, 1fr) auto;
		gap: 1rem;
		align-items: center;
	}

	.file-mark {
		width: 64px;
		height: 64px;
		border-radius: 14px;
		background: var(--primary-soft);
		color: var(--primary);
		border: 1px solid var(--primary-border);
		display: grid;
		place-items: center;
		font-size: 0.7rem;
		font-weight: 900;
	}

	.module-main {
		min-width: 0;
	}

	.module-main h3 {
		font-size: 1rem;
		overflow-wrap: anywhere;
	}

	.module-meta {
		display: flex;
		flex-wrap: wrap;
		gap: 0.5rem;
		margin-top: 0.6rem;
		color: var(--text-muted);
		font-size: 0.78rem;
	}

	.module-actions {
		display: flex;
		gap: 0.5rem;
		align-items: center;
	}

	.item-actions {
		display: flex;
		gap: 0.35rem;
		align-items: center;
	}

	.btn-icon {
		width: 38px;
		height: 38px;
		padding: 0;
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
		width: min(620px, 100%);
		background: var(--bg-surface);
		border: 1px solid var(--border);
		border-radius: var(--radius);
		box-shadow: var(--shadow-lg);
		padding: 1.5rem;
	}

	.modal-head,
	.modal-actions {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 1rem;
	}

	.modal-form {
		margin-top: 1.25rem;
	}

	.sticky-card {
		position: sticky;
		top: 92px;
		margin-bottom: 0;
	}

	.form-row {
		display: grid;
		grid-template-columns: 120px 1fr;
		gap: 1rem;
	}

	.drop-zone {
		display: grid;
		place-items: center;
		text-align: center;
		gap: 0.5rem;
		min-height: 170px;
		padding: 1.25rem;
		border: 2px dashed var(--primary-border);
		border-radius: var(--radius);
		background: var(--primary-soft);
		color: var(--primary);
		cursor: pointer;
		text-transform: none;
		letter-spacing: 0;
	}

	.drop-zone input {
		display: none;
	}

	.drop-zone span {
		color: var(--text-muted);
		font-size: 0.82rem;
		font-weight: 600;
		max-width: 240px;
	}

	.drop-zone.invalid {
		border-color: var(--error-border);
		background: var(--error-soft);
		color: var(--error);
	}

	.inline-error {
		border: 1px solid var(--error-border);
		background: var(--error-soft);
		color: var(--error-strong);
		border-radius: var(--radius-sm);
		padding: 0.75rem;
		font-size: 0.85rem;
		font-weight: 700;
	}

	@media (max-width: 1100px) {
		.toolbar,
		.content-grid {
			grid-template-columns: 1fr;
		}

		.toolbar-summary {
			justify-items: start;
			text-align: left;
		}

		.sticky-card {
			position: static;
		}
	}

	@media (max-width: 720px) {
		.tabs,
		.tabs button {
			width: 100%;
		}

		.tabs {
			display: grid;
			grid-template-columns: 1fr 1fr;
		}

		.module-card {
			grid-template-columns: 1fr;
		}

		.module-actions {
			justify-content: stretch;
		}

		.module-actions .btn-secondary {
			flex: 1;
		}

		.form-row {
			grid-template-columns: 1fr;
		}
	}
</style>
