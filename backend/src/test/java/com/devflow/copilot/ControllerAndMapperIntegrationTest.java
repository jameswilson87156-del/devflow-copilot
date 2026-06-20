package com.devflow.copilot;

import com.devflow.copilot.entity.ProjectContext;
import com.devflow.copilot.mapper.ProjectContextMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
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
}
