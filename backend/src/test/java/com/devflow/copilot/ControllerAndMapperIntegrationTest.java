package com.devflow.copilot;

import com.devflow.copilot.entity.ProjectContext;
import com.devflow.copilot.entity.AiTask;
import com.devflow.copilot.mapper.AiTaskMapper;
import com.devflow.copilot.mapper.ProjectContextMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ControllerAndMapperIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ProjectContextMapper projectMapper;
    @Autowired AiTaskMapper aiTaskMapper;

    @Test
    void createsProjectThroughController() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectName":"订单中心","techStack":"Java 17, Vue 3","codingRules":"人工确认"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.projectName").value("订单中心"));
    }

    @Test
    void rejectsBlankProjectName() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(4000))
                .andExpect(jsonPath("$.message").value("项目名称不能为空"));
    }

    @Test
    void rejectsBlankGenerationInput() throws Exception {
        mockMvc.perform(post("/api/ai/requirement-split")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":1,\"input\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(4000));
    }

    @Test
    void listsAiTasksByProjectId() throws Exception {
        seedTask(1L, "先处理的任务");
        seedTask(1L, "后处理的任务");
        seedTask(2L, "其他项目任务");

        mockMvc.perform(get("/api/tasks").param("projectId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].taskTitle").value("后处理的任务"))
                .andExpect(jsonPath("$.data[1].taskTitle").value("先处理的任务"));
    }

    @Test
    void returnsEmptyAiTaskListForUnknownProject() throws Exception {
        seedTask(1L, "已有项目任务");

        mockMvc.perform(get("/api/tasks").param("projectId", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void rejectsMissingProjectIdWhenListingAiTasks() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(4000))
                .andExpect(jsonPath("$.message").value("projectId 不能为空"));
    }

    @Test
    void mapperCrudPersistsProject() {
        ProjectContext project = new ProjectContext();
        project.setProjectName("Mapper CRUD 测试");
        project.setVersion(0);
        project.setCreatedAt(java.time.LocalDateTime.now());
        project.setUpdatedAt(project.getCreatedAt());

        projectMapper.insert(project);

        assertThat(project.getId()).isNotNull();
        assertThat(projectMapper.selectById(project.getId()).getProjectName()).isEqualTo("Mapper CRUD 测试");
    }

    private AiTask seedTask(Long projectId, String taskTitle) {
        AiTask task = new AiTask();
        task.setProjectId(projectId);
        task.setTaskTitle(taskTitle);
        task.setTaskType("backend");
        task.setPriority("High");
        task.setStatus("Ready for Review");
        task.setRiskLevel("Low");
        task.setVersion(0);
        LocalDateTime now = LocalDateTime.now();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        aiTaskMapper.insert(task);
        return task;
    }
}
