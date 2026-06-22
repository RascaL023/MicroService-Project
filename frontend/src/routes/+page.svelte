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
			title: 'Akademik Terpusat',
			desc: 'Kelola subject, materi, modul, group, dan jadwal dari satu portal yang konsisten.',
			icon: 'book',
			color: '#3b82f6'
		},
		{
			title: 'Kelas Lebih Tertata',
			desc: 'Enrollment instructor dan learner dibuat rapi, termasuk validasi status peserta.',
			icon: 'users',
			color: '#10b981'
		},
		{
			title: 'Nilai dan Laporan',
			desc: 'Assessment, gradebook, rekap mingguan, dan export Excel tersedia untuk kebutuhan akademik.',
			icon: 'barChart',
			color: '#f59e0b'
		},
		{
			title: 'Akses Berbasis Authority',
			desc: 'Menu dan tombol aksi mengikuti permission user, sementara validasi final tetap di backend.',
			icon: 'shield',
			color: '#f43f5e'
		}
	];

	const highlights = [
		{ label: 'Portal', value: '2', note: 'Pengguna dan pengelola' },
		{ label: 'Laporan', value: 'Excel', note: 'Rekap nilai mingguan' },
		{ label: 'Akses', value: 'RBAC', note: 'Role dan authority' }
	];

	const workflow = [
		{ title: 'Admin siapkan data', desc: 'Batch, jurusan, user, subject, dan group dibuat lebih terstruktur.' },
		{ title: 'Instruktur mengajar', desc: 'Jadwal, pertemuan, assessment, dan nilai dikelola dari detail group.' },
		{ title: 'Peserta mengikuti kelas', desc: 'Materi, group, jadwal, dan nilai dapat diakses dari portal pengguna.' }
	];

	const socialLinks = [
		{ icon: 'instagram', label: 'Instagram', href: 'https://instagram.com' },
		{ icon: 'github', label: 'GitHub', href: 'https://github.com/RascaL023' },
		{ icon: 'linkedin', label: 'LinkedIn', href: 'https://linkedin.com' },
		{ icon: 'facebook', label: 'Facebook', href: 'https://facebook.com' }
	];
</script>

<svelte:head>
	<title>Divdik Course - Portal Pembelajaran Terpadu</title>
</svelte:head>

