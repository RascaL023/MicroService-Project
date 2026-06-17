<script lang="ts">
	import { onMount } from 'svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import AccessPanel from '$lib/components/AccessPanel.svelte';
	import Notice from '$lib/components/Notice.svelte';
	import PageTitle from '$lib/components/PageTitle.svelte';
	import Icons from '$lib/components/Icons.svelte';
	import Pagination from '$lib/components/Pagination.svelte';
	import { api, hasAnyAuthority, pageItems, paginationMeta, readSession } from '$lib/api';
	import type { AuthUser, LoginData, PageData, PaginationMeta } from '$lib/types';

	let users = $state<AuthUser[]>([]);
	let pageMeta = $state<PaginationMeta>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
	let session = $state<LoginData | null>(null);
	let error = $state('');
	let success = $state('');
	let busy = $state('');

	const managedRoles = ['CHIEF', 'CHIEF_DEPUTY', 'CHIEF_INSTRUCTOR', 'CHIEF_DEPUTY_INSTRUCTOR'];
	const canManageRoles = $derived(hasAnyAuthority(session, ['user.*']));

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
				success = status === 'BANNED' ? 'Akun berhasil diblokir.' : 'Akun berhasil diaktifkan.';
			} finally {
				busy = '';
			}
		});
	}

	async function setManagedRole(user: AuthUser, role: string) {
		if (!role || hasRole(user, 'ADMIN')) return;
		await submit(async () => {
			busy = `role-${user.id}`;
			try {
				await api<AuthUser>(`/api/auths/users/${user.id}/role`, {
					method: 'PATCH',
					body: JSON.stringify({ role })
				});
				await load();
				success = `Role ${user.email} berhasil diubah.`;
			} finally {
				busy = '';
			}
		});
	}

	async function demoteRole(user: AuthUser) {
		if (hasRole(user, 'ADMIN')) return;
		if (!confirm(`Turunkan role ${user.email} ke USER saja?`)) return;

		await submit(async () => {
			busy = `demote-${user.id}`;
			try {
				await api<AuthUser>(`/api/auths/users/${user.id}/role`, { method: 'DELETE' });
				await load();
				success = `Role ${user.email} berhasil diturunkan ke USER.`;
			} finally {
				busy = '';
			}
		});
	}

	function hasRole(user: AuthUser, role: string) {
		return (user.roles ?? []).some((value) => value.toUpperCase() === role);
	}

	function currentManagedRole(user: AuthUser) {
		return (user.roles ?? []).find((role) => managedRoles.includes(role.toUpperCase())) ?? '';
	}

	function changePage(page: number) {
		pageMeta = { ...pageMeta, page };
		void load();
	}

	function changePageSize(size: number) {
		pageMeta = { ...pageMeta, page: 0, size };
		void load();
	}
</script>

<svelte:head><title>Auth - Divdik Course</title></svelte:head>

<PageTitle eyebrow="Security & Auth" title="Auth Identities" description="Kelola identitas autentikasi, status akun, dan penetapan role keamanan sistem." />

<Notice {error} {success} />

<AccessPanel authorities={['user.read', 'user.*']}>
	<div class="table-container">
		<table>
			<thead>
				<tr>
					<th>ID</th>
					<th>Identity Email</th>
					<th>Status</th>
					<th>Roles</th>
					{#if canManageRoles}
						<th>Adjust Role</th>
					{/if}
					<th class="text-right">Aksi</th>
				</tr>
			</thead>
			<tbody>
				{#each users as user}
					<tr>
						<td><small>#{user.id}</small></td>
						<td><strong>{user.email}</strong></td>
						<td>
							<span class="badge" class:badge-green={user.status === 'ACTIVE'} class:badge-red={user.status === 'BANNED'} class:badge-gray={user.status !== 'ACTIVE' && user.status !== 'BANNED'}>
								{user.status}
							</span>
						</td>
						<td>
							<div class="flex gap-1" style="flex-wrap: wrap;">
								{#each user.roles || [] as role}
									<span class="badge badge-blue" style="font-size: 0.65rem;">{role}</span>
								{/each}
							</div>
						</td>
						{#if canManageRoles}
							<td>
								{#if hasRole(user, 'ADMIN')}
									<span class="badge badge-gray">Protected</span>
								{:else}
									<div class="role-tools">
										<select
											value={currentManagedRole(user)}
											disabled={busy === `role-${user.id}` || busy === `demote-${user.id}`}
											onchange={(event) => void setManagedRole(user, (event.currentTarget as HTMLSelectElement).value)}
										>
											<option value="">USER only</option>
											{#each managedRoles as role}
												<option value={role}>{role}</option>
											{/each}
										</select>
										<button
											class="btn btn-ghost"
											type="button"
											disabled={!currentManagedRole(user) || busy === `demote-${user.id}`}
											onclick={() => void demoteRole(user)}
										>
											Demote
										</button>
									</div>
								{/if}
							</td>
						{/if}
						<td class="text-right">
							{#if user.status === 'BANNED'}
								<button class="btn btn-ghost" disabled={busy === `ACTIVE-${user.id}`} onclick={() => void updateStatus(user, 'ACTIVE')}>
									<Icons name="check" size={16} />
									<span>Aktifkan</span>
								</button>
							{:else}
								<button class="btn btn-ghost" style="color: var(--error);" disabled={busy === `BANNED-${user.id}`} onclick={() => void updateStatus(user, 'BANNED')}>
									<Icons name="x" size={16} />
									<span>Blokir</span>
								</button>
							{/if}
						</td>
					</tr>
				{:else}
					<tr><td colspan={canManageRoles ? 6 : 5}><EmptyState text="Belum ada identitas auth." /></td></tr>
				{/each}
			</tbody>
		</table>
	</div>
	<Pagination meta={pageMeta} onPage={changePage} onSize={changePageSize} />
</AccessPanel>

<style>
	button span {
		white-space: nowrap;
	}

	.role-tools {
		display: grid;
		grid-template-columns: minmax(180px, 1fr) auto;
		gap: 0.5rem;
		align-items: center;
		min-width: 260px;
	}

	.role-tools select {
		min-width: 0;
	}

	@media (max-width: 820px) {
		.role-tools {
			min-width: 220px;
			grid-template-columns: 1fr;
		}
	}
</style>
