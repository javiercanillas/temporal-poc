package io.github.javiercanillas.rest.controllers;

import io.github.javiercanillas.workflows.TransactionWorkflow;
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
@RequestMapping("/transaction")
public class TransactionController {

    public static final String TRANSACTION_WORKER_POC = "Transaction-Workers-POC";
    public static final String TRANSACTION_EVENT_POC = "Transaction-Event-POC";
    private final WorkflowClient client;
    private final String taskQueue;

    @Autowired
    public TransactionController(WorkflowClient client, @Value("transaction-workflow-task-queue") String taskQueue) {
        this.client = client;
        this.taskQueue = taskQueue;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createAndApproveTransaction(@RequestBody Map<String, Object> transactionData) {
        final var transactionId = transactionData.getOrDefault("transactionId", UUID.randomUUID()).toString();
        var options = WorkflowOptions.newBuilder()
                .setTaskQueue(taskQueue)
                // A WorkflowId prevents this it from having duplicate instances, remove it to duplicate.
                .setWorkflowId(transactionId)
                .build();
        var workflowIntance = client.newWorkflowStub(TransactionWorkflow.class, options);
        var we = WorkflowClient.start(workflowIntance::approve, transactionId);
        return ResponseEntity.ok(
                Map.of(
                "transactionId", transactionId,
                "workflowId", we.getWorkflowId(),
                "runId", we.getRunId()
                )
        );
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, path = "/{transactionId}")
    public ResponseEntity<Map<String, Object>> getTransaction(@PathVariable("transactionId") String transactionId) {
        TransactionWorkflow workflowInstance;
        try {
            workflowInstance = client.newWorkflowStub(TransactionWorkflow.class, transactionId);
        } catch (WorkflowNotFoundException e) {
            return ResponseEntity.notFound().build();
        }

        var os = workflowInstance.getTransactionInformation();
        return ResponseEntity.ok(
                Map.of(
                "transactionId", transactionId,
                "status", os
                )
        );
    }

    @PatchMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, path = "/{transactionId}")
    public ResponseEntity<Map<String, Object>> alterTransaction(@PathVariable("transactionId") String transactionId, @RequestBody Map<String, Object> transactionData) {
        var workflowInstance = client.newWorkflowStub(TransactionWorkflow.class, transactionId);
        if (transactionData.containsKey("validateRisk")) {
            workflowInstance.notifyFulfilledSignal(TransactionWorkflow.Signal.PENDING_RISK_VALIDATION);
            return ResponseEntity.accepted().build();
        } else if (transactionData.containsKey("newPaymentMethod")) {
            workflowInstance.notifyFulfilledSignal(TransactionWorkflow.Signal.NEW_PAYMEMT_METHOD);
            return ResponseEntity.accepted().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
    }
}
