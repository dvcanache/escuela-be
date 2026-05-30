package com.escuela.modules.attendance.web;

import com.escuela.modules.attendance.domain.Attendance;
import com.escuela.modules.attendance.service.AttendanceService;
import com.escuela.modules.schedule.domain.Schedule;
import com.escuela.modules.security.UserDetailsImpl;
import com.escuela.modules.security.domain.User;
import com.escuela.modules.security.domain.UserRepository;
import com.escuela.modules.tenant.domain.Tenant;
import com.escuela.core.common.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> recordAttendance(@RequestBody AttendanceRequest request) {
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        User student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Attendance attendance = new Attendance();
        
        Schedule schedule = new Schedule();
        schedule.setId(request.getScheduleId());
        attendance.setSchedule(schedule);
        
        attendance.setStudent(student);
        attendance.setPresenceStatus(request.getStatus());
        attendance.setTeacherNotes(request.getNotes());

        Tenant tenant = new Tenant();
        tenant.setId(TenantContext.getCurrentTenant());
        attendance.setTenant(tenant);

        try {
            Attendance created = attendanceService.recordAttendance(attendance, currentUser.getId());
            return ResponseEntity.ok(new AttendanceResponse(created));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateAttendance(@PathVariable Long id, 
                                            @RequestParam Attendance.PresenceStatus status,
                                            @RequestParam(required = false) String notes) {
        try {
            Attendance updated = attendanceService.updateAttendanceStatus(id, status, notes);
            return ResponseEntity.ok(new AttendanceResponse(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/schedule/{scheduleId}")
    public List<AttendanceResponse> getAttendanceBySchedule(@PathVariable Long scheduleId) {
        return attendanceService.getAttendanceBySchedule(scheduleId).stream()
                .map(AttendanceResponse::new)
                .toList();
    }
}
