package com.rascal.course_service.enumerated;

public enum CourseStatusEnum{
    ON_GOING("On going"),
    PASSED("Passed");

    private final String displayName;

    CourseStatusEnum(String displayName){ this.displayName = displayName; }

    public String getDisplayName(){ return displayName; }
    public static CourseStatusEnum from(String value) { 
        return CourseStatusEnum.valueOf(value.toUpperCase()); 
    }
}

