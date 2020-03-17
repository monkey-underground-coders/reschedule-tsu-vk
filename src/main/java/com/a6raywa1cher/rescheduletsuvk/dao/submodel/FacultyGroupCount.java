package com.a6raywa1cher.rescheduletsuvk.dao.submodel;

import lombok.Data;

@Data
public class FacultyGroupCount {
    private final String facultyId;

    private final String groupId;

    private final long count;

    public FacultyGroupCount(String facultyId, String groupId, long count) {
        this.facultyId = facultyId;
        this.groupId = groupId;
        this.count = count;
    }
}
