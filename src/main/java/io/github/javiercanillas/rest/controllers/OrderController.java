package io.github.javiercanillas.rest.controllers;

import io.github.javiercanillas.workflows.OrderWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowNotFoundException;
import io.temporal.client.WorkflowOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController()
@RequestMapping("/order")
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

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createAndApproveOrder(@RequestBody Map<String, Object> orderData) {
        final var orderId = orderData.getOrDefault("orderId", UUID.randomUUID()).toString();
        var options = WorkflowOptions.newBuilder()
                .setTaskQueue(taskQueue)
                // A WorkflowId prevents this it from having duplicate instances, remove it to duplicate.
                .setWorkflowId(orderId)
                .build();
        var workflowIntance = client.newWorkflowStub(OrderWorkflow.class, options);
        var we = WorkflowClient.start(workflowIntance::approve, orderId);
        return ResponseEntity.ok(
                Map.of(
                "orderId", orderId,
                "workflowId", we.getWorkflowId(),
                "runId", we.getRunId()
                )
        );
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, path = "/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable("orderId") String orderId) {
        OrderWorkflow workflowInstance;
        try {
            workflowInstance = client.newWorkflowStub(OrderWorkflow.class, orderId);
        } catch (WorkflowNotFoundException e) {
            return ResponseEntity.notFound().build();
        }

        var os = workflowInstance.getOrderInformation();
        return ResponseEntity.ok(
                Map.of(
                "orderId", orderId,
                "status", os
                )
        );
    }

    @PatchMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, path = "/{orderId}")
    public ResponseEntity<Map<String, Object>> alterOrder(@PathVariable("orderId") String orderId, @RequestBody Map<String, Object> orderData) {
        var workflowInstance = client.newWorkflowStub(OrderWorkflow.class, orderId);
        if (orderData.containsKey("completedKyc")) {
            workflowInstance.notifyFulfilledSignal(OrderWorkflow.Signal.PENDING_KYC);
            return ResponseEntity.accepted().build();
        } else if (orderData.containsKey("newPaymentMethod")) {
            workflowInstance.notifyFulfilledSignal(OrderWorkflow.Signal.NEW_PAYMEMT_METHOD);
            return ResponseEntity.accepted().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
    }
}
