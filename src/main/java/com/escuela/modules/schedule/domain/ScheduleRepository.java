package com.escuela.modules.schedule.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @Query("SELECT s FROM Schedule s WHERE s.professor.id = :professorId " +
           "AND ((s.startTime < :end AND s.endTime > :start)) " +
           "AND s.status = 'ACTIVE'")
    List<Schedule> findOverlappingProfessorSchedules(@Param("professorId") Long professorId, 
                                                    @Param("start") LocalDateTime start, 
                                                    @Param("end") LocalDateTime end);

    @Query("SELECT s FROM Schedule s WHERE s.classroom = :classroom " +
           "AND ((s.startTime < :end AND s.endTime > :start)) " +
           "AND s.status = 'ACTIVE'")
    List<Schedule> findOverlappingClassroomSchedules(@Param("classroom") String classroom, 
                                                     @Param("start") LocalDateTime start, 
                                                     @Param("end") LocalDateTime end);
}
