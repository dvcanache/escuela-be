package com.escuela.modules.schedule.service;

import com.escuela.modules.schedule.domain.Schedule;
import com.escuela.modules.schedule.domain.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ScheduleService {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Transactional
    public Schedule createSchedule(Schedule schedule) {
        // Step 3.1: Check for collisions (Professor or Classroom)
        List<Schedule> professorConflicts = scheduleRepository.findOverlappingProfessorSchedules(
                schedule.getProfessor().getId(), schedule.getStartTime(), schedule.getEndTime());
        
        if (!professorConflicts.isEmpty()) {
            throw new RuntimeException("Conflict: Professor already has a class in this time slot.");
        }

        List<Schedule> classroomConflicts = scheduleRepository.findOverlappingClassroomSchedules(
                schedule.getClassroom(), schedule.getStartTime(), schedule.getEndTime());
        
        if (!classroomConflicts.isEmpty()) {
            throw new RuntimeException("Conflict: Classroom is already occupied in this time slot.");
        }

        return scheduleRepository.save(schedule);
    }

    @Transactional
    public Schedule updateStatus(Long scheduleId, Schedule.ScheduleStatus status) {
        // Step 3.2: Update status (CANCELLED, FREE_HOUR)
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));
        schedule.setStatus(status);
        return scheduleRepository.save(schedule);
    }

    public List<Schedule> getAllSchedules() {
        return scheduleRepository.findAll();
    }
}
