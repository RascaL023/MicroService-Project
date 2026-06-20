<script lang="ts">
	import type { Snippet } from 'svelte';
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import Icons from './Icons.svelte';
	import {
		api,
		clearSession,
		hasAnyAuthority,
		isAdminSession,
		readSession,
		sessionKey
	} from '$lib/api';
	import type { LoginData } from '$lib/types';

	type PortalKind = 'app' | 'admin';
	type NavItem = {
		href: string;
		label: string;
		icon: string;
		authorities?: string[];
	};

	let {
		kind,
		children
	}: {
		kind: PortalKind;
		children: Snippet;
	} = $props();

	let session = $state<LoginData | null>(null);
	let ready = $state(false);
	let sidebarOpen = $state(false);
	let isMobile = $state(false);
	let theme = $state<'light' | 'dark'>('light');

	const themeKey = 'mcr.ui.theme';
	const isAdminPortal = $derived(kind === 'admin');
	const portalLabel = $derived(isAdminPortal ? 'Portal Pengelola' : 'Portal Pengguna');
	const portalHome = $derived(isAdminPortal ? '/admin' : '/app');
	const alternatePortal = $derived(isAdminPortal ? '/app' : '/admin');
	const alternateLabel = $derived(isAdminPortal ? 'Portal Pengguna' : 'Portal Pengelola');

	const appNavigation: { label: string; items: NavItem[] }[] = [
		{
			label: 'Ringkasan',
			items: [{ href: '/app', label: 'Dashboard', icon: 'home' }]
		},
		{
			label: 'Pembelajaran',
			items: [
				{ href: '/app/groups', label: 'Group', icon: 'layers', authorities: ['group.read', 'group.*'] },
				{ href: '/app/enrollments', label: 'Kursus Saya', icon: 'graduationCap', authorities: ['enrollment.read', 'enrollment.*'] },
				{
					href: '/app/materials',
					label: 'Materi',
					icon: 'book',
					authorities: ['subject.read', 'subject-material.read', 'subject-module.read', 'subject.*']
				},
				{ href: '/app/majors', label: 'Jurusan', icon: 'graduationCap', authorities: ['major.read', 'major.*'] }
			]
		}
	];

	const adminNavigation: { label: string; items: NavItem[] }[] = [
		{
			label: 'Ringkasan',
			items: [{ href: '/admin', label: 'Dashboard', icon: 'home' }]
		},
		{
			label: 'Akademik',
			items: [
				{ href: '/admin/groups', label: 'Group', icon: 'layers', authorities: ['group.read', 'group.*'] },
				{ href: '/admin/schedules', label: 'Jadwal', icon: 'calendar', authorities: ['group-schedule.read', 'group-schedule.*'] },
				{ href: '/admin/enrollments', label: 'Enrollment', icon: 'graduationCap', authorities: ['enrollment.read', 'enrollment.*'] },
				{
					href: '/admin/materials',
					label: 'Materi & Modul',
					icon: 'book',
					authorities: ['subject-material.read', 'subject-module.read', 'subject.*']
				},
				{ href: '/admin/reports', label: 'Laporan', icon: 'download', authorities: ['subject.read', 'subject.*'] }
			]
		},
		{
			label: 'Administrasi',
			items: [
				{ href: '/admin/users', label: 'User', icon: 'users', authorities: ['user.read', 'user.*'] },
				{ href: '/admin/batches', label: 'Batch', icon: 'layers', authorities: ['batch.read', 'batch.*'] },
				{ href: '/admin/majors', label: 'Jurusan', icon: 'graduationCap', authorities: ['major.read', 'major.*'] },
				{ href: '/admin/subjects', label: 'Subject', icon: 'book', authorities: ['subject.read', 'subject.*'] },
				{ href: '/admin/auth', label: 'Role & Auth', icon: 'shield', authorities: ['user.*'] }
			]
		}
	];

	const navigation = $derived(isAdminPortal ? adminNavigation : appNavigation);
	const visibleNavigation = $derived(
		navigation
			.map((group) => ({
				...group,
				items: group.items.filter((item) => !item.authorities || hasAnyAuthority(session, item.authorities))
			}))
			.filter((group) => group.items.length > 0)
	);

	onMount(() => {
		const syncSession = () => {
			session = readSession();
			if (!session) {
				void goto('/login', { replaceState: true });
				return;
			}
			if (isAdminPortal && !isAdminSession(session)) {
				void goto('/app', { replaceState: true });
				return;
			}
			ready = true;
		};
		const syncViewport = () => {
			isMobile = window.innerWidth <= 1024;
			sidebarOpen = !isMobile;
		};
		const syncTheme = () => applyTheme(readTheme());
		const syncStorage = (event: StorageEvent) => {
			if (event.key === sessionKey || event.key === null) syncSession();
			if (event.key === themeKey || event.key === null) syncTheme();
		};

		syncSession();
		syncViewport();
		syncTheme();
		window.addEventListener('session-change', syncSession);
		window.addEventListener('storage', syncStorage);
		window.addEventListener('resize', syncViewport);
		return () => {
			window.removeEventListener('session-change', syncSession);
			window.removeEventListener('storage', syncStorage);
			window.removeEventListener('resize', syncViewport);
		};
	});

	function isActive(href: string) {
		return href === portalHome
			? page.url.pathname === href
			: page.url.pathname === href || page.url.pathname.startsWith(`${href}/`);
	}

	function pageLabel() {
		const item = navigation.flatMap((group) => group.items)
			.filter((candidate) => isActive(candidate.href))
			.sort((a, b) => b.href.length - a.href.length)[0];
		if (item) return item.label;
		if (page.url.pathname.includes('/grades')) return 'Gradebook';
		return portalLabel;
	}

	function closeSidebar() {
		if (isMobile) sidebarOpen = false;
	}

	function readTheme(): 'light' | 'dark' {
		const stored = localStorage.getItem(themeKey);
		if (stored === 'light' || stored === 'dark') return stored;
		return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
	}

	function applyTheme(nextTheme: 'light' | 'dark') {
		theme = nextTheme;
		document.documentElement.dataset.theme = nextTheme;
		document.documentElement.style.colorScheme = nextTheme;
		localStorage.setItem(themeKey, nextTheme);
	}

	async function logout() {
		try {
			await api('/api/auths/logout', { method: 'POST' });
		} finally {
			clearSession();
			session = null;
			void goto('/login', { replaceState: true });
		}
	}
