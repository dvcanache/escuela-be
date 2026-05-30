package com.escuela.modules.schedule.web;

import com.escuela.modules.schedule.domain.Schedule;
import java.time.LocalDateTime;

public class ScheduleResponse {
    private Long id;
    private String subjectName;
    private String classroom;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String professorName;

    public ScheduleResponse(Schedule s) {
        this.id = s.getId();
        this.subjectName = s.getSubjectName();
        this.classroom = s.getClassroom();
        this.startTime = s.getStartTime();
        this.endTime = s.getEndTime();
        this.status = s.getStatus().name();
        this.professorName = s.getProfessor().getFirstName() + " " + s.getProfessor().getLastName();
    }

    // Getters
    public Long getId() { return id; }
    public String getSubjectName() { return subjectName; }
    public String getClassroom() { return classroom; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public String getStatus() { return status; }
    public String getProfessorName() { return professorName; }
}
