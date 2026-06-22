<script lang="ts">
	import { onMount } from 'svelte';
	import AccessPanel from '$lib/components/AccessPanel.svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import Icons from '$lib/components/Icons.svelte';
	import Notice from '$lib/components/Notice.svelte';
	import PageTitle from '$lib/components/PageTitle.svelte';
	import Pagination from '$lib/components/Pagination.svelte';
	import { api, pageItems, paginationMeta } from '$lib/api';
	import type { AuditLog, PageData, PaginationMeta } from '$lib/types';

	let logs = $state<AuditLog[]>([]);
	let pageMeta = $state<PaginationMeta>({ page: 0, size: 12, totalPages: 1, totalElements: 0 });
	let filters = $state({
		actorUserId: '',
		action: '',
		entityType: '',
		entityId: '',
		fromTime: '',
		toTime: ''
	});
	let selectedLog = $state<AuditLog | null>(null);
	let loading = $state(true);
	let error = $state('');

	onMount(() => void load());

	async function load() {
		loading = true;
		error = '';
		try {
			const query = new URLSearchParams({
				page: String(pageMeta.page),
				size: String(pageMeta.size),
				sort: 'createdAt,desc'
			});
			if (filters.actorUserId.trim()) query.set('actorUserId', filters.actorUserId.trim());
			if (filters.action.trim()) query.set('action', filters.action.trim());
			if (filters.entityType) query.set('entityType', filters.entityType);
			if (filters.entityId.trim()) query.set('entityId', filters.entityId.trim());
			if (filters.fromTime) query.set('fromTime', toIsoDateTime(filters.fromTime));
			if (filters.toTime) query.set('toTime', toIsoDateTime(filters.toTime));

			const payload = await api<PageData<AuditLog> | AuditLog[]>(`/api/audit-logs?${query}`);
			logs = pageItems(payload);
			pageMeta = paginationMeta(payload, pageMeta);
			if (!selectedLog || !logs.some((log) => log.id === selectedLog?.id)) {
				selectedLog = logs[0] ?? null;
			}
		} catch (err) {
			error = err instanceof Error ? err.message : 'Gagal memuat audit log.';
		} finally {
			loading = false;
		}
	}

	function applyFilters() {
		pageMeta = { ...pageMeta, page: 0 };
		void load();
	}

	function resetFilters() {
		filters = { actorUserId: '', action: '', entityType: '', entityId: '', fromTime: '', toTime: '' };
		applyFilters();
	}

	function changePage(page: number) {
		pageMeta = { ...pageMeta, page };
		void load();
	}

	function changePageSize(size: number) {
		pageMeta = { ...pageMeta, page: 0, size };
		void load();
	}

	function toIsoDateTime(value: string) {
		return value.length === 16 ? `${value}:00` : value;
	}

	function timeLabel(value: string) {
		const date = new Date(value);
		if (Number.isNaN(date.getTime())) return value;

		return new Intl.DateTimeFormat('id-ID', {
			dateStyle: 'medium',
			timeStyle: 'short'
		}).format(date);
	}

	function compactAction(action: string) {
		return action.replaceAll('_', ' ');
	}

	function metadataLabel(value?: string | null) {
		if (!value) return 'Tidak ada metadata.';
		try {
			return JSON.stringify(JSON.parse(value), null, 2);
		} catch {
			return value;
		}
	}
</script>

<svelte:head><title>Audit Log - Divdik Course</title></svelte:head>

<PageTitle eyebrow="System Trace" title="Audit Log" description="Pantau perubahan domain penting yang terjadi di course-service." />

<Notice {error} />

