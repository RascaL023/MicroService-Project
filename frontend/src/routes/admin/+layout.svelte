<script lang="ts">
	import PortalShell from '$lib/components/PortalShell.svelte';
	let { children } = $props();
</script>

<PortalShell kind="admin">
	{@render children()}
</PortalShell>
