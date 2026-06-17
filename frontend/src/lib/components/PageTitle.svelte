<script lang="ts">
	let {
		eyebrow,
		title,
		description
	}: {
		eyebrow: string;
		title: string;
		description?: string;
	} = $props();
</script>

<div class="page-header-container">
	<span class="eyebrow">{eyebrow}</span>
	<h1>{title}</h1>
	{#if description}
		<p class="description">{description}</p>
	{/if}
</div>

<style>
	.page-header-container {
		margin-bottom: 2.5rem;
		position: relative;
	}

	.eyebrow {
		display: inline-block;
		color: var(--primary);
		font-size: 0.75rem;
		font-weight: 800;
		text-transform: uppercase;
		letter-spacing: 0.1em;
		margin-bottom: 0.5rem;
		padding: 0.25rem 0.75rem;
		background: var(--primary-soft);
		border-radius: 6px;
	}

	h1 {
		margin-top: 0.25rem;
		color: var(--text-main);
	}

	.description {
		margin-top: 0.75rem;
		max-width: 800px;
		color: var(--secondary);
		font-size: 1rem;
		line-height: 1.6;
	}
</style>
