export type Envelope<T> = {
	isSuccess?: boolean;
	message?: string;
	data?: T;
	meta?: {
		pagination?: {
			currentPage?: number;
			perPage?: number;
			totalItems?: number;
			totalPages?: number;
			hasNextPage?: boolean;
			hasPrevPage?: boolean;
		};
		timestamp?: string;
	} & Record<string, unknown>;
	errors?: { field: string; message: string }[];
	errorCode?: string;
};

export type PageData<T> = {
	content?: T[];
	totalElements?: number;
	totalPages?: number;
	number?: number;
	size?: number;
};

export type PaginationMeta = {
	page: number;
	size: number;
	totalPages: number;
	totalElements: number;
};

export type LoginData = {
	userId: number;
	email: string;
	roles: string[];
	permissions: string[];
	sessionId: string;
};

export type Batch = {
	id: number;
	name?: string;
	userCount?: number;
};

export type Major = {
	id: string;
	name: string;
	userCount?: number;
};

export type User = {
	id: number;
	name: string;
	email?: string;
	batch: string;
	majorId?: string | null;
	majorName?: string | null;
	gender?: string;
	status?: string;
	graduatedAt?: string | null;
	createdAt?: string;
};

export type UserImportRowReport = {
	row: number;
	email: string;
	status: 'CREATED' | 'SKIPPED' | 'FAILED';
	message: string;
};

export type UserBulkImportResult = {
	createdCount: number;
	skippedCount: number;
	failedCount: number;
	rows: UserImportRowReport[];
};

export type AuthUser = {
	id: number;
	email: string;
	status: string;
	lastLogin?: string | null;
	roles: string[];
};

export type AuthDashboardSummary = {
	totalUsers: number;
	activeUsers: number;
	pendingActivation: number;
	bannedUsers: number;
};

export type UserDashboardSummary = {
	totalUsers: number;
	totalBatches: number;
	totalMajors: number;
};

export type CourseDashboardSummary = {
	activeGroups: number;
	subjects: number;
	instructors: number;
	groupsWithoutInstructor: number;
	groupsWithoutSchedule: number;
};

export type CourseDashboardReminder = {
	learnerPendingAcknowledgements: number;
	instructorPendingGrades: number;
};

export type AuditLog = {
	id: number;
	actorUserId?: number | null;
	service: string;
	action: string;
	entityType: string;
	entityId?: string | null;
	description?: string | null;
	metadataJson?: string | null;
	createdAt: string;
};

export type Subject = {
	id: number;
	name: string;
};

export type Group = {
	id: number;
	name: string;
	subjectId: number;
	subjectName: string;
	academicYear: string;
	status: string;
};

export type GroupSchedule = {
	id: number;
	groupId: number;
	groupName: string;
	subjectId: number;
	subjectName: string;
	templateId?: number | null;
	templateName?: string | null;
	dayOfWeek: string;
	startTime: string;
	endTime: string;
};

export type ScheduleTemplate = {
	id: number;
	name: string;
	startTime: string;
	endTime: string;
};

export type UserLookup = {
	id: number;
	name: string | null;
	gender: string | null;
	batch: number | null;
};

export type GroupMember = {
	enrollmentId: number;
	user: UserLookup;
	role: string;
};

export type GroupMeeting = {
	id: number | null;
	groupId: number;
	groupName: string;
	subjectId: number;
	subjectName: string;
	subjectMaterialId: number;
	meetingNumber: number;
	title: string;
	description?: string | null;
	meetingDate?: string | null;
	status: 'NOT_STARTED' | 'STARTED' | 'DONE' | string;
	displayStatus: string;
	note?: string | null;
	startedAt?: string | null;
	completedAt?: string | null;
};

export type Assessment = {
	id: number;
	groupId: number;
	groupName: string;
	subjectId: number;
	subjectName: string;
	groupMeetingId?: number | null;
	subjectMaterialId?: number | null;
	meetingNumber?: number | null;
	meetingTitle?: string | null;
	type: string;
	typeLabel: string;
	title: string;
	description?: string | null;
	weight: number;
	dueAt?: string | null;
	hasFile: boolean;
	originalFilename?: string | null;
	filePath?: string | null;
	mimeType?: string | null;
	fileSize?: number | null;
	acknowledged: boolean;
	acknowledgedAt?: string | null;
};

export type AssessmentGrade = {
	id: number;
	assessmentId: number;
	user: UserLookup;
	score: number;
	feedback?: string | null;
	gradedBy: number;
	gradedAt: string;
	updatedAt?: string | null;
};

export type GroupDetail = {
	group: Group;
	schedules: GroupSchedule[];
	meetings: GroupMeeting[];
	assessments: Assessment[];
	members: GroupMember[];
};

export type GroupGradebook = {
	group: Group;
	members: GroupMember[];
	assessments: Assessment[];
	grades: AssessmentGrade[];
};

export type GroupCompleteResult = {
	subjectId: number;
	academicYear: string;
	completedGroups: number;
	deletedSchedules: number;
};

export type Enrollment = {
	id: number;
	user: UserLookup;
	userRole: string;
	subjectId: number;
	subjectName: string;
	groupId: number;
	groupName: string;
	academicYear: string;
};

export type SubjectMaterial = {
	id: number;
	subjectId: number;
	subjectName?: string;
	meetingNumber: number;
	title: string;
	description?: string | null;
};

export type SubjectModule = {
	id: number;
	subjectId: number;
	subjectName?: string;
	originalFilename: string;
	storedFilename: string;
	filePath: string;
	mimeType: string;
	fileSize: number;
};
