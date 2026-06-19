<script lang="ts">
	import Icons from '$lib/components/Icons.svelte';
	import { onMount } from 'svelte';
	import { isAdminSession, readSession } from '$lib/api';
	import type { LoginData } from '$lib/types';
	import { fade, fly } from 'svelte/transition';

	let session = $state<LoginData | null>(null);
	let mounted = $state(false);

	onMount(() => {
		session = readSession();
		mounted = true;
	});

	const features = [
		{ 
			title: 'Manajemen Akademik', 
			desc: 'Pengaturan subjek, modul, dan materi kursus yang terintegrasi.', 
			icon: 'book',
			color: '#3b82f6'
		},
		{ 
			title: 'Penjadwalan Cerdas', 
			desc: 'Sistem plotting jadwal group dengan validasi bentrok otomatis.', 
			icon: 'calendar',
			color: '#10b981'
		},
		{ 
			title: 'Kontrol Keamanan', 
			desc: 'Manajemen role dan permission berbasis identity-service modern.', 
			icon: 'shield',
			color: '#f43f5e'
		},
		{ 
			title: 'Multi-Role Portal', 
			desc: 'Tampilan dashboard yang menyesuaikan peran Instructor dan Learner.', 
			icon: 'users',
			color: '#f59e0b'
		}
	];
</script>

<svelte:head>
	<title>Divdik Course - Integrated Learning Management</title>
</svelte:head>

