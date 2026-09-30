package com.assessment.urlshortener.orchestration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkflowController.class)
@Import({
        WorkflowApiService.class,
        WorkflowStore.class,
        WorkflowOrchestrator.class,
        PolicyGuardrail.class,
        RequirementAnalysisExecutor.class,
        ArchitectureDesignExecutor.class,
        ImplementationExecutor.class,
        TestingExecutor.class,
        DocumentationExecutor.class,
        ReleaseReadinessExecutor.class
})
class WorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateWorkflow() throws Exception {

        mockMvc.perform(
                        post("/api/workflows")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "requirement": "Build a URL shortener service"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workflowId").isNotEmpty())
                .andExpect(jsonPath("$.requirement")
                        .value("Build a URL shortener service"))
                .andExpect(jsonPath("$.stageStatuses.REQUIREMENT_ANALYSIS")
                        .value("PENDING"))
                .andExpect(jsonPath("$.stageStatuses.RELEASE_READINESS")
                        .value("PENDING"))
                .andExpect(jsonPath("$.releaseApproved")
                        .value(false));
    }

    @Test
    void shouldRejectEmptyRequirement() throws Exception {

        mockMvc.perform(
                        post("/api/workflows")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "requirement": ""
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldExecuteWorkflowAndWaitForApproval() throws Exception {

        MvcResult createResult = mockMvc.perform(
                        post("/api/workflows")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "requirement": "Build a URL shortener service"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andReturn();

        String response =
                createResult.getResponse().getContentAsString();

        String workflowId = response
                .split("\"workflowId\":\"")[1]
                .split("\"")[0];

        mockMvc.perform(
                        post("/api/workflows/" + workflowId + "/execute")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.stageStatuses.REQUIREMENT_ANALYSIS")
                        .value("COMPLETED"))
                .andExpect(jsonPath(
                        "$.stageStatuses.ARCHITECTURE_DESIGN")
                        .value("COMPLETED"))
                .andExpect(jsonPath(
                        "$.stageStatuses.IMPLEMENTATION")
                        .value("COMPLETED"))
                .andExpect(jsonPath(
                        "$.stageStatuses.TESTING")
                        .value("COMPLETED"))
                .andExpect(jsonPath(
                        "$.stageStatuses.DOCUMENTATION")
                        .value("COMPLETED"))
                .andExpect(jsonPath(
                        "$.stageStatuses.RELEASE_READINESS")
                        .value("WAITING_FOR_APPROVAL"))
                .andExpect(jsonPath("$.releaseApproved")
                        .value(false));
    }

    @Test
    void shouldApproveWorkflowThroughApi() throws Exception {

        MvcResult createResult = mockMvc.perform(
                        post("/api/workflows")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "requirement": "Build a URL shortener service"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andReturn();

        String response =
                createResult.getResponse().getContentAsString();

        String workflowId = response
                .split("\"workflowId\":\"")[1]
                .split("\"")[0];

        mockMvc.perform(
                        post("/api/workflows/" + workflowId + "/execute")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.stageStatuses.RELEASE_READINESS")
                        .value("WAITING_FOR_APPROVAL"));

        mockMvc.perform(
                        post("/api/workflows/" + workflowId + "/approve")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "approvedBy": "Engineering Manager"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.releaseApproved")
                        .value(true))
                .andExpect(jsonPath("$.approvedBy")
                        .value("Engineering Manager"));
    }

    @Test
    void shouldCompleteWorkflowAfterApproval() throws Exception {

        MvcResult createResult = mockMvc.perform(
                        post("/api/workflows")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "requirement": "Build a URL shortener service"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andReturn();

        String response =
                createResult.getResponse().getContentAsString();

        String workflowId = response
                .split("\"workflowId\":\"")[1]
                .split("\"")[0];

        mockMvc.perform(
                        post("/api/workflows/" + workflowId + "/execute")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.stageStatuses.RELEASE_READINESS")
                        .value("WAITING_FOR_APPROVAL"));

        mockMvc.perform(
                        post("/api/workflows/" + workflowId + "/approve")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "approvedBy": "Engineering Manager"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.releaseApproved")
                        .value(true));

        mockMvc.perform(
                        post("/api/workflows/" + workflowId + "/execute")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.stageStatuses.RELEASE_READINESS")
                        .value("COMPLETED"))
                .andExpect(jsonPath("$.releaseApproved")
                        .value(true));
    }

    @Test
    void shouldExposeWorkflowMetrics() throws Exception {

        MvcResult createResult = mockMvc.perform(
                        post("/api/workflows")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "requirement": "Build a URL shortener service"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andReturn();

        String response =
                createResult.getResponse().getContentAsString();

        String workflowId = response
                .split("\"workflowId\":\"")[1]
                .split("\"")[0];

        mockMvc.perform(
                        post("/api/workflows/" + workflowId + "/execute")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successRate")
                        .isNumber())
                .andExpect(jsonPath("$.retryCount")
                        .isNumber())
                .andExpect(jsonPath("$.rollbackCount")
                        .isNumber())
                .andExpect(jsonPath("$.meanTimeToRecoveryMillis")
                        .isNumber())
                .andExpect(jsonPath("$.endToEndLatencyMillis")
                        .isNumber());
    }

    @Test
    void shouldGetWorkflowById() throws Exception {

        MvcResult createResult = mockMvc.perform(
                        post("/api/workflows")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "requirement": "Build a URL shortener service"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andReturn();

        String response =
                createResult.getResponse().getContentAsString();

        String workflowId = response
                .split("\"workflowId\":\"")[1]
                .split("\"")[0];

        mockMvc.perform(
                        get("/api/workflows/" + workflowId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowId")
                        .value(workflowId))
                .andExpect(jsonPath("$.requirement")
                        .value("Build a URL shortener service"))
                .andExpect(jsonPath(
                        "$.stageStatuses.REQUIREMENT_ANALYSIS")
                        .value("PENDING"))
                .andExpect(jsonPath(
                        "$.stageStatuses.RELEASE_READINESS")
                        .value("PENDING"))
                .andExpect(jsonPath("$.releaseApproved")
                        .value(false));
    }
    @Test
void shouldReturnNotFoundForUnknownWorkflow() throws Exception {

    mockMvc.perform(
                    get("/api/workflows/non-existent-workflow")
            )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message")
                    .value("Workflow not found: non-existent-workflow"));
}
}