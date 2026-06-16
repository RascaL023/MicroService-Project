package com.rascal.course_service.enumerated;

public enum GroupMeetingStatusEnum {
    STARTED("Berjalan"),
    DONE("Selesai");

    private final String displayName;

    GroupMeetingStatusEnum(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }
    public static GroupMeetingStatusEnum from(String value) {
        return GroupMeetingStatusEnum.valueOf(value.trim().toUpperCase());
    }
}
