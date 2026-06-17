import type { Envelope, LoginData, PageData, PaginationMeta } from './types';

export const sessionKey = 'mcr.auth.session';

export function readSession(): LoginData | null {
	if (typeof localStorage === 'undefined') return null;
	const stored = localStorage.getItem(sessionKey);
	if (!stored) return null;

	try {
		return JSON.parse(stored) as LoginData;
	} catch {
		localStorage.removeItem(sessionKey);
		return null;
	}
}

export function saveSession(session: LoginData) {
	localStorage.setItem(sessionKey, JSON.stringify(session));
	window.dispatchEvent(new CustomEvent('session-change'));
}

export function clearSession() {
	localStorage.removeItem(sessionKey);
	window.dispatchEvent(new CustomEvent('session-change'));
}

export async function api<T>(path: string, init: RequestInit = {}) {
	const headers = new Headers(init.headers);
	headers.set('Accept', 'application/json');
	if (init.body && !(init.body instanceof FormData)) headers.set('Content-Type', 'application/json');

	const session = readSession();
	if (session?.sessionId) headers.set('Authorization', `Session ${session.sessionId}`);

	const response = await fetch(path, { ...init, headers });
	const payload = (await response.json().catch(() => ({}))) as Envelope<T>;
	if ((response.status === 401 || response.status === 403) && session?.sessionId) {
		clearSession();
	}
	if (!response.ok || payload.isSuccess === false) throw new Error(formatError(payload));

	return payload;
}

export function isPublicRoute(pathname: string) {
	return pathname === '/' || pathname === '/login' || pathname === '/activate' || pathname === '/reset-password';
}

export function pageItems<T>(payload: Envelope<PageData<T> | T[]>) {
	const data = payload.data;
	if (Array.isArray(data)) return data;
	if (data?.content) return data.content;
	return [];
}

export function paginationMeta<T>(
	payload: Envelope<PageData<T> | T[]>,
	fallback: PaginationMeta
): PaginationMeta {
	const pagination = payload.meta?.pagination;
	if (pagination) {
		return {
			page: Math.max((pagination.currentPage ?? 1) - 1, 0),
			size: pagination.perPage ?? fallback.size,
			totalPages: Math.max(pagination.totalPages ?? fallback.totalPages, 1),
			totalElements: pagination.totalItems ?? fallback.totalElements
		};
	}

	const data = payload.data;
	if (!data || Array.isArray(data)) {
		const total = Array.isArray(data) ? data.length : 0;
		return {
			...fallback,
			totalElements: total,
			totalPages: total === 0 ? 1 : Math.ceil(total / fallback.size)
		};
	}

	return {
		page: data.number ?? fallback.page,
		size: data.size ?? fallback.size,
		totalPages: Math.max(data.totalPages ?? fallback.totalPages, 1),
		totalElements: data.totalElements ?? fallback.totalElements
	};
}

export function formatError(payload: Envelope<unknown>) {
	if (payload.errors?.length) {
		return payload.errors.map((item) => `${item.field}: ${item.message}`).join(', ');
	}
	return payload.message || payload.errorCode || 'Request gagal.';
}

export function dayLabel(day: string) {
	const labels: Record<string, string> = {
		MONDAY: 'Senin',
		TUESDAY: 'Selasa',
		WEDNESDAY: 'Rabu',
		THURSDAY: 'Kamis',
		FRIDAY: 'Jumat',
		SATURDAY: 'Sabtu',
		SUNDAY: 'Minggu'
	};
	return labels[day] ?? day;
}

export function timeLabel(startTime: string, endTime: string) {
	return `${startTime.slice(0, 5)} - ${endTime.slice(0, 5)}`;
}

export function hasAnyAuthority(session: LoginData | null, authorities: string[]) {
	if (!session) return false;
	const owned = new Set([...(session.permissions ?? []), ...(session.roles ?? []).map((role) => `ROLE_${role}`)]);
	return authorities.some((authority) => {
		if (owned.has(authority)) return true;
		const prefix = authority.split('.')[0];
		return owned.has(`${prefix}.*`);
	});
}

export function hasAnyRole(session: LoginData | null, roles: string[]) {
	if (!session) return false;
	const owned = new Set((session.roles ?? []).map((role) => role.toUpperCase()));
	return roles.some((role) => owned.has(role.toUpperCase()));
}

export function isAdminSession(session: LoginData | null) {
	if (!session) return false;
	return hasAnyRole(session, ['ADMIN', 'SUPER_ADMIN']) ||
		hasAnyAuthority(session, ['user.*', 'batch.*', 'subject.*', 'group.*', 'enrollment.*']);
}
