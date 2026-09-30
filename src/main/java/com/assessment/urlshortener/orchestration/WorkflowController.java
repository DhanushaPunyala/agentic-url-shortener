package com.assessment.urlshortener.orchestration;

import com.assessment.urlshortener.orchestration.dto.ApproveWorkflowRequest;
import com.assessment.urlshortener.orchestration.dto.CreateWorkflowRequest;
import com.assessment.urlshortener.orchestration.dto.WorkflowResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowApiService workflowApiService;

    public WorkflowController(
            WorkflowApiService workflowApiService) {

        this.workflowApiService = workflowApiService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowResponse createWorkflow(
            @Valid @RequestBody CreateWorkflowRequest request) {

        return workflowApiService.createWorkflow(
                request.getRequirement()
        );
    }

    @GetMapping("/{workflowId}")
    public WorkflowResponse getWorkflow(
            @PathVariable String workflowId) {

        return workflowApiService.getWorkflow(workflowId);
    }

    @PostMapping("/{workflowId}/execute")
    public WorkflowResponse executeWorkflow(
            @PathVariable String workflowId) {

        return workflowApiService.executeWorkflow(workflowId);
    }

    @PostMapping("/{workflowId}/approve")
    public WorkflowResponse approveWorkflow(
            @PathVariable String workflowId,
            @Valid @RequestBody ApproveWorkflowRequest request) {

        return workflowApiService.approveWorkflow(
                workflowId,
                request.getApprovedBy()
        );
    }
}