<div class="landing-page">
	{#if mounted}
		<section class="hero" in:fade={{ duration: 360 }}>
			<div class="hero-content">
				<span class="eyebrow">Portal Pembelajaran Divisi Pendidikan</span>
				<h1>Kelola pembelajaran, kelas, dan nilai dalam satu sistem.</h1>
				<p>
					Divdik Course membantu pengelola, instruktur, dan peserta bekerja dari alur yang sama:
					data user rapi, group jelas, jadwal terpantau, assessment terdokumentasi, dan laporan nilai siap diunduh.
				</p>

				<div class="hero-actions">
					{#if session}
						<a href={isAdminSession(session) ? '/admin' : '/app'} class="btn btn-primary btn-lg">
							<span>Buka Portal</span>
							<Icons name="chevronRight" size={18} />
						</a>
					{:else}
						<a href="/login" class="btn btn-primary btn-lg">
							<span>Masuk ke Sistem</span>
							<Icons name="chevronRight" size={18} />
						</a>
						<a href="#features" class="btn btn-ghost btn-lg">Lihat Fitur</a>
					{/if}
				</div>

				<div class="hero-social" aria-label="Media sosial">
					<span>Ikuti update</span>
					{#each socialLinks as social}
						<a href={social.href} aria-label={social.label} title={social.label}>
							<Icons name={social.icon} size={18} />
						</a>
					{/each}
				</div>
			</div>

			<div class="hero-visual" in:fly={{ x: 36, duration: 520, delay: 120 }}>
				<div class="dashboard-preview">
					<div class="preview-top">
						<div>
							<span>Ringkasan Hari Ini</span>
							<strong>Operasional Akademik</strong>
						</div>
						<div class="preview-status">Aktif</div>
					</div>

					<div class="preview-grid">
						<div class="preview-metric">
							<Icons name="users" size={20} />
							<strong>128</strong>
							<span>Peserta</span>
						</div>
						<div class="preview-metric">
							<Icons name="layers" size={20} />
							<strong>12</strong>
							<span>Group</span>
						</div>
						<div class="preview-metric">
							<Icons name="bell" size={20} />
							<strong>7</strong>
							<span>Reminder</span>
						</div>
					</div>

					<div class="activity-list">
						<div class="activity-item">
							<div class="activity-icon blue"><Icons name="calendar" size={16} /></div>
							<div>
								<strong>Jadwal Logika Algoritma</strong>
								<span>Selasa, 13.00 - 15.00</span>
							</div>
						</div>
						<div class="activity-item">
							<div class="activity-icon green"><Icons name="checkCircle" size={16} /></div>
							<div>
								<strong>Nilai Tugas 2 diperbarui</strong>
								<span>Gradebook siap direkap</span>
							</div>
						</div>
						<div class="activity-item">
							<div class="activity-icon amber"><Icons name="award" size={16} /></div>
							<div>
								<strong>Laporan mingguan</strong>
								<span>Export Excel berdasarkan subject</span>
							</div>
						</div>
					</div>
				</div>
			</div>
		</section>

		<section class="highlights" aria-label="Ringkasan sistem">
			{#each highlights as item, i}
				<div class="highlight-item" in:fly={{ y: 12, duration: 360, delay: 140 + i * 80 }}>
					<span>{item.label}</span>
					<strong>{item.value}</strong>
					<p>{item.note}</p>
				</div>
			{/each}
		</section>

		<section id="features" class="features">
			<div class="section-header">
				<span class="eyebrow">Fitur Utama</span>
				<h2>Disusun untuk kerja akademik harian.</h2>
				<p>Antarmuka dibuat padat dan jelas agar admin maupun instruktur bisa bergerak cepat tanpa kehilangan konteks.</p>
			</div>

			<div class="feature-grid">
				{#each features as feature, i}
					<div class="feature-card" in:fly={{ y: 18, duration: 360, delay: 220 + i * 80 }}>
						<div class="feature-icon" style={`--accent: ${feature.color}`}>
							<Icons name={feature.icon} size={23} />
						</div>
						<h3>{feature.title}</h3>
						<p>{feature.desc}</p>
					</div>
				{/each}
			</div>
		</section>

		<section class="workflow">
			<div class="section-header compact">
				<span class="eyebrow">Alur Kerja</span>
				<h2>Dari data awal sampai laporan nilai.</h2>
			</div>

			<div class="workflow-list">
				{#each workflow as step, i}
					<div class="workflow-item" in:fly={{ y: 18, duration: 360, delay: 260 + i * 90 }}>
						<div class="step-number">{i + 1}</div>
						<div>
							<h3>{step.title}</h3>
							<p>{step.desc}</p>
						</div>
					</div>
				{/each}
			</div>
		</section>

		<section class="contact-strip">
			<div>
				<span class="eyebrow">Terhubung</span>
				<h2>Butuh akses atau ingin cek perkembangan sistem?</h2>
				<p>Hubungi pengelola Divdik atau ikuti kanal sosial untuk update fitur dan informasi operasional.</p>
			</div>
			<div class="social-panel">
				{#each socialLinks as social}
					<a href={social.href} aria-label={social.label} title={social.label}>
						<Icons name={social.icon} size={20} />
						<span>{social.label}</span>
					</a>
				{/each}
			</div>
		</section>
	{/if}
</div>

<style>
	.landing-page {
		display: grid;
		gap: 4.5rem;
		padding-bottom: 4rem;
	}

	.hero {
		display: grid;
		grid-template-columns: minmax(0, 1.05fr) minmax(360px, 0.95fr);
		gap: 3rem;
		align-items: center;
		padding: 4rem 0 1rem;
		min-height: 560px;
	}

	.eyebrow {
		display: inline-flex;
		width: fit-content;
		align-items: center;
		border: 1px solid var(--primary-border);
		border-radius: 999px;
		background: var(--primary-soft);
		color: var(--primary);
		padding: 0.45rem 0.75rem;
		font-size: 0.72rem;
		font-weight: 800;
		letter-spacing: 0.08em;
		text-transform: uppercase;
	}

	.hero-content {
		display: grid;
		gap: 1.4rem;
	}

	.hero-content h1 {
		max-width: 760px;
		font-size: clamp(2.6rem, 6vw, 4.9rem);
		line-height: 1.03;
		letter-spacing: 0;
	}

	.hero-content p {
		max-width: 680px;
		font-size: 1.05rem;
		line-height: 1.8;
		color: var(--text-muted);
	}

	.hero-actions,
	.hero-social,
	.social-panel {
		display: flex;
		align-items: center;
		gap: 0.85rem;
		flex-wrap: wrap;
	}

	.btn-lg {
		min-height: 48px;
		padding: 0.85rem 1.3rem;
		font-size: 0.95rem;
	}

	.hero-social {
		margin-top: 0.3rem;
		color: var(--text-muted);
		font-size: 0.85rem;
		font-weight: 700;
	}

	.hero-social a,
	.social-panel a {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		text-decoration: none;
		color: var(--text-main);
		border: 1px solid var(--border-light);
		background: var(--bg-surface);
		transition:
			transform var(--motion-base),
			border-color var(--motion-base),
			background-color var(--motion-base),
			color var(--motion-base);
	}

	.hero-social a {
		width: 38px;
		height: 38px;
		border-radius: 10px;
	}

	.hero-social a:hover,
	.social-panel a:hover {
		transform: translateY(-2px);
		border-color: var(--primary-border);
		background: var(--primary-soft);
		color: var(--primary);
	}

	.hero-visual {
		display: flex;
		justify-content: flex-end;
	}

	.dashboard-preview {
		width: min(100%, 520px);
		border: 1px solid var(--border);
		border-radius: 18px;
		background:
			linear-gradient(180deg, color-mix(in srgb, var(--primary) 9%, transparent), transparent 38%),
			var(--bg-surface);
		box-shadow: var(--shadow-lg);
		padding: 1rem;
	}

	.preview-top {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 1rem;
		padding: 0.55rem 0.55rem 1rem;
	}

	.preview-top span,
	.preview-metric span,
	.activity-item span {
		display: block;
		color: var(--text-muted);
		font-size: 0.78rem;
		font-weight: 700;
	}

	.preview-top strong {
		display: block;
		margin-top: 0.2rem;
		font-size: 1.05rem;
	}

	.preview-status {
		border: 1px solid var(--success-border);
		border-radius: 999px;
		background: var(--success-soft);
		color: var(--success-text);
		padding: 0.35rem 0.7rem;
		font-size: 0.78rem;
		font-weight: 800;
	}

	.preview-grid {
		display: grid;
		grid-template-columns: repeat(3, minmax(0, 1fr));
		gap: 0.75rem;
	}

	.preview-metric {
		display: grid;
		gap: 0.45rem;
		min-height: 116px;
		border: 1px solid var(--border-light);
		border-radius: 14px;
		background: color-mix(in srgb, var(--bg-surface) 84%, var(--primary-soft));
		padding: 0.9rem;
	}

	.preview-metric :global(svg) {
		color: var(--primary);
	}

	.preview-metric strong {
		font-size: 1.65rem;
		line-height: 1;
	}

	.activity-list {
		display: grid;
		gap: 0.75rem;
		margin-top: 1rem;
	}

	.activity-item {
		display: grid;
		grid-template-columns: auto minmax(0, 1fr);
		gap: 0.85rem;
		align-items: center;
		border: 1px solid var(--border-light);
		border-radius: 14px;
		background: var(--bg-app);
		padding: 0.85rem;
	}

	.activity-item strong {
		display: block;
		font-size: 0.9rem;
		margin-bottom: 0.1rem;
	}

	.activity-icon {
		width: 38px;
		height: 38px;
		border-radius: 11px;
		display: grid;
		place-items: center;
	}

	.activity-icon.blue { color: #3b82f6; background: rgb(59 130 246 / 0.12); }
	.activity-icon.green { color: #10b981; background: rgb(16 185 129 / 0.12); }
	.activity-icon.amber { color: #f59e0b; background: rgb(245 158 11 / 0.12); }

	.highlights {
		display: grid;
		grid-template-columns: repeat(3, minmax(0, 1fr));
		gap: 1rem;
	}

	.highlight-item,
	.feature-card,
	.workflow-item,
	.contact-strip {
		border: 1px solid var(--border);
		background: var(--bg-surface);
		box-shadow: var(--shadow-sm);
	}

	.highlight-item {
		border-radius: 14px;
		padding: 1.1rem;
	}

	.highlight-item span {
		color: var(--text-muted);
		font-size: 0.75rem;
		font-weight: 800;
		text-transform: uppercase;
	}

	.highlight-item strong {
		display: block;
		margin: 0.35rem 0 0.25rem;
		font-size: 1.8rem;
		color: var(--primary);
	}

	.highlight-item p {
		margin: 0;
		color: var(--text-muted);
		font-size: 0.86rem;
	}

	.features,
	.workflow {
		display: grid;
		gap: 2rem;
	}

	.section-header {
		max-width: 720px;
		display: grid;
		gap: 0.85rem;
	}

	.section-header.compact {
		max-width: 620px;
	}

	.section-header h2 {
		font-size: clamp(2rem, 4vw, 3rem);
		letter-spacing: 0;
	}

	.section-header p {
		color: var(--text-muted);
		line-height: 1.75;
	}

	.feature-grid {
		display: grid;
		grid-template-columns: repeat(4, minmax(0, 1fr));
		gap: 1rem;
	}

	.feature-card {
		border-radius: 16px;
		padding: 1.25rem;
		transition:
			transform var(--motion-base),
			border-color var(--motion-base),
			box-shadow var(--motion-base);
	}

	.feature-card:hover {
		transform: translateY(-3px);
		border-color: var(--primary-border);
		box-shadow: var(--shadow-md);
	}

	.feature-icon {
		width: 48px;
		height: 48px;
		border-radius: 13px;
		display: grid;
		place-items: center;
		margin-bottom: 1rem;
		background: color-mix(in srgb, var(--accent) 14%, transparent);
		color: var(--accent);
	}

	.feature-card h3 {
		font-size: 1.02rem;
		margin-bottom: 0.55rem;
	}

	.feature-card p {
		color: var(--text-muted);
		line-height: 1.65;
	}

	.workflow-list {
		display: grid;
		grid-template-columns: repeat(3, minmax(0, 1fr));
		gap: 1rem;
	}

	.workflow-item {
		display: grid;
		grid-template-columns: auto minmax(0, 1fr);
		gap: 1rem;
		border-radius: 16px;
		padding: 1.2rem;
	}

	.step-number {
		width: 38px;
		height: 38px;
		border-radius: 12px;
		display: grid;
		place-items: center;
		background: var(--primary-soft);
		color: var(--primary);
		font-weight: 900;
	}

	.workflow-item h3 {
		font-size: 1rem;
		margin-bottom: 0.35rem;
	}

	.workflow-item p {
		color: var(--text-muted);
		line-height: 1.65;
	}

	.contact-strip {
		display: grid;
		grid-template-columns: minmax(0, 1fr) auto;
		gap: 2rem;
		align-items: center;
		border-radius: 18px;
		padding: 1.5rem;
	}

	.contact-strip h2 {
		margin: 0.8rem 0 0.45rem;
		font-size: clamp(1.45rem, 3vw, 2.1rem);
		letter-spacing: 0;
	}

	.contact-strip p {
		color: var(--text-muted);
	}

	.social-panel a {
		gap: 0.55rem;
		min-height: 44px;
		border-radius: 12px;
		padding: 0.7rem 0.9rem;
		font-size: 0.86rem;
		font-weight: 800;
	}

	@media (max-width: 1180px) {
		.hero,
		.contact-strip {
			grid-template-columns: 1fr;
		}

		.hero-visual {
			justify-content: flex-start;
		}

		.feature-grid {
			grid-template-columns: repeat(2, minmax(0, 1fr));
		}
	}

	@media (max-width: 760px) {
		.landing-page {
			gap: 3rem;
		}

		.hero {
			padding-top: 2.2rem;
			min-height: auto;
		}

		.hero-actions {
			align-items: stretch;
		}

		.hero-actions a {
			width: 100%;
			justify-content: center;
		}

		.preview-grid,
		.highlights,
		.feature-grid,
		.workflow-list {
			grid-template-columns: 1fr;
		}

		.contact-strip {
			padding: 1.1rem;
		}

		.social-panel a {
			flex: 1 1 150px;
			justify-content: center;
		}
	}
</style>
