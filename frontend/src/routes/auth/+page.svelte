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
	import type { AuthUser, LoginData, PageData, PaginationMeta } from '$lib/types';

	let users = $state<AuthUser[]>([]);
	let pageMeta = $state<PaginationMeta>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
	let session = $state<LoginData | null>(null);
	let selectedUser = $state<AuthUser | null>(null);
	let selectedRole = $state('');
	let email = $state('');
	let status = $state('');
	let showManage = $state(false);
	let confirmState = $state({ open: false, title: '', message: '', confirmLabel: 'Ya, lanjutkan' });
	let pendingConfirm: (() => Promise<void>) | null = null;
	let error = $state('');
	let success = $state('');
	let busy = $state('');

	const managedRoles = ['CHIEF', 'CHIEF_DEPUTY', 'CHIEF_INSTRUCTOR', 'CHIEF_DEPUTY_INSTRUCTOR'];
	const canUpdateAuth = $derived(hasAnyAuthority(session, ['user.update', 'user.*']));
	const canDemoteRole = $derived(hasAnyAuthority(session, ['user.delete', 'user.*']));
	const canManageAuth = $derived(canUpdateAuth || canDemoteRole);

	onMount(() => {
		session = readSession();
		if (hasAnyAuthority(session, ['user.*'])) void load();
	});

	async function load() {
		const query = new URLSearchParams({
			page: String(pageMeta.page),
			size: String(pageMeta.size),
			sort: 'id,desc'
		});
		if (email.trim()) query.set('email', email.trim());
		if (status) query.set('status', status);
		const payload = await api<PageData<AuthUser> | AuthUser[]>(`/api/auths/users?${query}`);
		users = pageItems(payload);
		pageMeta = paginationMeta(payload, pageMeta);
	}

	async function submit(task: () => Promise<void>) {
		error = ''; success = '';
		try { await task(); } catch (err) { error = err instanceof Error ? err.message : 'Request gagal.'; }
	}

	async function updateStatus(user: AuthUser, status: 'ACTIVE' | 'BANNED') {
		await submit(async () => {
			busy = `${status}-${user.id}`;
			try {
				await api<AuthUser>(`/api/auths/users/${user.id}/status`, {
					method: 'PATCH',
					body: JSON.stringify({
						status,
						revokeSessions: status === 'BANNED'
					})
				});
				await load();
				closeManage();
				success = status === 'BANNED' ? 'Akun berhasil diblokir.' : 'Akun berhasil diaktifkan.';
			} finally {
				busy = '';
			}
		});
	}

	async function setManagedRole(user: AuthUser, role: string = selectedRole) {
		if (!role || hasRole(user, 'ADMIN') || !canUpdateAuth) return;
		await submit(async () => {
			busy = `role-${user.id}`;
			try {
				await api<AuthUser>(`/api/auths/users/${user.id}/role`, {
					method: 'PATCH',
					body: JSON.stringify({ role })
				});
				await load();
				closeManage();
				success = `Role ${user.email} berhasil diubah.`;
			} finally {
				busy = '';
			}
		});
	}

	async function demoteRole(user: AuthUser) {
		if (hasRole(user, 'ADMIN') || !canDemoteRole) return;

		askConfirm({
			title: 'Turunkan role?',
			message: `Role ${user.email} akan diturunkan ke USER saja.`,
			confirmLabel: 'Turunkan Role'
		}, async () => {
			await submit(async () => {
				busy = `demote-${user.id}`;
				try {
					await api<AuthUser>(`/api/auths/users/${user.id}/role`, { method: 'DELETE' });
					await load();
					closeManage();
					success = `Role ${user.email} berhasil diturunkan ke USER.`;
				} finally {
					busy = '';
				}
			});
		});
	}

	function requestStatusChange(user: AuthUser, status: 'ACTIVE' | 'BANNED') {
		if (status === 'BANNED') {
			askConfirm({
				title: 'Blokir akun?',
				message: `Akun ${user.email} akan diblokir dan session aktif akan dicabut.`,
				confirmLabel: 'Blokir Akun'
			}, async () => {
				await updateStatus(user, status);
			});
			return;
		}

		void updateStatus(user, status);
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

	function hasRole(user: AuthUser, role: string) {
		return (user.roles ?? []).some((value) => value.toUpperCase() === role);
	}

	function currentManagedRole(user: AuthUser) {
		return (user.roles ?? []).find((role) => managedRoles.includes(role.toUpperCase())) ?? '';
	}

	function openManage(user: AuthUser) {
		selectedUser = user;
		selectedRole = currentManagedRole(user);
		showManage = true;
	}

	function closeManage() {
		showManage = false;
		selectedUser = null;
		selectedRole = '';
	}

	function lastLoginLabel(value?: string | null) {
		if (!value) return 'Belum pernah login';
		const parsed = new Date(value);
		if (Number.isNaN(parsed.getTime())) return '-';
		return new Intl.DateTimeFormat('id-ID', {
			dateStyle: 'medium',
			timeStyle: 'short'
		}).format(parsed);
	}

	function changePage(page: number) {
		pageMeta = { ...pageMeta, page };
		void load();
	}

	function changePageSize(size: number) {
		pageMeta = { ...pageMeta, page: 0, size };
		void load();
	}

	function applyFilters() {
		pageMeta = { ...pageMeta, page: 0 };
		void load();
	}

	function resetFilters() {
		email = '';
		status = '';
		applyFilters();
	}
</script>

<svelte:head><title>Auth - Divdik Course</title></svelte:head>

<PageTitle eyebrow="Security & Auth" title="Auth Identities" description="Kelola identitas autentikasi, status akun, dan penetapan role keamanan sistem." />

<Notice {error} {success} />

<AccessPanel authorities={['user.read', 'user.*']}>
	<form class="filter-card" onsubmit={(event) => { event.preventDefault(); applyFilters(); }}>
		<label>
			<span>Email</span>
			<input bind:value={email} placeholder="Cari email auth..." />
		</label>
		<label>
			<span>Status</span>
			<select bind:value={status}>
				<option value="">Semua status</option>
				<option value="ACTIVE">ACTIVE</option>
				<option value="PENDING_ACTIVATION">PENDING_ACTIVATION</option>
				<option value="BANNED">BANNED</option>
			</select>
		</label>
		<div class="filter-actions">
			<button class="btn btn-secondary" type="submit">
				<Icons name="search" size={16} />
				<span>Cari</span>
			</button>
			<button class="btn btn-ghost" type="button" onclick={resetFilters}>Reset</button>
		</div>
	</form>

	<div class="table-container">
		<table>
			<thead>
				<tr>
					<th>ID</th>
					<th>Identity Email</th>
					<th>Status</th>
					<th>Roles</th>
					<th>Last Login</th>
					<th class="text-right">Aksi</th>
				</tr>
			</thead>
			<tbody>
				{#each users as user}
					<tr>
						<td><small>#{user.id}</small></td>
						<td>
							<div class="identity-cell">
								<div class="avatar">{user.email.charAt(0).toUpperCase()}</div>
								<div>
									<strong>{user.email}</strong>
									<small>Auth identity</small>
								</div>
							</div>
						</td>
						<td>
							<span class="badge" class:badge-green={user.status === 'ACTIVE'} class:badge-red={user.status === 'BANNED'} class:badge-gray={user.status !== 'ACTIVE' && user.status !== 'BANNED'}>
								{user.status}
							</span>
						</td>
						<td>
							<div class="role-list">
								{#each user.roles || [] as role}
									<span class="badge badge-blue role-badge">{role}</span>
								{/each}
							</div>
						</td>
						<td><span class="muted-date">{lastLoginLabel(user.lastLogin)}</span></td>
						<td class="text-right">
							{#if canManageAuth && !hasRole(user, 'ADMIN')}
								<button class="btn btn-secondary" type="button" onclick={() => openManage(user)}>
									<Icons name="edit" size={16} />
									<span>Kelola</span>
								</button>
							{:else if hasRole(user, 'ADMIN')}
								<span class="badge badge-gray">Protected</span>
							{:else}
								<span class="muted-date">Read only</span>
							{/if}
						</td>
					</tr>
				{:else}
					<tr><td colspan="6"><EmptyState text="Belum ada identitas auth." /></td></tr>
				{/each}
			</tbody>
		</table>
	</div>
	<Pagination meta={pageMeta} onPage={changePage} onSize={changePageSize} />
</AccessPanel>

{#if showManage && selectedUser}
	<div class="modal-backdrop" role="presentation" onclick={closeManage}>
		<section
			class="modal-panel manage-panel"
			role="dialog"
			aria-modal="true"
			tabindex="-1"
			onclick={(event) => event.stopPropagation()}
			onkeydown={(event) => event.stopPropagation()}
		>
			<div class="modal-head">
				<div>
					<span class="modal-eyebrow">Auth Identity</span>
					<h3>Kelola Akun</h3>
					<p>{selectedUser.email}</p>
				</div>
				<button class="btn btn-ghost icon-button" aria-label="Tutup modal" type="button" onclick={closeManage}>
					<Icons name="x" size={18} />
				</button>
			</div>

			<div class="account-summary">
				<div>
					<span>Status</span>
					<strong>{selectedUser.status}</strong>
				</div>
				<div>
					<span>Last Login</span>
					<strong>{lastLoginLabel(selectedUser.lastLogin)}</strong>
				</div>
				<div>
					<span>Roles</span>
					<strong>{(selectedUser.roles ?? []).join(', ') || '-'}</strong>
				</div>
			</div>

			{#if canUpdateAuth}
				<section class="manage-section">
					<div>
						<h4>Status Akun</h4>
						<p>Blokir akan mencabut session aktif. Aktifkan akan membuka akses login kembali.</p>
					</div>
					<div class="manage-actions">
						{#if selectedUser.status === 'BANNED'}
							<button class="btn btn-primary" disabled={busy === `ACTIVE-${selectedUser.id}`} type="button" onclick={() => requestStatusChange(selectedUser!, 'ACTIVE')}>
								<Icons name="checkCircle" size={16} />
								Aktifkan
							</button>
						{:else}
							<button class="btn btn-ghost danger-action" disabled={busy === `BANNED-${selectedUser.id}`} type="button" onclick={() => requestStatusChange(selectedUser!, 'BANNED')}>
								<Icons name="x" size={16} />
								Blokir Akun
							</button>
						{/if}
					</div>
				</section>

				<section class="manage-section">
					<div>
						<h4>Ganti Role</h4>
						<p>Pilih role manajerial. Role USER tetap menjadi default dasar.</p>
					</div>
					<div class="role-editor">
						<select bind:value={selectedRole} disabled={busy === `role-${selectedUser.id}`}>
							<option value="">USER only</option>
							{#each managedRoles as role}
								<option value={role}>{role}</option>
							{/each}
						</select>
						<button class="btn btn-primary" disabled={!selectedRole || busy === `role-${selectedUser.id}`} type="button" onclick={() => void setManagedRole(selectedUser!)}>
							Simpan Role
						</button>
					</div>
				</section>
			{/if}

			{#if canDemoteRole}
				<section class="manage-section danger-section">
					<div>
						<h4>Turunkan Role</h4>
						<p>Hapus role manajerial dan sisakan USER sebagai role dasar.</p>
					</div>
					<button
						class="btn btn-ghost danger-action"
						type="button"
						disabled={!currentManagedRole(selectedUser) || busy === `demote-${selectedUser.id}`}
						onclick={() => void demoteRole(selectedUser!)}
					>
						Demote ke USER
					</button>
				</section>
			{/if}
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
	button span {
		white-space: nowrap;
	}

	.filter-card {
		display: grid;
		grid-template-columns: minmax(220px, 1fr) minmax(190px, 0.35fr) auto;
		align-items: end;
		gap: 0.85rem;
		margin-bottom: 1rem;
		padding: 1rem;
		background: var(--bg-surface);
		border: 1px solid var(--border);
		border-radius: var(--radius-sm);
		box-shadow: var(--shadow-sm);
	}

	.filter-card label {
		display: grid;
		gap: 0.35rem;
	}

	.filter-card label span {
		color: var(--text-muted);
		font-size: 0.75rem;
		font-weight: 800;
		letter-spacing: 0.04em;
		text-transform: uppercase;
	}

	.filter-actions {
		display: flex;
		gap: 0.5rem;
	}

	.identity-cell {
		display: flex;
		align-items: center;
		gap: 0.85rem;
		min-width: 240px;
	}

	.identity-cell > div:last-child {
		display: grid;
		gap: 0.15rem;
	}

	.avatar {
		width: 38px;
		height: 38px;
		display: grid;
		place-items: center;
		border-radius: 12px;
		background: var(--primary-soft);
		color: var(--primary);
		font-weight: 900;
		border: 1px solid var(--primary-border);
	}

	.role-list {
		display: flex;
		flex-wrap: wrap;
		gap: 0.35rem;
		max-width: 360px;
	}

	.role-badge {
		font-size: 0.65rem;
	}

	.muted-date {
		color: var(--text-muted);
		font-size: 0.82rem;
		font-weight: 700;
	}

	.manage-panel {
		width: min(760px, 100%);
	}

	.icon-button {
		width: 38px;
		height: 38px;
		padding: 0;
	}

	.modal-eyebrow {
		color: var(--primary);
		font-size: 0.72rem;
		font-weight: 900;
		letter-spacing: 0.08em;
		text-transform: uppercase;
	}

	.account-summary {
		display: grid;
		grid-template-columns: repeat(3, minmax(0, 1fr));
		gap: 0.75rem;
		margin-bottom: 1.25rem;
	}

	.account-summary > div {
		display: grid;
		gap: 0.25rem;
		padding: 0.9rem;
		border: 1px solid var(--border);
		border-radius: var(--radius-sm);
		background: var(--bg-app);
	}

	.account-summary span {
		color: var(--text-muted);
		font-size: 0.72rem;
		font-weight: 800;
		text-transform: uppercase;
	}

	.account-summary strong {
		font-size: 0.86rem;
		overflow-wrap: anywhere;
	}

	.manage-section {
		display: grid;
		grid-template-columns: minmax(0, 1fr) minmax(240px, 0.75fr);
		gap: 1rem;
		align-items: center;
		padding: 1rem 0;
		border-top: 1px solid var(--border-light);
	}

	.manage-section h4 {
		font-size: 0.98rem;
	}

	.manage-section p {
		margin-top: 0.25rem;
		font-size: 0.84rem;
	}

	.manage-actions,
	.role-editor {
		display: grid;
		gap: 0.65rem;
	}

	.danger-section {
		align-items: start;
	}

	.danger-action {
		color: var(--error);
		border-color: var(--error-border);
		background: var(--error-soft);
	}

	.danger-action:hover {
		color: var(--error-strong);
		background: var(--error-soft);
	}

	@media (max-width: 820px) {
		.filter-card {
			grid-template-columns: 1fr;
		}

		.filter-actions {
			display: grid;
			grid-template-columns: 1fr;
		}

		.account-summary,
		.manage-section {
			grid-template-columns: 1fr;
		}

		.identity-cell {
			min-width: 200px;
		}
	}
</style>
