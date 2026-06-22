<script lang="ts">
	import Icons from './Icons.svelte';
	
	const footerConfig = {
		company: {
			name: 'Divdik Course',
			tagline: 'Portal Pembelajaran Terpadu',
			description: 'Sistem internal untuk mengelola peserta, kelas, jadwal, assessment, nilai, dan laporan akademik.'
		},
		sections: [
			{
				title: 'Portal',
				links: [
					{ label: 'Beranda', href: '/' },
					{ label: 'Masuk Sistem', href: '/login' },
					{ label: 'Portal Pengguna', href: '/app' },
					{ label: 'Portal Pengelola', href: '/admin' }
				]
			},
			{
				title: 'Akademik',
				links: [
					{ label: 'Group Belajar', href: '/groups' },
					{ label: 'Materi Kursus', href: '/materials' },
					{ label: 'Jadwal', href: '/schedules' },
					{ label: 'Kursus Saya', href: '/enrollments' }
				]
			},
			{
				title: 'Data Master',
				links: [
					{ label: 'Data Peserta', href: '/users' },
					{ label: 'Batch', href: '/batches' },
					{ label: 'Jurusan', href: '/majors' },
					{ label: 'Subject', href: '/subjects' }
				]
			},
			{
				title: 'Dukungan',
				links: [
					{ label: 'Kontak Admin', href: 'mailto:admin@example.com' },
					{ label: 'Panduan Sistem', href: '#features' },
					{ label: 'Laporan', href: '/admin/reports' },
					{ label: 'Audit Log', href: '/audit-logs' }
				]
			}
		],
		social: [
			{ icon: 'github', label: 'GitHub', href: 'https://github.com/RascaL023' },
			{ icon: 'instagram', label: 'Instagram', href: 'https://instagram.com' },
			{ icon: 'linkedin', label: 'LinkedIn', href: 'https://linkedin.com' },
			{ icon: 'facebook', label: 'Facebook', href: 'https://facebook.com' }
		],
		copyright: '© 2026 Divdik Course. Semua hak dilindungi.',
		location: 'Indonesia'
	};
</script>