<div class="landing-page">
	{#if mounted}
		<!-- Hero Section -->
		<section class="hero" in:fade={{ duration: 400 }}>
			<div class="hero-content">
				<span class="badge badge-blue mb-4">v2.0 Production Ready</span>
				<h1>Solusi Terpadu Manajemen Kursus & Pelatihan</h1>
				<p>Platform modern untuk mengelola ekosistem akademik Anda, mulai dari administrasi user hingga monitoring progres pembelajaran dalam satu dasbor.</p>
				
				<div class="hero-actions">
					{#if session}
						<a href={isAdminSession(session) ? '/admin' : '/app'} class="btn btn-primary btn-lg">
							<span>Buka Portal</span>
							<Icons name="chevronRight" size={18} />
						</a>
					{:else}
						<a href="/login" class="btn btn-primary btn-lg">
							<span>Mulai Sekarang</span>
							<Icons name="chevronRight" size={18} />
						</a>
						<a href="#features" class="btn btn-ghost btn-lg">Pelajari Fitur</a>
					{/if}
				</div>
			</div>
			
			<div class="hero-visual" in:fly={{ x: 50, duration: 600, delay: 200 }}>
				<div class="visual-card">
					<div class="visual-header">
						<div class="dot"></div><div class="dot"></div><div class="dot"></div>
					</div>
					<div class="visual-body">
						<div class="skeleton line-w-3"></div>
						<div class="skeleton line-w-1"></div>
						<div class="grid-skeleton">
							<div class="skeleton box"></div>
							<div class="skeleton box"></div>
							<div class="skeleton box"></div>
						</div>
					</div>
				</div>
			</div>
		</section>

		<!-- Features Grid -->
		<section id="features" class="features">
			<div class="section-header">
				<h2>Fitur Unggulan Sistem</h2>
				<p>Didesain untuk skalabilitas dan kemudahan penggunaan bagi seluruh civitas akademik.</p>
			</div>

			<div class="feature-grid">
				{#each features as feature, i}
					<div class="feature-card card" in:fly={{ y: 20, duration: 400, delay: 300 + (i * 100) }}>
						<div class="feature-icon" style="background: {feature.color}15; color: {feature.color}">
							<Icons name={feature.icon} size={24} />
						</div>
						<h3>{feature.title}</h3>
						<p>{feature.desc}</p>
					</div>
				{/each}
			</div>
		</section>

		<!-- Stats / Social Proof -->
		<section class="stats">
			<div class="stat-item">
				<strong>100%</strong>
				<span>Uptime Service</span>
			</div>
			<div class="stat-divider"></div>
			<div class="stat-item">
				<strong>Go & Java</strong>
				<span>Backend Stack</span>
			</div>
			<div class="stat-divider"></div>
			<div class="stat-item">
				<strong>RESTful</strong>
				<span>API Architecture</span>
			</div>
		</section>
	{/if}
</div>

<style>
	.landing-page {
		padding-bottom: 4rem;
	}

	.hero {
		display: grid;
		grid-template-columns: 1.2fr 1fr;
		gap: 4rem;
		align-items: center;
		padding: 4rem 0;
		min-height: 500px;
	}

	.hero-content h1 {
		font-size: 3.5rem;
		margin-bottom: 1.5rem;
		background: linear-gradient(135deg, var(--text-main), var(--primary));
		background-clip: text;
		-webkit-background-clip: text;
		-webkit-text-fill-color: transparent;
	}

	.hero-content p {
		font-size: 1.125rem;
		margin-bottom: 2.5rem;
		max-width: 600px;
		line-height: 1.7;
	}

	.hero-actions {
		display: flex;
		gap: 1rem;
	}

	.btn-lg {
		padding: 1rem 2rem;
		font-size: 1rem;
	}

	/* Hero Visual / Skeleton Dashboard */
	.hero-visual {
		position: relative;
		display: flex;
		justify-content: center;
	}

	.visual-card {
		background: var(--bg-surface);
		border-radius: 16px;
		border: 1px solid var(--border);
		box-shadow: var(--shadow-lg);
		width: 100%;
		max-width: 480px;
		overflow: hidden;
	}

	.visual-header {
		background: var(--bg-table-head);
		padding: 0.75rem 1rem;
		display: flex;
		gap: 0.5rem;
	}

	.visual-header .dot {
		width: 10px;
		height: 10px;
		border-radius: 50%;
		background: var(--border);
	}

	.visual-body {
		padding: 2rem;
	}

	.skeleton {
		background: var(--border-light);
		border-radius: 4px;
		margin-bottom: 1rem;
	}

	.line-w-3 { height: 12px; width: 80%; }
	.line-w-1 { height: 12px; width: 40%; margin-bottom: 2rem; }

	.grid-skeleton {
		display: grid;
		grid-template-columns: repeat(3, 1fr);
		gap: 1rem;
	}

	.skeleton.box {
		aspect-ratio: 1;
		border-radius: 12px;
	}

	/* Features */
	.features {
		padding: 6rem 0;
	}

	.section-header {
		text-align: center;
		margin-bottom: 4rem;
	}

	.section-header h2 {
		font-size: 2.5rem;
		margin-bottom: 1rem;
	}

	.feature-grid {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
		gap: 2rem;
	}

	.feature-card {
		padding: 2.5rem;
		text-align: center;
		transition: all 0.3s ease;
	}

	.feature-card:hover {
		border-color: var(--primary-border);
		background: var(--bg-surface);
	}

	.feature-icon {
		width: 56px;
		height: 56px;
		border-radius: 14px;
		display: grid;
		place-items: center;
		margin: 0 auto 1.5rem;
	}

	.feature-card h3 {
		margin-bottom: 1rem;
		font-size: 1.25rem;
	}

	/* Stats */
	.stats {
		display: flex;
		justify-content: center;
		align-items: center;
		background: var(--bg-surface);
		padding: 3rem;
		border-radius: 24px;
		border: 1px solid var(--border);
		gap: 4rem;
	}

	.stat-item {
		text-align: center;
	}

	.stat-item strong {
		display: block;
		font-size: 2rem;
		color: var(--primary);
		margin-bottom: 0.25rem;
	}

	.stat-item span {
		font-size: 0.875rem;
		font-weight: 600;
		color: var(--text-muted);
	}

	.stat-divider {
		width: 1px;
		height: 40px;
		background: var(--border);
	}

	@media (max-width: 1024px) {
		.hero {
			grid-template-columns: 1fr;
			text-align: center;
			padding: 2rem 0;
		}

		.hero-content {
			display: flex;
			flex-direction: column;
			align-items: center;
		}

		.hero-content h1 {
			font-size: 2.75rem;
		}

		.hero-visual {
			display: none;
		}

		.stats {
			flex-direction: column;
			gap: 2rem;
		}

		.stat-divider {
			display: none;
		}
	}
</style>
