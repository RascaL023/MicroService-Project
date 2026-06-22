<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import '../app.css';
	import Icons from '$lib/components/Icons.svelte';
	import { api, clearSession, hasAnyAuthority, isAdminSession, isPublicRoute, readSession, sessionKey } from '$lib/api';
	import type { LoginData } from '$lib/types';

	let { children } = $props();
	let session = $state<LoginData | null>(null);
	let sidebarOpen = $state(false);
	let isMobile = $state(false);
	let theme = $state<'light' | 'dark'>('light');

	const themeKey = 'mcr.ui.theme';
	const isPortalRoute = $derived(
		page.url.pathname === '/app' ||
		page.url.pathname.startsWith('/app/') ||
		page.url.pathname === '/admin' ||
		page.url.pathname.startsWith('/admin/')
	);

	const navGroups = $derived.by(() => {
		const groups = [
			{
				label: 'Menu Utama',
				items: [{ href: '/', label: 'Home / Info', icon: 'home' }]
			}
		];

		if (!session) {
			groups[0].items.push({ href: '/login', label: 'Masuk / Aktivasi', icon: 'shield' });
			return groups;
		}

		// Academic Group
		const academicItems = [];
		if (hasAnyAuthority(session, ['group.read', 'group.*'])) {
			academicItems.push({ href: '/groups', label: 'Grup Belajar', icon: 'layers' });
		}
		if (isAdminSession(session) && hasAnyAuthority(session, ['group-schedule.read', 'group-schedule.*'])) {
			academicItems.push({ href: '/schedules', label: 'Jadwal Kuliah', icon: 'calendar' });
		}
		if (hasAnyAuthority(session, ['course.read', 'enrollment.read', 'enrollment.*'])) {
			academicItems.push({ href: '/enrollments', label: 'Kursus Saya', icon: 'graduationCap' });
		}
		if (hasAnyAuthority(session, ['subject.read', 'subject-material.read', 'subject-material.*', 'subject-module.read', 'subject-module.*'])) {
			academicItems.push({ href: '/materials', label: 'Materi Kursus', icon: 'book' });
		}
		if (hasAnyAuthority(session, ['major.read', 'major.*'])) {
			academicItems.push({ href: '/majors', label: 'Jurusan', icon: 'graduationCap' });
		}
		if (!isAdminSession(session) && hasAnyAuthority(session, ['user.read', 'user.*'])) {
			academicItems.push({ href: '/users', label: 'Data Peserta', icon: 'users' });
		}
		if (!isAdminSession(session) && hasAnyAuthority(session, ['batch.read', 'batch.*'])) {
			academicItems.push({ href: '/batches', label: 'Angkatan / Batch', icon: 'layers' });
		}
		if (academicItems.length) {
			groups.push({ label: 'Akademik', items: academicItems });
		}

		// Management Group (Admin only)
		if (isAdminSession(session)) {
			groups.push({
				label: 'Administrasi',
				items: [
					{ href: '/users', label: 'Data Peserta', icon: 'users' },
					{ href: '/batches', label: 'Angkatan / Batch', icon: 'layers' },
					{ href: '/majors', label: 'Jurusan', icon: 'graduationCap' },
					{ href: '/subjects', label: 'Master Subjek', icon: 'book' },
					{ href: '/auth', label: 'Keamanan Auth', icon: 'shield' }
				]
			});
		}

		return groups;
	});

	onMount(() => {
		const sync = () => (session = readSession());
		const syncTheme = () => {
			theme = readTheme();
			applyTheme(theme);
		};
		const syncViewport = () => {
			isMobile = window.innerWidth <= 1024;
			sidebarOpen = !isMobile;
		};
		sync();
		syncTheme();
		syncViewport();
		window.addEventListener('session-change', sync);
		window.addEventListener('pageshow', sync);
		window.addEventListener('storage', syncStorage);
		window.addEventListener('resize', syncViewport);
		return () => {
			window.removeEventListener('session-change', sync);
			window.removeEventListener('pageshow', sync);
			window.removeEventListener('storage', syncStorage);
			window.removeEventListener('resize', syncViewport);
		};
	});

	$effect(() => {
		if (page.url.pathname === '/login' && session) {
			void goto(isAdminSession(session) ? '/admin' : '/app', { replaceState: true });
			return;
		}

		if (!isPortalRoute && !session && !isPublicRoute(page.url.pathname)) {
			void goto('/login', { replaceState: true });
		}
	});

	async function logout() {
		try {
			await api('/api/auths/logout', { method: 'POST' });
		} finally {
			clearSession();
			session = null;
			void goto('/login', { replaceState: true });
		}
	}

	function toggleSidebar() {
		sidebarOpen = !sidebarOpen;
	}

	function closeSidebar() {
		if (isMobile) sidebarOpen = false;
	}

	function toggleTheme() {
		applyTheme(theme === 'dark' ? 'light' : 'dark');
	}

	function syncStorage(event: StorageEvent) {
		if (event.key === themeKey || event.key === null) applyTheme(readTheme());
		if (event.key === sessionKey || event.key === null) session = readSession();
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

	function pageLabel(pathname: string) {
		if (pathname === '/') return 'Beranda';
		if (pathname.startsWith('/groups/') && pathname.endsWith('/grades')) return 'Gradebook';
		if (pathname.startsWith('/groups/')) return 'Group Detail';
		if (pathname.startsWith('/groups')) return 'Groups';
		if (pathname.startsWith('/materials')) return 'Materi Kursus';
		if (pathname.startsWith('/schedules')) return 'Jadwal';
		if (pathname.startsWith('/enrollments')) return 'Kursus Saya';
		if (pathname.startsWith('/users')) return 'Data Peserta';
		if (pathname.startsWith('/batches')) return 'Batch';
		if (pathname.startsWith('/subjects')) return 'Subjects';
		if (pathname.startsWith('/auth')) return 'Auth';
		if (pathname.startsWith('/login')) return 'Login';
		if (pathname.startsWith('/activate')) return 'Aktivasi';
		if (pathname.startsWith('/reset-password')) return 'Reset Password';

		const segment = pathname.split('/').filter(Boolean)[0] ?? 'Beranda';
		return segment.charAt(0).toUpperCase() + segment.slice(1);
	}
</script>

<svelte:head>
	<title>Divdik Course</title>
</svelte:head>

{#if isPortalRoute}
	{@render children()}
{:else}
<div class="app-shell">
	<!-- Sidebar -->
	<aside class="sidebar" class:open={sidebarOpen}>
		<div class="sidebar-header">
			<a class="brand" href="/" onclick={closeSidebar}>
				<div class="brand-icon">D</div>
				<span>Divdik Portal</span>
			</a>
		</div>

		<nav class="sidebar-nav">
			{#each navGroups as group}
				<div class="nav-group">
					<div class="nav-label">{group.label}</div>
					{#each group.items as item}
						<a
							href={item.href}
							class="nav-link"
							class:active={page.url.pathname === item.href}
							onclick={closeSidebar}
						>
							<Icons name={item.icon} size={20} />
							<span>{item.label}</span>
						</a>
					{/each}
				</div>
			{/each}
		</nav>

		{#if session}
			<div style="padding: 1rem; border-top: 1px solid var(--border-light);">
				<button class="nav-link w-full" style="background: transparent; border: none; cursor: pointer; color: var(--error);" onclick={logout}>
					<Icons name="logout" size={20} />
					<span>Keluar Sistem</span>
				</button>
			</div>
		{/if}
	</aside>

	<!-- Main Content -->
	<div class="main-content" class:expanded={!sidebarOpen}>
		<header class="top-header">
			<div class="header-left">
				<button class="mobile-toggle" onclick={toggleSidebar} aria-label="Menu">
					<Icons name={sidebarOpen ? 'x' : 'menu'} size={24} />
				</button>
				<div class="breadcrumb">
					<span style="color: var(--text-muted); font-size: 0.875rem;">Divdik /</span>
					<span style="font-weight: 700; font-size: 0.875rem; margin-left: 0.4rem; color: var(--primary);">
						{pageLabel(page.url.pathname)}
					</span>
				</div>
			</div>

			<div class="header-right">
				<button
					class="theme-toggle"
					type="button"
					aria-label={theme === 'dark' ? 'Aktifkan light mode' : 'Aktifkan dark mode'}
					title={theme === 'dark' ? 'Light mode' : 'Dark mode'}
					onclick={toggleTheme}
				>
					<Icons name={theme === 'dark' ? 'sun' : 'moon'} size={19} />
				</button>

				{#if session}
					<div class="flex items-center gap-4">
						<div class="text-right">
							<div style="font-size: 0.875rem; font-weight: 700;">{session.email.split('@')[0]}</div>
							<div style="font-size: 0.75rem; color: var(--text-muted); font-weight: 600;">
								{isAdminSession(session) ? 'Administrator' : 'Sesi Pengguna'}
							</div>
						</div>
						<div
							style="width: 40px; height: 40px; background: linear-gradient(135deg, var(--primary), #60a5fa); color: var(--text-on-brand); border-radius: 12px; display: grid; place-items: center; font-weight: 700; box-shadow: var(--shadow-sm);"
						>
							{session.email[0].toUpperCase()}
						</div>
					</div>
				{:else}
					<a href="/login" class="btn btn-primary">Masuk</a>
				{/if}
			</div>
		</header>

		<main class="content-body">
			{@render children()}
		</main>
	</div>
</div>

{#if sidebarOpen && isMobile}
	<!-- svelte-ignore a11y_click_events_have_key_events -->
	<!-- svelte-ignore a11y_no_static_element_interactions -->
	<div 
		style="position: fixed; inset: 0; background: var(--bg-overlay); backdrop-filter: blur(4px); z-index: 45;" 
		onclick={closeSidebar}
	></div>
{/if}
{/if}