</script>

{#if ready && session}
	<div class="app-shell portal-shell" class:admin-portal={isAdminPortal}>
		<aside class="sidebar" class:open={sidebarOpen}>
			<div class="sidebar-header portal-brand">
				<a class="brand" href={portalHome} onclick={closeSidebar}>
					<div class="brand-icon">D</div>
					<div>
						<strong>Divdik</strong>
						<span>{portalLabel}</span>
					</div>
				</a>
			</div>

			<nav class="sidebar-nav">
				{#each visibleNavigation as group}
					<div class="nav-group">
						<div class="nav-label">{group.label}</div>
						{#each group.items as item}
							<a
								href={item.href}
								class="nav-link"
								class:active={isActive(item.href)}
								onclick={closeSidebar}
							>
								<Icons name={item.icon} size={19} />
								<span>{item.label}</span>
							</a>
						{/each}
					</div>
				{/each}
			</nav>

			<div class="portal-sidebar-footer">
				{#if isAdminSession(session)}
					<a class="portal-switch" href={alternatePortal}>
						<Icons name={isAdminPortal ? 'graduationCap' : 'shield'} size={18} />
						<span>{alternateLabel}</span>
						<Icons name="chevronRight" size={15} />
					</a>
				{/if}
				<button class="nav-link logout-button" type="button" onclick={logout}>
					<Icons name="logout" size={19} />
					<span>Keluar</span>
				</button>
			</div>
		</aside>

		<div class="main-content" class:expanded={!sidebarOpen}>
			<header class="top-header">
				<div class="header-left">
					<button class="mobile-toggle" type="button" onclick={() => sidebarOpen = !sidebarOpen} aria-label="Menu">
						<Icons name={sidebarOpen ? 'x' : 'menu'} size={22} />
					</button>
					<div class="portal-context">
						<span>{portalLabel}</span>
						<strong>{pageLabel()}</strong>
					</div>
				</div>

				<div class="header-right">
					<button
						class="theme-toggle"
						type="button"
						onclick={() => applyTheme(theme === 'dark' ? 'light' : 'dark')}
						aria-label="Ganti tema"
					>
						<Icons name={theme === 'dark' ? 'sun' : 'moon'} size={18} />
					</button>
					<div class="portal-user">
						<div>
							<strong>{session.email?.split('@')[0] || 'Pengguna'}</strong>
							<span>{session.roles?.join(', ') || 'USER'}</span>
						</div>
						<div class="user-avatar">{(session.email?.[0] || 'U').toUpperCase()}</div>
					</div>
				</div>
			</header>

			<main class="content-body portal-content">
				{@render children()}
			</main>
		</div>
	</div>

	{#if sidebarOpen && isMobile}
		<button class="sidebar-overlay" type="button" onclick={closeSidebar} aria-label="Tutup menu"></button>
	{/if}
{/if}

<style>
	.portal-brand {
		border-bottom: 1px solid var(--border-light);
	}

	.portal-brand .brand > div:last-child {
		display: grid;
		line-height: 1.2;
	}

	.portal-brand strong {
		color: var(--text-main);
		font-size: 1rem;
	}

	.portal-brand span {
		color: var(--text-muted);
		font-size: 0.72rem;
		font-weight: 700;
	}

	.portal-sidebar-footer {
		display: grid;
		gap: 0.5rem;
		padding: 1rem;
		border-top: 1px solid var(--border-light);
	}

	.portal-switch {
		display: grid;
		grid-template-columns: auto 1fr auto;
		align-items: center;
		gap: 0.7rem;
		padding: 0.75rem;
		border: 1px solid var(--primary-border);
		border-radius: var(--radius-sm);
		background: var(--primary-soft);
		color: var(--primary);
		text-decoration: none;
		font-size: 0.82rem;
		font-weight: 800;
	}

	.logout-button {
		width: 100%;
		border: 0;
		background: transparent;
		color: var(--error);
		cursor: pointer;
	}

	.portal-context {
		display: grid;
		line-height: 1.25;
	}

	.portal-context span,
	.portal-user span {
		color: var(--text-muted);
		font-size: 0.72rem;
		font-weight: 700;
	}

	.portal-context strong {
		font-size: 0.92rem;
	}

	.portal-user {
		display: flex;
		align-items: center;
		gap: 0.75rem;
	}

	.portal-user > div:first-child {
		display: grid;
		text-align: right;
		line-height: 1.25;
	}

	.portal-user strong {
		font-size: 0.82rem;
	}

	.user-avatar {
		width: 38px;
		height: 38px;
		display: grid;
		place-items: center;
		border-radius: var(--radius-sm);
		background: var(--primary);
		color: var(--text-on-primary);
		font-weight: 800;
	}

	.sidebar-overlay {
		position: fixed;
		inset: 0;
		z-index: 45;
		border: 0;
		background: var(--bg-overlay);
		backdrop-filter: blur(3px);
	}

	@media (max-width: 640px) {
		.portal-user > div:first-child {
			display: none;
		}

		.portal-content {
			padding: 1rem;
		}
	}
</style>
