<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import AccessPanel from '$lib/components/AccessPanel.svelte';
	import ConfirmModal from '$lib/components/ConfirmModal.svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import Icons from '$lib/components/Icons.svelte';
	import Notice from '$lib/components/Notice.svelte';
	import PageTitle from '$lib/components/PageTitle.svelte';
	import Pagination from '$lib/components/Pagination.svelte';
	import { api, hasAnyAuthority, pageItems, paginationMeta, readSession } from '$lib/api';
	import type { Batch, LoginData, Major, PageData, PaginationMeta, User, UserBulkImportResult } from '$lib/types';

	let users = $state<User[]>([]);
	let selectedUser = $state<User | null>(null);
	let batches = $state<Batch[]>([]);
	let majors = $state<Major[]>([]);
	let pageMeta = $state<PaginationMeta>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
	let session = $state<LoginData | null>(null);
	let filters = $state({ name: '', batch: '', major: '', sortBy: 'id', sortDirection: 'desc' });
	let form = $state({ name: '', email: '', batch: '', major: '', gender: 'L' });
	let patch = $state({ id: '', name: '', email: '', batch: '', major: '', gender: 'L', graduatedAt: '', status: 'ACTIVE' });
	let showCreate = $state(false);
	let showEdit = $state(false);
	let showImport = $state(false);
	let selectedFile = $state<File | null>(null);
	let importBatch = $state('');
	let importResult = $state<UserBulkImportResult | null>(null);
	let confirmState = $state({ open: false, title: '', message: '', confirmLabel: 'Ya, lanjutkan' });
	let pendingConfirm: (() => Promise<void>) | null = null;
	let error = $state('');
	let success = $state('');
	let loading = $state(true);
	let busy = $state('');

	const maxImportFileSizeMb = 12;
	const maxImportFileSize = maxImportFileSizeMb * 1024 * 1024;
	const maxImportRows = 100;
	const canCreate = $derived(hasAnyAuthority(session, ['user.create', 'user.*']));
	const canUpdateAll = $derived(hasAnyAuthority(session, ['user.update', 'user.*']));
	const canDelete = $derived(hasAnyAuthority(session, ['user.delete', 'user.*']));
	const portalPrefix = $derived(page.url.pathname.startsWith('/admin/') ? '/admin' : '');

	onMount(() => {
		session = readSession();
		void loadPage();
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

	async function loadPage() {
		loading = true;
		try {
			await Promise.all([loadUsers(), loadBatches(), loadMajors()]);
		} catch (err) {
			error = err instanceof Error ? err.message : 'Gagal memuat user.';
		} finally {
			loading = false;
		}
	}

	async function loadUsers() {
		const query = new URLSearchParams({
			page: String(pageMeta.page),
			size: String(pageMeta.size),
			sort: `${filters.sortBy},${filters.sortDirection}`
		});
		if (filters.name.trim()) query.set('name', filters.name.trim());
		if (filters.batch) query.set('batch', filters.batch);
		if (filters.major) query.set('major', filters.major);
		const payload = await api<PageData<User> | User[]>(`/api/users?${query}`);
		users = pageItems(payload);
		pageMeta = paginationMeta(payload, pageMeta);
	}

	async function loadBatches() {
		const payload = await api<PageData<Batch> | Batch[]>('/api/batches?sort=id,desc');
		batches = pageItems(payload);
		if (!form.batch && batches[0]) form.batch = String(batches[0].id);
	}

	async function loadMajors() {
		const payload = await api<PageData<Major> | Major[]>('/api/majors?sort=name,asc&size=100');
		majors = pageItems(payload);
		if (!form.major && majors[0]) form.major = majors[0].id;
	}

	async function createUser() {
		await submit(async () => {
			busy = 'create';
			try {
				await api<User>('/api/users', {
					method: 'POST',
					body: JSON.stringify({
						name: form.name,
						email: form.email,
						batch: Number(form.batch),
						major: form.major,
						gender: form.gender
					})
				});
				form = { name: '', email: '', batch: form.batch, major: form.major, gender: 'L' };
				showCreate = false;
				await loadUsers();
				success = 'User berhasil ditambahkan.';
			} finally {
				busy = '';
			}
		});
	}

	async function patchUser() {
		await submit(async () => {
			busy = 'patch';
			try {
				const body: Record<string, unknown> = {};
				if (canUpdateAll && patch.name.trim()) body.name = patch.name.trim();
				if (patch.email.trim()) body.email = patch.email.trim();
				if (canUpdateAll && patch.batch) body.batch = Number(patch.batch);
				if (canUpdateAll && patch.major) body.major = patch.major;
				if (canUpdateAll && patch.gender) body.gender = patch.gender;
				if (canUpdateAll && patch.graduatedAt) body.graduatedAt = patch.graduatedAt;
				if (canUpdateAll && patch.status) body.status = patch.status;

				await api<User>(`/api/users/${patch.id}`, { method: 'PATCH', body: JSON.stringify(body) });
				showEdit = false;
				patch = { id: '', name: '', email: '', batch: '', major: '', gender: 'L', graduatedAt: '', status: 'ACTIVE' };
				await loadUsers();
				success = 'User berhasil diupdate.';
			} finally {
				busy = '';
			}
		});
	}

	async function importUsers() {
		await submit(async () => {
			if (!selectedFile) {
				error = 'Pilih file Excel terlebih dulu.';
				return;
			}
			if (!isExcelFile(selectedFile)) {
				error = 'File harus berformat .xlsx atau .xls.';
				return;
			}
			if (selectedFile.size > maxImportFileSize) {
				error = `Ukuran file maksimal ${maxImportFileSizeMb}MB.`;
				return;
			}
			if (!importBatch) {
				error = 'Pilih batch tujuan import.';
				return;
			}
			busy = 'import';
			try {
				const data = new FormData();
				data.set('file', selectedFile);
				data.set('batch', importBatch);
				const payload = await api<UserBulkImportResult>('/api/users/bulk/excel', {
					method: 'POST',
					body: data
				});
				importResult = payload.data ?? null;
				selectedFile = null;
				await loadUsers();
				success = importResult
					? `${importResult.createdCount} dibuat, ${importResult.skippedCount} dilewati, ${importResult.failedCount} gagal.`
					: 'Import user selesai.';
			} finally {
				busy = '';
			}
		});
	}

	async function deleteUser(user: User) {
		askConfirm({
			title: 'Hapus user?',
			message: `User ${user.name} akan dihapus dari sistem.`,
			confirmLabel: 'Hapus User'
		}, async () => {
			await submit(async () => {
				await api<null>(`/api/users/${user.id}`, { method: 'DELETE' });
				await loadUsers();
				if (selectedUser?.id === user.id) {
					showEdit = false;
					selectedUser = null;
					patch = { id: '', name: '', email: '', batch: '', major: '', gender: 'L', graduatedAt: '', status: 'ACTIVE' };
				}
				success = 'User berhasil dihapus.';
			});
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

	function openCreate() {
		form = {
			name: '',
			email: '',
			batch: form.batch || (batches[0] ? String(batches[0].id) : ''),
			major: form.major || (majors[0] ? majors[0].id : ''),
			gender: 'L'
		};
		showCreate = true;
	}

	function openDetail(user: User) {
		void goto(`${portalPrefix}/users/${user.id}`);
	}

	async function openEdit(user: User) {
		if (!canManageUser(user)) return;
		const detailPayload = await api<User>(`/api/users/${user.id}`);
		const detail = detailPayload.data ?? user;
		selectedUser = detail;
		patch = {
			id: String(detail.id),
			name: detail.name,
			email: detail.email ?? '',
			batch: batchIdFromLabel(detail.batch),
			major: detail.majorId ?? '',
			gender: genderCode(detail.gender ?? 'L'),
			graduatedAt: detail.graduatedAt ? detail.graduatedAt.slice(0, 10) : '',
			status: detail.status ?? 'ACTIVE'
		};
		showEdit = true;
	}

	function openImport() {
		selectedFile = null;
		importBatch = filters.batch || form.batch || (batches[0] ? String(batches[0].id) : '');
		importResult = null;
		showImport = true;
	}

	function applyFilters() {
		pageMeta = { ...pageMeta, page: 0 };
		void loadUsers();
	}

	function changePage(page: number) {
		pageMeta = { ...pageMeta, page };
		void loadUsers();
	}

	function changePageSize(size: number) {
		pageMeta = { ...pageMeta, page: 0, size };
		void loadUsers();
	}

	function onFileChange(event: Event) {
		const input = event.currentTarget as HTMLInputElement;
		selectedFile = input.files?.[0] ?? null;
		importResult = null;
	}

	function genderCode(value: string) {
		return value.toUpperCase().startsWith('L') ? 'L' : 'P';
	}

	function genderLabel(value: string) {
		return genderCode(value) === 'L' ? 'Laki-laki' : 'Perempuan';
	}

	function userStatusLabel(value?: string) {
		if (value === 'GRADUATED') return 'Lulus';
		if (value === 'DROP_OUT') return 'Drop Out';
		return 'Aktif';
	}

	function batchIdFromLabel(value: string) {
		return value.split(' - ')[0] ?? value;
	}

	function canManageUser(user: User) {
		return canUpdateAll || user.id === session?.userId || canDelete;
	}

	function canEditMainFields() {
		return canUpdateAll;
	}

	function isExcelFile(file: File) {
		const name = file.name.toLowerCase();
		return name.endsWith('.xlsx') || name.endsWith('.xls');
	}

	function fileSizeLabel(file: File) {
		return `${(file.size / 1024 / 1024).toFixed(2)} MB`;
	}

	async function downloadImportTemplate() {
		await submit(async () => {
			busy = 'template';
			try {
				const headers = new Headers();
				const currentSession = readSession();
				if (currentSession?.sessionId) headers.set('Authorization', `Session ${currentSession.sessionId}`);

				const response = await fetch('/api/users/bulk/excel/template', { headers });
				if (!response.ok) throw new Error('Gagal download template import user.');

				const blob = await response.blob();
				const url = URL.createObjectURL(blob);
				const anchor = document.createElement('a');
				anchor.href = url;
				anchor.download = filenameFromDisposition(response.headers.get('Content-Disposition')) ?? 'users-import-template.xlsx';
				document.body.appendChild(anchor);
				anchor.click();
				anchor.remove();
				URL.revokeObjectURL(url);
				success = 'Template import berhasil didownload.';
			} finally {
				busy = '';
			}
		});
	}

	function csvEscape(value: unknown) {
		const text = String(value ?? '');
		return `"${text.replaceAll('"', '""')}"`;
	}

	function filenameFromDisposition(disposition: string | null) {
		if (!disposition) return null;
		const match = disposition.match(/filename="?([^";]+)"?/i);
		return match?.[1] ?? null;
	}

	function downloadImportReport() {
		if (!importResult) return;

		const header = ['row', 'email', 'status', 'message'];
		const lines = [
			header.map(csvEscape).join(','),
			...importResult.rows.map((row) =>
				[row.row, row.email, row.status, row.message]
					.map(csvEscape)
					.join(',')
			)
		];
		const csv = lines.join('\n');
		const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
		const url = URL.createObjectURL(blob);
		const anchor = document.createElement('a');
		anchor.href = url;
		anchor.download = `user-import-report-${new Date().toISOString().slice(0, 19)}.csv`;
		document.body.appendChild(anchor);
		anchor.click();
		anchor.remove();
		URL.revokeObjectURL(url);
	}
</script>

<svelte:head><title>Users - Divdik Course</title></svelte:head>

<div class="page-heading">
	<PageTitle
		eyebrow="User Management"
		title="Peserta Kursus"
		description="Kelola profil peserta dan batch. Akun login serta role sistem diproses otomatis oleh auth-service."
	/>

	{#if canCreate}
		<div class="page-actions">
			<button class="btn btn-ghost" type="button" disabled={busy === 'template'} onclick={downloadImportTemplate}>
				<Icons name="download" size={17} />
				<span>{busy === 'template' ? 'Mengunduh...' : 'Template'}</span>
			</button>
			<button class="btn btn-secondary" type="button" onclick={openImport}>
				<Icons name="upload" size={17} />
				<span>Import Excel</span>
			</button>
			<button class="btn btn-primary" type="button" onclick={openCreate}>
				<Icons name="users" size={17} />
				<span>Tambah User</span>
			</button>
		</div>
	{/if}
</div>

<Notice {error} {success} />

<AccessPanel authorities={['user.read', 'user.*']}>
	<section class="toolbar">
		<form class="filters" onsubmit={(event) => { event.preventDefault(); applyFilters(); }}>
			<label>
				<span>Nama</span>
				<input bind:value={filters.name} placeholder="Cari nama..." />
			</label>
			<label>
				<span>Batch</span>
				<select bind:value={filters.batch}>
					<option value="">Semua batch</option>
					{#each batches as batch}
						<option value={batch.id}>Batch {batch.id}</option>
					{/each}
				</select>
			</label>
			<label>
				<span>Jurusan</span>
				<select bind:value={filters.major}>
					<option value="">Semua jurusan</option>
					{#each majors as major}
						<option value={major.id}>{major.id} - {major.name}</option>
					{/each}
				</select>
			</label>
			<label>
				<span>Urutkan</span>
				<select bind:value={filters.sortBy}>
					<option value="id">Terbaru</option>
					<option value="name">Nama</option>
				</select>
			</label>
			<label>
				<span>Arah</span>
				<select bind:value={filters.sortDirection}>
					<option value="asc">Naik</option>
					<option value="desc">Turun</option>
				</select>
			</label>
			<button class="btn btn-primary filter-submit" type="submit">
				<Icons name="search" size={17} />
				<span>Filter</span>
			</button>
		</form>
	</section>

	{#if loading}
		<div class="card">Memuat data user...</div>
	{:else if users.length === 0}
		<EmptyState text="Belum ada user yang sesuai filter." />
	{:else}
		<section class="user-grid">
			{#each users as user}
				<article class="user-card">
					<div class="user-card-main">
						<div class="avatar">{user.name.slice(0, 1).toUpperCase()}</div>
						<div class="user-text">
							<h3>{user.name}</h3>
							<p>{user.majorName ?? 'Jurusan belum diisi'}</p>
						</div>
					</div>

					<div class="user-meta">
						<span class="badge badge-gray">Batch {user.batch}</span>
						<span class="badge badge-blue">{user.majorId ?? '-'}</span>
					</div>

					<div class="user-actions">
						<button class="btn btn-ghost" type="button" onclick={() => openDetail(user)}>
							<Icons name="file" size={16} />
							<span>Detail</span>
						</button>
						{#if canManageUser(user)}
							<button class="btn btn-secondary" type="button" onclick={() => void openEdit(user)}>
								<Icons name="edit" size={16} />
								<span>Manage</span>
							</button>
						{/if}
					</div>
				</article>
			{/each}
		</section>
		<Pagination meta={pageMeta} onPage={changePage} onSize={changePageSize} />
	{/if}
</AccessPanel>

{#if showCreate && canCreate}
	<div class="modal-backdrop" role="presentation" onclick={() => showCreate = false}>
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
					<span class="nav-label flush">Create</span>
					<h3>Tambah User</h3>
				</div>
				<button class="btn btn-ghost icon-btn" type="button" onclick={() => showCreate = false}>
					<Icons name="x" size={18} />
				</button>
			</div>

			<form class="modal-form" onsubmit={(event) => { event.preventDefault(); void createUser(); }}>
				<label>
					<span>Nama Lengkap</span>
					<input bind:value={form.name} placeholder="Contoh: Budi Santoso" required />
				</label>
				<label>
					<span>Email</span>
					<input bind:value={form.email} type="email" placeholder="budi@example.com" required />
				</label>
				<label>
					<span>Batch</span>
					<select bind:value={form.batch} required>
						{#each batches as batch}
							<option value={batch.id}>Batch {batch.id}</option>
						{/each}
					</select>
				</label>
				<label>
					<span>Jurusan</span>
					<select bind:value={form.major} required>
						<option value="">Pilih jurusan</option>
						{#each majors as major}
							<option value={major.id}>{major.id} - {major.name}</option>
						{/each}
					</select>
				</label>
				<label>
					<span>Gender</span>
					<select bind:value={form.gender}>
						<option value="L">Laki-laki</option>
						<option value="P">Perempuan</option>
					</select>
				</label>
				<div class="modal-actions">
					<button class="btn btn-ghost" type="button" onclick={() => showCreate = false}>Batal</button>
					<button class="btn btn-primary" type="submit" disabled={busy === 'create'}>
						{busy === 'create' ? 'Menyimpan...' : 'Simpan'}
					</button>
				</div>
			</form>
		</section>
	</div>
{/if}

{#if showEdit && selectedUser}
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
					<span class="nav-label flush">Update</span>
					<h3>Manage User</h3>
				</div>
				<button class="btn btn-ghost icon-btn" type="button" onclick={() => showEdit = false}>
					<Icons name="x" size={18} />
				</button>
			</div>

			<form class="modal-form" onsubmit={(event) => { event.preventDefault(); void patchUser(); }}>
				<label>
					<span>Nama Lengkap</span>
					<input bind:value={patch.name} disabled={!canEditMainFields()} required />
				</label>
				<label>
					<span>Email</span>
					<input bind:value={patch.email} type="email" required />
				</label>
				<label>
					<span>Batch</span>
					<select bind:value={patch.batch} disabled={!canEditMainFields()} required>
						{#each batches as batch}
							<option value={batch.id}>Batch {batch.id}</option>
						{/each}
					</select>
				</label>
				<label>
					<span>Jurusan</span>
					<select bind:value={patch.major} disabled={!canEditMainFields()} required>
						{#each majors as major}
							<option value={major.id}>{major.id} - {major.name}</option>
						{/each}
					</select>
				</label>
				<label>
					<span>Gender</span>
					<select bind:value={patch.gender} disabled={!canEditMainFields()}>
						<option value="L">Laki-laki</option>
						<option value="P">Perempuan</option>
					</select>
				</label>
				<label>
					<span>Status Course</span>
					<select bind:value={patch.status} disabled={!canEditMainFields()}>
						<option value="ACTIVE">{userStatusLabel('ACTIVE')}</option>
						<option value="GRADUATED">{userStatusLabel('GRADUATED')}</option>
						<option value="DROP_OUT">{userStatusLabel('DROP_OUT')}</option>
					</select>
				</label>
				<label>
					<span>Tanggal Lulus</span>
					<input bind:value={patch.graduatedAt} disabled={!canEditMainFields()} type="date" />
				</label>
				<div class="modal-actions">
					<div class="action-cluster">
						{#if canDelete}
							<button class="btn btn-ghost danger" type="button" onclick={() => selectedUser && deleteUser(selectedUser)}>
								<Icons name="x" size={16} />
								<span>Hapus</span>
							</button>
						{/if}
					</div>
					<div class="action-cluster">
						<button class="btn btn-ghost" type="button" onclick={() => showEdit = false}>Batal</button>
						<button class="btn btn-primary" type="submit" disabled={busy === 'patch'}>
							{busy === 'patch' ? 'Menyimpan...' : 'Simpan'}
						</button>
					</div>
				</div>
			</form>
		</section>
	</div>
{/if}

{#if showImport && canCreate}
	<div class="modal-backdrop" role="presentation" onclick={() => showImport = false}>
		<section
			class="modal-panel import-panel"
			role="dialog"
			aria-modal="true"
			tabindex="-1"
			onclick={(event) => event.stopPropagation()}
			onkeydown={(event) => event.stopPropagation()}
		>
			<div class="modal-head">
				<div>
					<span class="nav-label flush">Bulk Create</span>
					<h3>Import User dari Excel</h3>
				</div>
				<button class="btn btn-ghost icon-btn" type="button" onclick={() => showImport = false}>
					<Icons name="x" size={18} />
				</button>
			</div>

			<form class="modal-form" onsubmit={(event) => { event.preventDefault(); void importUsers(); }}>
				<div class="format-box">
						<Icons name="file" size={18} />
					<div>
						<strong>Format kolom Excel</strong>
						<p>Gunakan template resmi. Header berada di baris ke-4 dengan kolom: nama, email, jeniskelamin, jurusan. Batch dipilih dari form ini, jurusan diisi per row memakai ID jurusan seperti TI.</p>
					</div>
					<button class="btn btn-secondary" type="button" disabled={busy === 'template'} onclick={downloadImportTemplate}>
						<Icons name="download" size={16} />
						<span>{busy === 'template' ? 'Mengunduh...' : 'Download Template'}</span>
					</button>
				</div>

				<label>
					<span>Batch Tujuan</span>
					<select bind:value={importBatch} required>
						{#each batches as batch}
							<option value={batch.id}>Batch {batch.id}</option>
						{/each}
					</select>
				</label>

				<label class="file-zone">
					<input type="file" accept=".xlsx,.xls" onchange={onFileChange} />
					<Icons name="upload" size={22} />
					<span>{selectedFile ? selectedFile.name : 'Pilih file Excel'}</span>
					<small>{selectedFile ? fileSizeLabel(selectedFile) : `Maksimal ${maxImportFileSizeMb}MB, sampai ${maxImportRows} row data.`}</small>
				</label>

				{#if importResult}
					<div class="import-result">
						<div class="result-summary">
							<span><strong>{importResult.createdCount}</strong> dibuat</span>
							<span><strong>{importResult.skippedCount}</strong> dilewati</span>
							<span><strong>{importResult.failedCount}</strong> gagal</span>
						</div>

						{#if importResult.rows.length > 0}
							<div class="error-list">
								{#each importResult.rows.slice(0, 8) as rowReport}
									<p class:ok={rowReport.status === 'CREATED'} class:skip={rowReport.status === 'SKIPPED'}>
										Row {rowReport.row}: {rowReport.status} - {rowReport.email || '-'} - {rowReport.message}
									</p>
								{/each}
								{#if importResult.rows.length > 8}
									<p>{importResult.rows.length - 8} row lain tidak ditampilkan.</p>
								{/if}
							</div>
						{/if}
					</div>
				{/if}

				<div class="modal-actions">
					<button class="btn btn-ghost" type="button" onclick={() => showImport = false}>Tutup</button>
					<div class="action-cluster">
						{#if importResult}
							<button class="btn btn-secondary" type="button" onclick={downloadImportReport}>
								<Icons name="download" size={16} />
								<span>Download CSV</span>
							</button>
						{/if}
						<button class="btn btn-primary" type="submit" disabled={busy === 'import' || !selectedFile || !importBatch}>
							{busy === 'import' ? 'Mengimport...' : 'Import'}
						</button>
					</div>
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
		margin-bottom: 1.25rem;
	}

	.page-heading :global(.page-header-container) {
		margin-bottom: 0;
		min-width: 0;
	}

	.page-actions {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		flex-wrap: wrap;
		justify-content: flex-end;
		padding-top: 0.35rem;
	}

	.toolbar {
		display: flex;
		align-items: end;
		justify-content: space-between;
		gap: 1rem;
		margin-bottom: 1.5rem;
	}

	.filters {
		display: grid;
		grid-template-columns: minmax(190px, 1fr) repeat(3, minmax(140px, 190px)) auto;
		gap: 0.875rem;
		align-items: end;
		flex: 1;
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

	.user-grid {
		display: grid;
		grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
		gap: 1rem;
	}

	.user-card {
		background: var(--bg-surface);
		border: 1px solid var(--border);
		border-radius: var(--radius);
		padding: 1.25rem;
		box-shadow: var(--shadow-sm);
		display: grid;
		gap: 1.15rem;
		min-height: 210px;
		min-width: 0;
	}

	.user-card-main {
		display: flex;
		gap: 0.875rem;
		align-items: flex-start;
		min-width: 0;
	}

	.avatar {
		width: 44px;
		height: 44px;
		border-radius: var(--radius-sm);
		background: var(--primary-soft);
		color: var(--primary);
		display: grid;
		place-items: center;
		font-weight: 900;
		flex: 0 0 auto;
	}

	.user-text {
		min-width: 0;
	}

	.user-text h3 {
		font-size: 1.05rem;
		margin-bottom: 0.25rem;
		letter-spacing: 0;
		overflow-wrap: anywhere;
	}

	.user-text p {
		font-size: 0.875rem;
		overflow-wrap: anywhere;
	}

	.user-meta,
	.user-actions {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 0.75rem;
		min-width: 0;
	}

	.user-meta {
		flex-wrap: wrap;
		color: var(--text-muted);
		font-size: 0.85rem;
		font-weight: 700;
	}

	.user-actions {
		margin-top: auto;
	}

	.danger {
		color: var(--error);
	}

	.icon-btn {
		width: 40px;
		height: 40px;
		padding: 0;
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
		width: min(520px, 100%);
		background: var(--bg-surface);
		border: 1px solid var(--border);
		border-radius: var(--radius);
		box-shadow: var(--shadow-lg);
		padding: 1.5rem;
		max-height: min(92vh, 760px);
		overflow: auto;
	}

	.import-panel {
		width: min(620px, 100%);
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

	.format-box {
		display: grid;
		grid-template-columns: auto minmax(0, 1fr) auto;
		gap: 0.75rem;
		align-items: start;
		padding: 0.875rem;
		border: 1px solid color-mix(in srgb, var(--primary) 28%, var(--border));
		background: color-mix(in srgb, var(--primary) 8%, var(--bg-surface));
		border-radius: var(--radius-sm);
	}

	.format-box :global(svg) {
		color: var(--primary);
		margin-top: 0.125rem;
	}

	.format-box p {
		font-size: 0.875rem;
		line-height: 1.5;
		margin: 0.25rem 0 0;
	}

	.format-box .btn {
		align-self: center;
		white-space: nowrap;
	}

	.file-zone {
		border: 1px dashed color-mix(in srgb, var(--primary) 45%, var(--border));
		border-radius: var(--radius);
		padding: 1.25rem;
		place-items: center;
		text-align: center;
		cursor: pointer;
		background: color-mix(in srgb, var(--primary) 5%, var(--bg-surface));
	}

	.file-zone input {
		position: absolute;
		width: 1px;
		height: 1px;
		opacity: 0;
		pointer-events: none;
	}

	.file-zone :global(svg) {
		color: var(--primary);
	}

	.file-zone span {
		color: var(--text-main);
		font-size: 0.95rem;
		overflow-wrap: anywhere;
	}

	.file-zone small {
		color: var(--text-muted);
	}

	.import-result {
		border: 1px solid var(--border);
		border-radius: var(--radius-sm);
		padding: 0.875rem;
		display: grid;
		gap: 0.75rem;
	}

	.result-summary {
		display: flex;
		gap: 0.75rem;
		flex-wrap: wrap;
		font-size: 0.875rem;
		color: var(--text-muted);
	}

	.error-list {
		display: grid;
		gap: 0.375rem;
	}

	.error-list p {
		margin: 0;
		font-size: 0.825rem;
		color: var(--error);
		overflow-wrap: anywhere;
	}

	.error-list p.ok {
		color: var(--success);
	}

	.error-list p.skip {
		color: var(--text-muted);
	}

	.action-cluster {
		display: flex;
		gap: 0.75rem;
		flex-wrap: wrap;
		justify-content: flex-end;
	}

	.flush {
		padding: 0;
		margin: 0 0 0.25rem;
	}

	@media (max-width: 760px) {
		.page-heading {
			display: grid;
		}

		.page-actions {
			justify-content: stretch;
		}

		.page-actions .btn {
			flex: 1;
		}

		.toolbar {
			display: grid;
			align-items: stretch;
		}

		.filters {
			grid-template-columns: 1fr;
		}

		.user-grid {
			grid-template-columns: 1fr;
		}

		.user-actions {
			align-items: stretch;
		}

		.user-actions .btn:first-child {
			flex: 1;
		}

		.modal-actions {
			align-items: stretch;
			flex-direction: column-reverse;
		}

		.modal-actions .btn,
		.action-cluster {
			width: 100%;
		}

		.action-cluster {
			display: grid;
		}

		.format-box {
			grid-template-columns: auto minmax(0, 1fr);
		}

		.format-box .btn {
			grid-column: 1 / -1;
			width: 100%;
		}
	}
</style>
