package com.example.taskmanager;

import com.example.taskmanager.model.Priority;
import com.example.taskmanager.model.Status;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    private Task newHighTask;
    private Task doneLowTask;
    private Task newMediumTask;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        newHighTask = taskRepository.save(new Task(
                null, "Prepare JPA lesson", "Explain repositories", Priority.HIGH, Status.NEW));
        doneLowTask = taskRepository.save(new Task(
                null, "Write report", null, Priority.LOW, Status.DONE));
        newMediumTask = taskRepository.save(new Task(
                null, "Review Hibernate", "JPA keyword in description", Priority.MEDIUM, Status.NEW));
    }

    @Test
    void savesTaskAndGeneratesId() {
        assertThat(newHighTask.getId()).isNotNull();
        assertThat(taskRepository.findById(newHighTask.getId()))
                .get()
                .extracting(Task::getTitle)
                .isEqualTo("Prepare JPA lesson");
    }

    @Test
    void executesDerivedQueries() {
        assertThat(taskRepository.findByStatus(Status.NEW))
                .extracting(Task::getId)
                .containsExactlyInAnyOrder(newHighTask.getId(), newMediumTask.getId());
        assertThat(taskRepository.findByPriority(Priority.LOW))
                .extracting(Task::getId)
                .containsExactly(doneLowTask.getId());
        assertThat(taskRepository.findByStatusAndPriority(Status.NEW, Priority.HIGH))
                .extracting(Task::getId)
                .containsExactly(newHighTask.getId());
        assertThat(taskRepository.findByTitleContainingIgnoreCase("jpa"))
                .extracting(Task::getId)
                .containsExactly(newHighTask.getId());
        assertThat(taskRepository.countByStatus(Status.NEW)).isEqualTo(2);
    }

    @Test
    void searchesTitleAndNullableDescriptionWithJpql() {
        assertThat(taskRepository.searchByKeyword("jpa"))
                .extracting(Task::getId)
                .containsExactlyInAnyOrder(newHighTask.getId(), newMediumTask.getId());
        assertThat(taskRepository.searchByKeyword("report"))
                .extracting(Task::getId)
                .containsExactly(doneLowTask.getId());
    }

    @Test
    void groupsCountsByStatusWithJpql() {
        Map<Status, Long> counts = taskRepository.countGroupedByStatus().stream()
                .collect(Collectors.toMap(row -> (Status) row[0], row -> (Long) row[1]));

        assertThat(counts).containsEntry(Status.NEW, 2L).containsEntry(Status.DONE, 1L);
    }

    @Test
    void paginatesAndSortsResultsInDatabase() {
        Page<Task> firstPage = taskRepository.findAll(PageRequest.of(0, 2, Sort.by("id")));
        Page<Task> secondPage = taskRepository.findAll(PageRequest.of(1, 2, Sort.by("id")));

        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(secondPage.getContent()).hasSize(1);
    }
}
