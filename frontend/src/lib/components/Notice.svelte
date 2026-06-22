<script lang="ts">
	import Icons from './Icons.svelte';

	let { error = '', success = '' }: { error?: string; success?: string } = $props();

	let visible = $state(false);
	let leaving = $state(false);
	let message = $state('');
	let tone = $state<'error' | 'success'>('success');
	let hideTimer: ReturnType<typeof setTimeout> | undefined;
	let leaveTimer: ReturnType<typeof setTimeout> | undefined;

	function portal(node: HTMLElement) {
		document.body.appendChild(node);

		return {
			destroy() {
				node.remove();
			}
		};
	}

	function close() {
		if (hideTimer) clearTimeout(hideTimer);
		if (leaveTimer) clearTimeout(leaveTimer);

		leaving = true;
		leaveTimer = setTimeout(() => {
			visible = false;
		}, 320);
	}

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
			}, 320);
		}, 3200);

		return () => {
			if (hideTimer) clearTimeout(hideTimer);
			if (leaveTimer) clearTimeout(leaveTimer);
		};
	});
</script>

{#if visible}
	<div class="toast-stack" aria-live="polite" aria-atomic="true" use:portal>
		<div class:error={tone === 'error'} class:success={tone === 'success'} class:leaving class="toast">
			<div class="toast-icon">
				<Icons name={tone === 'error' ? 'shield' : 'home'} size={18} />
			</div>
			<div class="toast-content">
				<span>{tone === 'error' ? 'Terjadi kendala' : 'Berhasil'}</span>
				<div class="toast-message">{message}</div>
			</div>
			<button class="toast-close" type="button" aria-label="Tutup notifikasi" onclick={close}>
				<Icons name="x" size={15} />
			</button>
			<div class="toast-progress"></div>
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
		perspective: 900px;
	}

	.toast {
		position: relative;
		overflow: hidden;
		width: min(420px, calc(100vw - 2rem));
		display: grid;
		grid-template-columns: 40px 1fr auto;
		gap: 0.75rem;
		align-items: start;
		padding: 0.95rem 1rem 1rem;
		border-radius: var(--radius-sm);
		background:
			linear-gradient(135deg, color-mix(in srgb, var(--bg-surface) 96%, var(--primary) 4%), var(--bg-surface));
		border: 1px solid var(--border);
		box-shadow: 0 18px 50px color-mix(in srgb, #020617 18%, transparent);
		color: var(--text-main);
		pointer-events: auto;
		backdrop-filter: blur(14px);
		animation: toast-in 360ms cubic-bezier(0.16, 1, 0.3, 1) both;
	}

	.toast.leaving {
		animation: toast-out 320ms cubic-bezier(0.7, 0, 0.84, 0) both;
	}

	.toast.success {
		border-color: color-mix(in srgb, var(--success) 34%, var(--border));
	}

	.toast.error {
		border-color: color-mix(in srgb, var(--error) 34%, var(--border));
	}

	.toast-icon {
		width: 40px;
		height: 40px;
		border-radius: 10px;
		display: grid;
		place-items: center;
		box-shadow: inset 0 0 0 1px color-mix(in srgb, #ffffff 22%, transparent);
	}

	.toast.success .toast-icon {
		background: rgba(16, 185, 129, 0.12);
		color: var(--success);
	}

	.toast.error .toast-icon {
		background: rgba(244, 63, 94, 0.12);
		color: var(--error);
	}

	.toast-content {
		min-width: 0;
		display: grid;
		gap: 0.15rem;
	}

	.toast-content > span {
		color: var(--text-main);
		font-size: 0.72rem;
		font-weight: 800;
		letter-spacing: 0.04em;
		text-transform: uppercase;
	}

	.toast-message {
		font-size: 0.9rem;
		font-weight: 700;
		line-height: 1.35;
		overflow-wrap: anywhere;
	}

	.toast-close {
		width: 30px;
		height: 30px;
		display: grid;
		place-items: center;
		border: 0;
		border-radius: 8px;
		background: transparent;
		color: var(--text-muted);
		cursor: pointer;
		transition:
			background-color var(--motion-fast, 160ms ease),
			color var(--motion-fast, 160ms ease),
			transform var(--motion-fast, 160ms ease);
	}

	.toast-close:hover {
		background: var(--primary-soft);
		color: var(--primary);
		transform: translateY(-1px);
	}

	.toast-progress {
		position: absolute;
		left: 0;
		right: 0;
		bottom: 0;
		height: 3px;
		transform-origin: left;
		animation: toast-progress 3200ms linear both;
	}

	.toast.success .toast-progress {
		background: linear-gradient(90deg, var(--success), color-mix(in srgb, var(--success) 35%, var(--primary)));
	}

	.toast.error .toast-progress {
		background: linear-gradient(90deg, var(--error), var(--warning));
	}

	.toast.leaving .toast-progress {
		animation-play-state: paused;
	}

	@keyframes toast-in {
		from {
			opacity: 0;
			transform: translate3d(18px, -8px, 0) scale(0.96) rotateX(-7deg);
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
			transform: translate3d(18px, -8px, 0) scale(0.96);
		}
	}

	@keyframes toast-progress {
		from {
			transform: scaleX(1);
		}

		to {
			transform: scaleX(0);
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

	@media (prefers-reduced-motion: reduce) {
		.toast,
		.toast.leaving,
		.toast-progress {
			animation-duration: 1ms;
		}
	}
</style>
