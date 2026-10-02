package com.example.taskmanager;

import com.example.taskmanager.repository.TaskRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void clearDatabase() {
        taskRepository.deleteAll();
    }

    @Test
    void completeJpaApiScenario() throws Exception {
        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));

        long lowId = createTask("Low task", "Ordinary work", "LOW");
        long mediumId = createTask("Medium keyword task", "Test", "MEDIUM");
        long highId = createTask("High task", "Contains KEYWORD", "HIGH");
        long updateId = createTask("Update task", null, "LOW");
        createTask("Another high task", "Test", "HIGH");

        mockMvc.perform(get("/tasks/{id}", lowId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Low task"));

        mockMvc.perform(get("/tasks/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Task not found"));

        mockMvc.perform(put("/tasks/{id}", updateId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Updated task","description":"Changed","priority":"LOW","status":"DONE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(updateId))
                .andExpect(jsonPath("$.status").value("DONE"));

        mockMvc.perform(patch("/tasks/{id}/status", mediumId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(get("/tasks").param("page", "0").param("size", "3").param("sort", "id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(3));

        mockMvc.perform(get("/tasks").param("page", "1").param("size", "3").param("sort", "id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.number").value(1));

        mockMvc.perform(get("/tasks").param("status", "NEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3));

        mockMvc.perform(get("/tasks").param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        mockMvc.perform(get("/tasks").param("status", "NEW").param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        mockMvc.perform(get("/tasks/search").param("keyword", "keyword"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/tasks/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.NEW").value(3))
                .andExpect(jsonPath("$.IN_PROGRESS").value(1))
                .andExpect(jsonPath("$.DONE").value(1));

        mockMvc.perform(delete("/tasks/{id}", highId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(get("/tasks/{id}", highId)).andExpect(status().isNotFound());
    }

    @Test
    void returnsExistingErrorFormatForInvalidInput() throws Exception {
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"priority\":\"HIGH\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad request"));

        mockMvc.perform(get("/tasks").param("status", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown status: INVALID"));

        mockMvc.perform(get("/tasks").param("priority", "SUPER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown priority: SUPER"));

        mockMvc.perform(get("/tasks/search").param("keyword", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Search keyword must not be empty"));
    }

    private long createTask(String title, String description, String priority) throws Exception {
        String body = objectMapper.writeValueAsString(new CreateTaskRequest(title, description, priority));
        String response = mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode task = objectMapper.readTree(response);
        return task.get("id").asLong();
    }

    private record CreateTaskRequest(String title, String description, String priority) {
    }
}
