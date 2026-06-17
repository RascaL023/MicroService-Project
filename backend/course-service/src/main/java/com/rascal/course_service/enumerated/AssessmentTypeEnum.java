package com.rascal.course_service.enumerated;

import java.math.BigDecimal;

public enum AssessmentTypeEnum {
    ASSIGNMENT("Tugas", "10"),
    QUIZ("Quiz", "20"),
    MIDTERM("UTS", "30"),
    FINAL_EXAM("UAS", "40");

    private final String displayName;
    private final BigDecimal weight;

    AssessmentTypeEnum(String displayName, String weight) {
        this.displayName = displayName;
        this.weight = new BigDecimal(weight);
    }

    public String getDisplayName() { return displayName; }
    public BigDecimal getWeight() { return weight; }

    public static AssessmentTypeEnum from(String value) {
        String normalized = value.trim().toUpperCase().replace("-", "_");
        return switch (normalized) {
            case "TASK", "TUGAS", "ASSIGNMENT" -> ASSIGNMENT;
            case "QUIZ", "KUIS" -> QUIZ;
            case "UTS", "MIDTERM" -> MIDTERM;
            case "UAS", "FINAL", "FINAL_EXAM" -> FINAL_EXAM;
            default -> AssessmentTypeEnum.valueOf(normalized);
        };
    }

}
