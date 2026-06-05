package com.rascal.course_service.enumerated;

public enum CourseRoleEnum{
  INSTRUCTOR("Instruktur"),
  LEARNER("Pelajar");

  private final String displayName;

  CourseRoleEnum(String displayName){ this.displayName = displayName; }

  public String getDisplayName(){ return displayName; }
  public static CourseRoleEnum from(String value) { return CourseRoleEnum.valueOf(value.toUpperCase()); }
}
