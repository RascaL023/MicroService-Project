<script lang="ts">
	import Icons from './Icons.svelte';
	import type { PaginationMeta } from '$lib/types';

	let {
		meta,
		onPage,
		onSize,
		sizes = [10, 20, 50, 100]
	}: {
		meta: PaginationMeta;
		onPage: (page: number) => void;
		onSize: (size: number) => void;
		sizes?: number[];
	} = $props();

	const currentPage = $derived(meta.page);
	const totalPages = $derived(Math.max(meta.totalPages, 1));
	const startItem = $derived(meta.totalElements === 0 ? 0 : currentPage * meta.size + 1);
	const endItem = $derived(Math.min((currentPage + 1) * meta.size, meta.totalElements));
	const visiblePages = $derived.by(() => {
		const pages: number[] = [];
		const start = Math.max(0, currentPage - 2);
		const end = Math.min(totalPages - 1, currentPage + 2);
		for (let page = start; page <= end; page++) pages.push(page);
		return pages;
	});
</script>

<div class="pagination">
	<div class="pagination-summary">
		<span>{startItem}-{endItem}</span>
		<span>dari</span>
		<strong>{meta.totalElements}</strong>
	</div>

	<div class="pagination-controls">
		<button
			class="page-button icon-button"
			disabled={currentPage <= 0}
			type="button"
			onclick={() => onPage(currentPage - 1)}
			aria-label="Halaman sebelumnya"
		>
			<Icons name="chevronLeft" size={16} />
		</button>

		{#if visiblePages[0] > 0}
			<button class="page-button" type="button" onclick={() => onPage(0)}>1</button>
			{#if visiblePages[0] > 1}
				<span class="page-ellipsis">...</span>
			{/if}
		{/if}

		{#each visiblePages as page}
			<button
				class="page-button"
				class:active={page === currentPage}
				type="button"
				onclick={() => onPage(page)}
			>
				{page + 1}
			</button>
		{/each}

		{#if visiblePages[visiblePages.length - 1] < totalPages - 1}
			{#if visiblePages[visiblePages.length - 1] < totalPages - 2}
				<span class="page-ellipsis">...</span>
			{/if}
			<button class="page-button" type="button" onclick={() => onPage(totalPages - 1)}>{totalPages}</button>
		{/if}

		<button
			class="page-button icon-button"
			disabled={currentPage >= totalPages - 1}
			type="button"
			onclick={() => onPage(currentPage + 1)}
			aria-label="Halaman berikutnya"
		>
			<Icons name="chevronRight" size={16} />
		</button>
	</div>

	<label class="page-size">
		<span>Rows</span>
		<select
			value={meta.size}
			onchange={(event) => onSize(Number((event.currentTarget as HTMLSelectElement).value))}
		>
			{#each sizes as size}
				<option value={size}>{size}</option>
			{/each}
		</select>
	</label>
</div>

<style>
	.pagination {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 1rem;
		min-height: 62px;
		padding: 0.85rem 1rem;
		background: var(--bg-surface);
		border: 1px solid var(--border);
		border-radius: var(--radius-sm);
		box-shadow: var(--shadow-sm);
		margin-top: clamp(1.25rem, 3vh, 2rem);
		transition:
			border-color var(--motion-base, 220ms ease),
			box-shadow var(--motion-base, 220ms ease),
			transform var(--motion-base, 220ms ease);
	}

	.pagination:hover {
		border-color: color-mix(in srgb, var(--border) 72%, var(--primary) 28%);
		box-shadow: var(--shadow-md);
	}

	.pagination-summary,
	.page-size {
		display: flex;
		align-items: center;
		gap: 0.4rem;
		color: var(--text-muted);
		font-size: 0.85rem;
		font-weight: 700;
		white-space: nowrap;
	}

	.pagination-summary strong {
		color: var(--text-main);
	}

	.pagination-controls {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 0.35rem;
		min-width: 0;
	}

	.page-button {
		min-width: 34px;
		height: 34px;
		border: 1px solid var(--border);
		border-radius: var(--radius-sm);
		background: var(--bg-surface);
		color: var(--secondary);
		font-weight: 800;
		cursor: pointer;
		display: inline-grid;
		place-items: center;
		transition:
			background-color var(--motion-fast, 160ms ease),
			border-color var(--motion-fast, 160ms ease),
			color var(--motion-fast, 160ms ease),
			transform var(--motion-fast, 160ms ease);
	}

	.page-button:hover:not(:disabled) {
		border-color: var(--primary-border);
		color: var(--primary);
		background: var(--primary-soft);
		transform: translateY(-1px);
	}

	.page-button.active {
		border-color: var(--primary);
		background: var(--primary);
		color: var(--text-on-primary);
	}

	.page-button:disabled {
		opacity: 0.45;
		cursor: not-allowed;
	}

	.icon-button {
		width: 34px;
	}

	.page-ellipsis {
		color: var(--text-light);
		font-weight: 800;
		padding: 0 0.2rem;
	}

	.page-size select {
		height: 34px;
		width: 72px;
		padding: 0 0.5rem;
		min-height: 34px;
	}

	@media (max-width: 720px) {
		.pagination {
			align-items: stretch;
			flex-direction: column;
		}

		.pagination-controls {
			order: -1;
			overflow-x: auto;
			justify-content: flex-start;
			padding-bottom: 0.1rem;
		}

		.pagination-summary,
		.page-size {
			justify-content: space-between;
		}
	}
</style>
