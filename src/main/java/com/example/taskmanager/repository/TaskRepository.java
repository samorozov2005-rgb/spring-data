package com.example.taskmanager.repository;

import com.example.taskmanager.model.Priority;
import com.example.taskmanager.model.Status;
import com.example.taskmanager.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByStatus(Status status);

    List<Task> findByPriority(Priority priority);

    List<Task> findByStatusAndPriority(Status status, Priority priority);

    List<Task> findByTitleContainingIgnoreCase(String keyword);

    long countByStatus(Status status);

    Page<Task> findByStatus(Status status, Pageable pageable);

    Page<Task> findByPriority(Priority priority, Pageable pageable);

    Page<Task> findByStatusAndPriority(Status status, Priority priority, Pageable pageable);

    @Query("""
            SELECT t
            FROM Task t
            WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :kw, '%'))
               OR (t.description IS NOT NULL
                   AND LOWER(t.description) LIKE LOWER(CONCAT('%', :kw, '%')))
            """)
    List<Task> searchByKeyword(@Param("kw") String keyword);

    @Query("""
            SELECT t.status, COUNT(t)
            FROM Task t
            GROUP BY t.status
            """)
    List<Object[]> countGroupedByStatus();
}