<footer class="footer">
	<div class="footer-top">
		<div class="footer-container">
			<!-- Brand Section -->
			<div class="footer-brand">
				<div class="brand-header">
					<div class="brand-icon">D</div>
					<div>
						<h3 class="brand-name">{footerConfig.company.name}</h3>
						<p class="brand-tagline">{footerConfig.company.tagline}</p>
					</div>
				</div>
				<p class="brand-description">{footerConfig.company.description}</p>

				<!-- Social Links -->
				<div class="social-links">
					{#each footerConfig.social as social (social.label)}
						<a href={social.href} title={social.label} class="social-icon" aria-label={social.label}>
							<Icons name={social.icon} size={20} />
						</a>
					{/each}
				</div>
			</div>

			<!-- Links Section -->
			<div class="footer-links-container">
				{#each footerConfig.sections as section (section.title)}
					<div class="footer-section">
						<h4 class="section-title">{section.title}</h4>
						<ul class="section-links">
							{#each section.links as link (link.label)}
								<li>
									<a href={link.href}>{link.label}</a>
								</li>
							{/each}
						</ul>
					</div>
				{/each}
			</div>
		</div>
	</div>

	<!-- Footer Bottom -->
	<div class="footer-bottom">
		<div class="footer-container">
			<div class="footer-bottom-content">
				<p class="copyright">{footerConfig.copyright}</p>
				<div class="footer-meta">
					<span class="location">{footerConfig.location}</span>
					<span class="divider">•</span>
					<span class="updated">Portal pembelajaran internal</span>
				</div>
			</div>
		</div>
	</div>
</footer>

<style>
	.footer {
		background: var(--bg-surface);
		border-top: 1px solid var(--border-light);
		margin-top: auto;
		color: var(--text-main);
	}

	.footer-top {
		padding: 3rem 1rem;
	}

	.footer-container {
		max-width: 1280px;
		margin: 0 auto;
		display: grid;
		grid-template-columns: 1fr 3fr;
		gap: 3rem;
	}

	/* Brand Section */
	.footer-brand {
		display: flex;
		flex-direction: column;
		gap: 1.5rem;
	}

	.brand-header {
		display: flex;
		gap: 1rem;
		align-items: flex-start;
	}

	.brand-icon {
		width: 48px;
		height: 48px;
		background: linear-gradient(135deg, var(--primary), #60a5fa);
		border-radius: var(--radius-sm);
		display: grid;
		place-items: center;
		color: var(--text-on-brand);
		font-weight: 700;
		font-size: 1.25rem;
		flex-shrink: 0;
	}

	.brand-name {
		font-size: 1.125rem;
		font-weight: 700;
		margin: 0;
		color: var(--text-main);
	}

	.brand-tagline {
		font-size: 0.875rem;
		color: var(--text-muted);
		margin: 0.25rem 0 0 0;
	}

	.brand-description {
		font-size: 0.875rem;
		color: var(--text-muted);
		line-height: 1.6;
		margin: 0;
	}

	/* Social Links */
	.social-links {
		display: flex;
		gap: 0.75rem;
	}

	.social-icon {
		width: 40px;
		height: 40px;
		border-radius: var(--radius-sm);
		background: var(--bg-app);
		border: 1px solid var(--border-light);
		display: grid;
		place-items: center;
		color: var(--text-main);
		text-decoration: none;
		transition: all var(--motion-fast);
	}

	.social-icon:hover {
		background: var(--primary-soft);
		border-color: var(--primary-border);
		color: var(--primary);
		transform: translateY(-2px);
	}

	/* Links Grid */
	.footer-links-container {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
		gap: 2rem;
	}

	.footer-section {
		display: flex;
		flex-direction: column;
		gap: 1rem;
	}

	.section-title {
		font-size: 0.875rem;
		font-weight: 700;
		color: var(--text-main);
		margin: 0;
		text-transform: uppercase;
		letter-spacing: 0.05em;
	}

	.section-links {
		list-style: none;
		padding: 0;
		margin: 0;
		display: flex;
		flex-direction: column;
		gap: 0.75rem;
	}

	.section-links a {
		font-size: 0.875rem;
		color: var(--text-muted);
		text-decoration: none;
		transition: color var(--motion-fast);
	}

	.section-links a:hover {
		color: var(--primary);
	}

	/* Footer Bottom */
	.footer-bottom {
		background: var(--bg-app);
		padding: 1.5rem 1rem;
		border-top: 1px solid var(--border-light);
	}

	.footer-bottom-content {
		display: flex;
		justify-content: space-between;
		align-items: center;
		flex-wrap: wrap;
		gap: 1rem;
	}

	.copyright {
		font-size: 0.875rem;
		color: var(--text-muted);
		margin: 0;
	}

	.footer-meta {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		font-size: 0.875rem;
		color: var(--text-muted);
	}

	.location,
	.updated {
		white-space: nowrap;
	}

	.divider {
		opacity: 0.3;
	}

	/* Responsive Design */
	@media (max-width: 768px) {
		.footer-top {
			padding: 2rem 1rem;
		}

		.footer-container {
			grid-template-columns: 1fr;
			gap: 2rem;
		}

		.footer-links-container {
			grid-template-columns: repeat(2, 1fr);
			gap: 1.5rem;
		}

		.footer-bottom-content {
			flex-direction: column;
			text-align: center;
		}

		.social-links {
			justify-content: flex-start;
		}
	}

	@media (max-width: 480px) {
		.footer-top {
			padding: 1.5rem 1rem;
		}

		.footer-links-container {
			grid-template-columns: 1fr;
		}

		.social-links {
			width: 100%;
			justify-content: space-around;
		}

		.section-title {
			font-size: 0.75rem;
		}

		.section-links a {
			font-size: 0.8125rem;
		}
	}

	/* Dark Mode */
	:global([data-theme='dark']) .social-icon {
		background: var(--bg-app);
		border-color: var(--border-light);
	}

	:global([data-theme='dark']) .social-icon:hover {
		background: var(--primary-soft);
		border-color: var(--primary-border);
	}
</style>
