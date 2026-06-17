<script lang="ts">
	import { onMount } from 'svelte';
	import { hasAnyAuthority, readSession } from '$lib/api';
	import type { LoginData } from '$lib/types';
	import Icons from './Icons.svelte';

	let {
		authorities,
		title = 'Akses Terbatas',
		description = 'Anda tidak memiliki izin untuk melihat halaman ini atau sesi Anda telah berakhir.',
		children
	}: {
		authorities: string[];
		title?: string;
		description?: string;
		children?: import('svelte').Snippet;
	} = $props();

	let session = $state<LoginData | null>(null);

	onMount(() => {
		const sync = () => (session = readSession());
		sync();
		window.addEventListener('session-change', sync);
		window.addEventListener('storage', sync);
		return () => {
			window.removeEventListener('session-change', sync);
			window.removeEventListener('storage', sync);
		};
	});

	const allowed = $derived(hasAnyAuthority(session, authorities));
</script>

{#if allowed}
	{@render children?.()}
{:else}
	<div class="card" style="text-align: center; padding: 4rem 2rem; max-width: 600px; margin: 2rem auto;">
		<div style="background: var(--error-soft); width: 64px; height: 64px; border-radius: 50%; display: grid; place-items: center; margin: 0 auto 1.5rem; color: var(--error);">
			<Icons name="shield" size={32} />
		</div>
		<h2 style="margin-bottom: 0.5rem;">{title}</h2>
		<p style="color: var(--text-muted); margin-bottom: 2rem;">{description}</p>
		<div class="flex justify-center gap-4">
			<a class="btn btn-primary" href="/login">
				<Icons name="shield" size={18} />
				<span>Login / Ganti Sesi</span>
			</a>
			<a class="btn btn-secondary" href="/">Kembali ke Home</a>
		</div>
	</div>
{/if}
