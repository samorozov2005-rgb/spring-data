package com.example.taskmanager.service;

import com.example.taskmanager.model.Task;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface TaskService {

    Page<Task> findAll(String status, String priority, int page, int size, String sort);

    List<Task> search(String keyword);

    Task getById(Long id);

    Task create(Task task);

    Task update(Long id, Task task);

    Task updateStatus(Long id, String status);

    void delete(Long id);

    Map<String, Long> getStats();
}
