package com.escuela.modules.schedule.web;

import com.escuela.modules.schedule.domain.Schedule;
import com.escuela.modules.schedule.service.ScheduleService;
import com.escuela.modules.security.domain.User;
import com.escuela.modules.security.domain.UserRepository;
import com.escuela.modules.tenant.domain.Tenant;
import com.escuela.core.common.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/schedules")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> createSchedule(@RequestBody ScheduleRequest request) {
        User professor = userRepository.findById(request.getProfessorId())
                .orElseThrow(() -> new RuntimeException("Professor not found"));

        Schedule schedule = new Schedule();
        schedule.setProfessor(professor);
        schedule.setSubjectName(request.getSubjectName());
        schedule.setClassroom(request.getClassroom());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        
        Tenant tenant = new Tenant();
        tenant.setId(TenantContext.getCurrentTenant());
        schedule.setTenant(tenant);

        try {
            Schedule created = scheduleService.createSchedule(schedule);
            return ResponseEntity.ok(new ScheduleResponse(created));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam Schedule.ScheduleStatus status) {
        try {
            Schedule updated = scheduleService.updateStatus(id, status);
            return ResponseEntity.ok(new ScheduleResponse(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public List<ScheduleResponse> getSchedules() {
        return scheduleService.getAllSchedules().stream()
                .map(ScheduleResponse::new)
                .toList();
    }
}
