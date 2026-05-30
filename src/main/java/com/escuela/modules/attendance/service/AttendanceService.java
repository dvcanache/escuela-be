package com.escuela.modules.attendance.service;

import com.escuela.modules.attendance.domain.Attendance;
import com.escuela.modules.attendance.domain.AttendanceRepository;
import com.escuela.modules.schedule.domain.Schedule;
import com.escuela.modules.schedule.domain.ScheduleRepository;
import com.escuela.core.common.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Transactional
    public Attendance recordAttendance(Attendance attendance, Long currentUserId) {
        // Step 4.3: Validate professor owns the schedule
        Schedule schedule = scheduleRepository.findById(attendance.getSchedule().getId())
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        if (!schedule.getProfessor().getId().equals(currentUserId)) {
            throw new RuntimeException("Validation Error: Only the assigned professor can record attendance for this class.");
        }

        return attendanceRepository.save(attendance);
    }

    @Transactional
    public Attendance updateAttendanceStatus(Long id, Attendance.PresenceStatus status, String notes) {
        // Step 4.2: Update with audit notes
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attendance record not found"));
        
        attendance.setPresenceStatus(status);
        attendance.setTeacherNotes(notes);
        
        return attendanceRepository.save(attendance);
    }

    public List<Attendance> getAttendanceBySchedule(Long scheduleId) {
        return attendanceRepository.findByScheduleId(scheduleId);
    }
}
