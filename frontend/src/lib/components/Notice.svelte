<script lang="ts">
	import Icons from './Icons.svelte';

	let { error = '', success = '' }: { error?: string; success?: string } = $props();

	let visible = $state(false);
	let leaving = $state(false);
	let message = $state('');
	let tone = $state<'error' | 'success'>('success');
	let hideTimer: ReturnType<typeof setTimeout> | undefined;
	let leaveTimer: ReturnType<typeof setTimeout> | undefined;

	$effect(() => {
		const nextMessage = error || success;
		if (!nextMessage) return;

		if (hideTimer) clearTimeout(hideTimer);
		if (leaveTimer) clearTimeout(leaveTimer);

		message = nextMessage;
		tone = error ? 'error' : 'success';
		visible = true;
		leaving = false;

		hideTimer = setTimeout(() => {
			leaving = true;
			leaveTimer = setTimeout(() => {
				visible = false;
			}, 260);
		}, 2740);

		return () => {
			if (hideTimer) clearTimeout(hideTimer);
			if (leaveTimer) clearTimeout(leaveTimer);
		};
	});
</script>

{#if visible}
	<div class="toast-stack" aria-live="polite" aria-atomic="true">
		<div class:error={tone === 'error'} class:success={tone === 'success'} class:leaving class="toast">
			<div class="toast-icon">
				<Icons name={tone === 'error' ? 'shield' : 'home'} size={18} />
			</div>
			<div class="toast-message">{message}</div>
		</div>
	</div>
{/if}

<style>
	.toast-stack {
		position: fixed;
		top: calc(var(--header-height, 70px) + 1rem);
		right: 1.25rem;
		z-index: 200;
		display: grid;
		justify-items: end;
		pointer-events: none;
	}

	.toast {
		width: min(420px, calc(100vw - 2rem));
		display: grid;
		grid-template-columns: 38px 1fr;
		gap: 0.75rem;
		align-items: center;
		padding: 0.85rem 1rem;
		border-radius: var(--radius-sm);
		background: var(--bg-surface);
		border: 1px solid var(--border);
		box-shadow: var(--shadow-lg);
		color: var(--text-main);
		pointer-events: auto;
		animation: toast-in 220ms cubic-bezier(0.2, 0.8, 0.2, 1) both;
	}

	.toast.leaving {
		animation: toast-out 260ms cubic-bezier(0.4, 0, 0.2, 1) both;
	}

	.toast.success {
		border-color: rgba(16, 185, 129, 0.35);
	}

	.toast.error {
		border-color: rgba(244, 63, 94, 0.35);
	}

	.toast-icon {
		width: 38px;
		height: 38px;
		border-radius: 10px;
		display: grid;
		place-items: center;
	}

	.toast.success .toast-icon {
		background: rgba(16, 185, 129, 0.12);
		color: var(--success);
	}

	.toast.error .toast-icon {
		background: rgba(244, 63, 94, 0.12);
		color: var(--error);
	}

	.toast-message {
		font-size: 0.9rem;
		font-weight: 700;
		line-height: 1.35;
		overflow-wrap: anywhere;
	}

	@keyframes toast-in {
		from {
			opacity: 0;
			transform: translate3d(0, -10px, 0) scale(0.98);
		}
		to {
			opacity: 1;
			transform: translate3d(0, 0, 0) scale(1);
		}
	}

	@keyframes toast-out {
		from {
			opacity: 1;
			transform: translate3d(0, 0, 0) scale(1);
		}
		to {
			opacity: 0;
			transform: translate3d(0, -8px, 0) scale(0.98);
		}
	}

	@media (max-width: 640px) {
		.toast-stack {
			top: 1rem;
			left: 1rem;
			right: 1rem;
			justify-items: stretch;
		}

		.toast {
			width: 100%;
		}
	}
</style>
