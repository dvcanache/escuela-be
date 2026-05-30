package com.escuela.modules.attendance.web;

import com.escuela.modules.attendance.domain.Attendance;

public class AttendanceRequest {
    private Long scheduleId;
    private Long studentId;
    private Attendance.PresenceStatus status;
    private String notes;

    // Getters and Setters
    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Attendance.PresenceStatus getStatus() { return status; }
    public void setStatus(Attendance.PresenceStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
