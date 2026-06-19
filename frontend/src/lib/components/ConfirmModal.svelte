<script lang="ts">
	import Icons from './Icons.svelte';

	type Tone = 'danger' | 'primary';

	let {
		open = false,
		title = 'Konfirmasi',
		message = 'Lanjutkan aksi ini?',
		confirmLabel = 'Ya, lanjutkan',
		cancelLabel = 'Batal',
		tone = 'danger',
		onConfirm = () => {},
		onCancel = () => {}
	}: {
		open?: boolean;
		title?: string;
		message?: string;
		confirmLabel?: string;
		cancelLabel?: string;
		tone?: Tone;
		onConfirm?: () => void;
		onCancel?: () => void;
	} = $props();
</script>

{#if open}
	<div class="modal-backdrop" role="presentation" onclick={onCancel}>
		<section
			class="modal-panel confirm-panel"
			role="dialog"
			aria-modal="true"
			tabindex="-1"
			onclick={(event) => event.stopPropagation()}
			onkeydown={(event) => {
				if (event.key === 'Escape') onCancel();
				event.stopPropagation();
			}}
		>
			<div class="confirm-body">
				<div class="confirm-icon" class:primary={tone === 'primary'}>
					<Icons name={tone === 'danger' ? 'alertTriangle' : 'checkCircle'} size={24} />
				</div>
				<div>
					<h3>{title}</h3>
					<p>{message}</p>
				</div>
			</div>

			<div class="modal-actions">
				<button class="btn btn-ghost" type="button" onclick={onCancel}>{cancelLabel}</button>
				<button class="btn" class:btn-primary={tone === 'primary'} class:btn-danger={tone === 'danger'} type="button" onclick={onConfirm}>
					{confirmLabel}
				</button>
			</div>
		</section>
	</div>
{/if}

<style>
	.confirm-panel {
		width: min(440px, 100%);
	}

	.confirm-body {
		display: grid;
		grid-template-columns: auto minmax(0, 1fr);
		gap: 1rem;
		align-items: flex-start;
	}

	.confirm-body h3 {
		font-size: 1.1rem;
	}

	.confirm-body p {
		margin-top: 0.35rem;
	}

	.confirm-icon {
		width: 48px;
		height: 48px;
		display: grid;
		place-items: center;
		border-radius: 14px;
		background: var(--error-soft);
		color: var(--error);
		border: 1px solid var(--error-border);
	}

	.confirm-icon.primary {
		background: var(--primary-soft);
		color: var(--primary);
		border-color: var(--primary-border);
	}

	.btn-danger {
		background: var(--error);
		color: white;
		box-shadow: 0 4px 10px rgb(244 63 94 / 0.2);
	}

	.btn-danger:hover {
		filter: brightness(0.95);
		transform: translateY(-1px);
	}

	@media (max-width: 560px) {
		.confirm-body {
			grid-template-columns: 1fr;
		}

		.modal-actions {
			display: grid;
		}
	}
</style>
