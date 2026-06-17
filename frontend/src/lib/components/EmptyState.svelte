<script lang="ts">
	import Icons from './Icons.svelte';

	let { text = 'Data tidak ditemukan.' }: { text?: string } = $props();
</script>

<div class="empty-state-card">
	<div class="icon-container">
		<Icons name="layers" size={36} strokeWidth={1.5} />
	</div>
	<h3>{text}</h3>
	<p>Silakan gunakan filter lain atau tambahkan item baru jika Anda memiliki akses.</p>
</div>

<style>
	.empty-state-card {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 5rem 2rem;
		background: var(--bg-surface);
		border: 2px dashed var(--border);
		border-radius: var(--radius);
		text-align: center;
	}

	.icon-container {
		background: var(--bg-app);
		color: var(--text-light);
		width: 80px;
		height: 80px;
		border-radius: 50%;
		display: grid;
		place-items: center;
		margin-bottom: 1.5rem;
	}

	h3 {
		color: var(--text-main);
		font-weight: 700;
		margin-bottom: 0.5rem;
	}

	p {
		font-size: 0.875rem;
		color: var(--text-muted);
		max-width: 320px;
	}
</style>