<AccessPanel authorities={['log-access']}>
	<section class="audit-toolbar card">
		<div class="toolbar-head">
			<div>
				<span class="mini-label">Filter Log</span>
				<h2>Temukan aktivitas</h2>
			</div>
			<p>Gunakan kombinasi actor, action, entity, dan rentang waktu.</p>
		</div>

		<form class="filter-grid" onsubmit={(event) => { event.preventDefault(); applyFilters(); }}>
			<div class="filter-row primary-row">
				<label class="filter-field compact">
					<span>Actor ID</span>
					<input bind:value={filters.actorUserId} inputmode="numeric" placeholder="Contoh: 4" />
				</label>
				<label class="filter-field action-field">
					<span>Action</span>
					<input bind:value={filters.action} placeholder="GRADE, GROUP, SUBJECT..." />
				</label>
				<label class="filter-field">
					<span>Entity</span>
					<select bind:value={filters.entityType}>
						<option value="">Semua entity</option>
						<option value="ASSESSMENT">ASSESSMENT</option>
						<option value="ENROLLMENT">ENROLLMENT</option>
						<option value="GROUP">GROUP</option>
						<option value="SUBJECT">SUBJECT</option>
					</select>
				</label>
				<label class="filter-field compact">
					<span>Entity ID</span>
					<input bind:value={filters.entityId} placeholder="ID entitas" />
				</label>
			</div>

			<div class="filter-row secondary-row">
				<div class="date-fields">
					<label class="filter-field">
						<span>Dari</span>
						<input bind:value={filters.fromTime} type="datetime-local" />
					</label>
					<label class="filter-field">
						<span>Sampai</span>
						<input bind:value={filters.toTime} type="datetime-local" />
					</label>
				</div>

				<div class="filter-actions">
					<button class="btn btn-primary" type="submit">
						<Icons name="search" size={16} />
						<span>Cari</span>
					</button>
					<button class="btn btn-ghost" type="button" onclick={resetFilters}>Reset</button>
				</div>
			</div>
		</form>
	</section>

	<section class="audit-shell">
		<div class="audit-list">
			<div class="audit-list-head">
				<div>
					<span class="mini-label">Course Service</span>
					<strong>{pageMeta.totalElements} record audit</strong>
				</div>
				<small>Urutan terbaru</small>
			</div>

			{#if loading}
				<div class="audit-loading">Memuat audit log...</div>
			{:else if logs.length === 0}
				<EmptyState text="Belum ada audit log untuk filter ini." />
			{:else}
				{#each logs as log}
					<button class="audit-row" class:active={selectedLog?.id === log.id} type="button" onclick={() => selectedLog = log}>
						<div class="audit-mark">
							<Icons name="clipboardList" size={18} />
						</div>
						<div class="audit-main">
							<div class="audit-title">
								<strong>{compactAction(log.action)}</strong>
								<span class="entity-pill">{log.entityType}{log.entityId ? ` #${log.entityId}` : ''}</span>
							</div>
							<p>{log.description || 'Tidak ada deskripsi.'}</p>
							<div class="audit-meta">
								<span>{timeLabel(log.createdAt)}</span>
								<span>Actor {log.actorUserId ?? '-'}</span>
							</div>
						</div>
						<Icons name="chevronRight" size={16} />
					</button>
				{/each}
			{/if}
		</div>

		<aside class="audit-detail">
			{#if selectedLog}
				<div class="detail-head">
					<div>
						<span>Detail</span>
						<h2>{compactAction(selectedLog.action)}</h2>
					</div>
					<button class="btn btn-ghost icon-button" type="button" aria-label="Tutup detail" onclick={() => selectedLog = null}>
						<Icons name="x" size={17} />
					</button>
				</div>
				<div class="detail-grid">
					<div><span>Waktu</span><strong>{timeLabel(selectedLog.createdAt)}</strong></div>
					<div><span>Actor</span><strong>{selectedLog.actorUserId ?? '-'}</strong></div>
					<div><span>Service</span><strong>{selectedLog.service}</strong></div>
					<div><span>Entity</span><strong>{selectedLog.entityType} {selectedLog.entityId ?? ''}</strong></div>
				</div>
				<div class="detail-section">
					<span>Deskripsi</span>
					<p>{selectedLog.description || '-'}</p>
				</div>
				<div class="detail-section">
					<span>Metadata</span>
					<pre>{metadataLabel(selectedLog.metadataJson)}</pre>
				</div>
			{:else}
				<div class="detail-empty">
					<Icons name="clipboardList" size={32} />
					<strong>Pilih log</strong>
					<span>Detail metadata akan tampil di sini.</span>
				</div>
			{/if}
		</aside>
	</section>

	<Pagination meta={pageMeta} onPage={changePage} onSize={changePageSize} />
</AccessPanel>

<style>
	.audit-toolbar {
		margin-bottom: 1.25rem;
		padding: 1.1rem;
	}

	.toolbar-head {
		display: flex;
		justify-content: space-between;
		gap: 1rem;
		align-items: flex-start;
		margin-bottom: 1rem;
		padding-bottom: 0.95rem;
		border-bottom: 1px solid var(--border-light);
	}

	.toolbar-head h2 {
		margin: 0.1rem 0 0;
		font-size: 1.05rem;
		letter-spacing: -0.02em;
	}

	.toolbar-head p {
		max-width: 360px;
		margin: 0;
		color: var(--text-muted);
		font-size: 0.88rem;
		text-align: right;
	}

	.filter-grid {
		display: grid;
		gap: 1rem;
	}

	.filter-row {
		display: grid;
		gap: 0.85rem;
		align-items: end;
	}

	.primary-row {
		grid-template-columns: minmax(130px, 0.7fr) minmax(240px, 1.4fr) minmax(190px, 1fr) minmax(130px, 0.7fr);
	}

	.secondary-row {
		grid-template-columns: minmax(0, 1fr) auto;
		align-items: end;
	}

	.date-fields {
		display: grid;
		grid-template-columns: repeat(2, minmax(220px, 280px));
		gap: 0.85rem;
		justify-content: start;
	}

	.filter-field {
		display: grid;
		gap: 0.4rem;
		min-width: 0;
	}

	.filter-field.compact {
		min-width: 0;
	}

	.filter-field input,
	.filter-field select {
		width: 100%;
		min-height: 42px;
	}

	.mini-label,
	.filter-field span,
	.detail-head span,
	.detail-grid span,
	.detail-section > span {
		color: var(--text-muted);
		font-size: 0.72rem;
		font-weight: 800;
		letter-spacing: 0.04em;
		text-transform: uppercase;
	}

	.filter-actions {
		display: flex;
		justify-content: flex-end;
		gap: 0.5rem;
		white-space: nowrap;
	}

	.audit-shell {
		display: grid;
		grid-template-columns: minmax(0, 1.25fr) minmax(340px, 0.75fr);
		gap: 1.25rem;
		align-items: start;
	}

	.audit-list,
	.audit-detail {
		border: 1px solid var(--border);
		border-radius: var(--radius-sm);
		background: var(--bg-surface);
		box-shadow: var(--shadow-sm);
	}

	.audit-list {
		overflow: hidden;
	}

	.audit-list-head {
		display: flex;
		justify-content: space-between;
		gap: 1rem;
		align-items: center;
		padding: 1rem 1.1rem;
		border-bottom: 1px solid var(--border-light);
	}

	.audit-list-head > div {
		display: grid;
		gap: 0.15rem;
	}

	.audit-list-head strong {
		font-size: 1rem;
	}

	.audit-list-head small {
		color: var(--text-muted);
		font-weight: 700;
	}

	.audit-row {
		width: 100%;
		display: grid;
		grid-template-columns: auto minmax(0, 1fr) auto;
		align-items: center;
		gap: 0.95rem;
		padding: 1rem 1.1rem;
		border: 0;
		border-bottom: 1px solid var(--border-light);
		background: transparent;
		color: inherit;
		text-align: left;
		cursor: pointer;
	}

	.audit-row:hover,
	.audit-row.active {
		background: var(--bg-app);
	}

	.audit-row.active {
		box-shadow: inset 3px 0 0 var(--primary);
	}

	.audit-row:last-child {
		border-bottom: 0;
	}

	.audit-mark {
		width: 40px;
		height: 40px;
		display: grid;
		place-items: center;
		border-radius: var(--radius-sm);
		background: var(--primary-soft);
		color: var(--primary);
	}

	.audit-main {
		min-width: 0;
		display: grid;
		gap: 0.35rem;
	}

	.audit-title,
	.audit-meta {
		display: flex;
		flex-wrap: wrap;
		gap: 0.45rem 0.7rem;
		align-items: center;
	}

	.audit-title strong {
		text-transform: capitalize;
		letter-spacing: -0.01em;
	}

	.audit-title span,
	.audit-meta span {
		color: var(--text-muted);
		font-size: 0.78rem;
		font-weight: 700;
	}

	.entity-pill {
		display: inline-flex;
		align-items: center;
		min-height: 24px;
		padding: 0.18rem 0.5rem;
		border: 1px solid var(--border-light);
		border-radius: 999px;
		background: var(--bg-surface);
	}

	.audit-main p {
		margin: 0;
		color: var(--text-secondary);
		font-size: 0.9rem;
		line-height: 1.45;
		overflow-wrap: anywhere;
	}

	.audit-detail {
		position: sticky;
		top: 1rem;
		padding: 1.1rem;
	}

	.detail-head {
		display: flex;
		justify-content: space-between;
		gap: 1rem;
		align-items: flex-start;
		margin-bottom: 1rem;
	}

	.detail-head h2 {
		margin: 0.1rem 0 0;
		font-size: 1.05rem;
		text-transform: capitalize;
		letter-spacing: 0;
	}

	.icon-button {
		width: 38px;
		height: 38px;
		padding: 0;
	}

	.detail-grid {
		display: grid;
		grid-template-columns: repeat(2, minmax(0, 1fr));
		gap: 0.65rem;
		margin-bottom: 1rem;
	}

	.detail-grid > div {
		display: grid;
		gap: 0.25rem;
		padding: 0.8rem;
		border: 1px solid var(--border-light);
		border-radius: var(--radius-sm);
		background: var(--bg-app);
	}

	.detail-grid strong,
	.detail-section p,
	.detail-section pre {
		overflow-wrap: anywhere;
	}

	.detail-section {
		display: grid;
		gap: 0.45rem;
		margin-top: 0.9rem;
	}

	.detail-section p,
	.detail-section pre {
		margin: 0;
	}

	.detail-section pre {
		max-height: 300px;
		overflow: auto;
		padding: 0.95rem;
		border: 1px solid var(--border-light);
		border-radius: var(--radius-sm);
		background: var(--bg-app);
		color: var(--text-primary);
		font-size: 0.78rem;
		line-height: 1.5;
		white-space: pre-wrap;
	}

	.detail-empty,
	.audit-loading {
		display: grid;
		place-items: center;
		gap: 0.45rem;
		padding: 2rem;
		color: var(--text-muted);
		text-align: center;
	}

	@media (max-width: 1180px) {
		.primary-row {
			grid-template-columns: repeat(2, minmax(0, 1fr));
		}

		.secondary-row {
			grid-template-columns: 1fr;
		}

		.date-fields {
			grid-template-columns: repeat(2, minmax(0, 1fr));
		}
	}

	@media (max-width: 920px) {
		.audit-shell {
			grid-template-columns: 1fr;
		}

		.audit-detail {
			position: static;
		}
	}

	@media (max-width: 640px) {
		.toolbar-head {
			display: grid;
		}

		.toolbar-head p {
			text-align: left;
		}

		.primary-row,
		.secondary-row,
		.date-fields,
		.detail-grid {
			grid-template-columns: 1fr;
		}

		.filter-actions {
			display: grid;
		}
	}
</style>
