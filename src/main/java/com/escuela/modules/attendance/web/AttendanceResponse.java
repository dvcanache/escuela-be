package com.escuela.modules.attendance.web;

import com.escuela.modules.attendance.domain.Attendance;
import java.time.LocalDateTime;

public class AttendanceResponse {
    private Long id;
    private Long scheduleId;
    private String studentName;
    private String status;
    private String notes;
    private LocalDateTime recordedAt;

    public AttendanceResponse(Attendance a) {
        this.id = a.getId();
        this.scheduleId = a.getSchedule().getId();
        this.studentName = a.getStudent().getFirstName() + " " + a.getStudent().getLastName();
        this.status = a.getPresenceStatus().name();
        this.notes = a.getTeacherNotes();
        this.recordedAt = a.getRecordedAt();
    }

    // Getters
    public Long getId() { return id; }
    public Long getScheduleId() { return scheduleId; }
    public String getStudentName() { return studentName; }
    public String getStatus() { return status; }
    public String getNotes() { return notes; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
}
