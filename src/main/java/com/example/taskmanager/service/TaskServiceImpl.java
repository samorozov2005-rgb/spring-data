package com.example.taskmanager.service;

import com.example.taskmanager.exception.TaskNotFoundException;
import com.example.taskmanager.model.Priority;
import com.example.taskmanager.model.Status;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;

    public TaskServiceImpl(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public Page<Task> findAll(String statusValue, String priorityValue, int page, int size, String sort) {
        Status status = parseStatus(statusValue);
        Priority priority = parsePriority(priorityValue);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));

        if (status != null && priority != null) {
            return taskRepository.findByStatusAndPriority(status, priority, pageable);
        }
        if (status != null) {
            return taskRepository.findByStatus(status, pageable);
        }
        if (priority != null) {
            return taskRepository.findByPriority(priority, pageable);
        }
        return taskRepository.findAll(pageable);
    }

    @Override
    public List<Task> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("Search keyword must not be empty");
        }
        return taskRepository.searchByKeyword(keyword);
    }

    @Override
    public Task getById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    @Override
    public Task create(Task task) {
        validateTitle(task);
        task.setId(null);
        task.setStatus(Status.NEW);
        return taskRepository.save(task);
    }

    @Override
    public Task update(Long id, Task task) {
        getById(id);
        validateTitle(task);
        task.setId(id);
        if (task.getStatus() == null) {
            task.setStatus(Status.NEW);
        }
        return taskRepository.save(task);
    }

    @Override
    public Task updateStatus(Long id, String statusValue) {
        Task task = getById(id);
        if (statusValue == null || statusValue.isBlank()) {
            throw new IllegalArgumentException("Status must not be empty");
        }
        task.setStatus(parseRequiredStatus(statusValue));
        return taskRepository.save(task);
    }

    @Override
    public void delete(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new TaskNotFoundException(id);
        }
        taskRepository.deleteById(id);
    }

    @Override
    public Map<String, Long> getStats() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Status status : Status.values()) {
            result.put(status.name(), 0L);
        }
        for (Object[] row : taskRepository.countGroupedByStatus()) {
            Status status = (Status) row[0];
            Long count = (Long) row[1];
            result.put(status.name(), count);
        }
        return result;
    }

    private void validateTitle(Task task) {
        if (task == null || task.getTitle() == null || task.getTitle().isBlank()) {
            throw new IllegalArgumentException("Task title must not be empty");
        }
    }

    private Status parseStatus(String value) {
        return value == null ? null : parseRequiredStatus(value);
    }

    private Status parseRequiredStatus(String value) {
        try {
            return Status.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown status: " + value);
        }
    }

    private Priority parsePriority(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Priority.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown priority: " + value);
        }
    }
}
