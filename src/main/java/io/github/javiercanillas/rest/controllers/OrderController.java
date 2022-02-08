package io.github.javiercanillas.rest.controllers;

import io.github.javiercanillas.workflows.OrderWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
public class OrderController {

    public static final String ORDER_WORKER_POC = "Order-Workers-POC";
    public static final String ORDER_EVENT_POC = "Order-Event-POC";
    private final WorkflowClient client;
    private final String taskQueue;

    @Autowired
    public OrderController(WorkflowClient client, @Value("order-workflow-task-queue") String taskQueue) {
        this.client = client;
        this.taskQueue = taskQueue;
    }

    @PostMapping(value = "/order", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> createAndApproveOrder(@RequestBody Map<String, Object> orderData) {
        final var orderId = orderData.getOrDefault("orderId", UUID.randomUUID()).toString();
        var options = WorkflowOptions.newBuilder()
                .setTaskQueue(taskQueue)
                // A WorkflowId prevents this it from having duplicate instances, remove it to duplicate.
                .setWorkflowId(orderId)
                .build();
        var workflowIntance = client.newWorkflowStub(OrderWorkflow.class, options);
        var we = WorkflowClient.start(workflowIntance::approve, orderId);
        return Map.of(
                "orderId", orderId,
                "workflowId", we.getWorkflowId(),
                "runId", we.getRunId()
        );
    }
}
