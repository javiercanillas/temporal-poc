package io.github.javiercanillas.workers;

import io.github.javiercanillas.activities.FraudActivity;
import io.github.javiercanillas.activities.FraudActivityImpl;
import io.github.javiercanillas.activities.PaymentActivity;
import io.github.javiercanillas.activities.PaymentActivityImpl;
import io.github.javiercanillas.activities.ProductActivity;
import io.github.javiercanillas.activities.ProductActivityImpl;
import io.github.javiercanillas.workflows.OrderWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class WorkerConfiguration {

    @Autowired
    @Bean(initMethod = "start", destroyMethod = "shutdown")
    WorkerFactory workerFactory(WorkflowClient workflowClient,
                                @Value("order-workflow-task-queue") String taskQueue,
                                FraudActivityImpl fraudActivityBean,
                                PaymentActivityImpl paymentActivityBean,
                                ProductActivityImpl productActivityBean) {
        var factory = WorkerFactory.newInstance(workflowClient);
        var options = WorkerOptions.newBuilder()
                .build();
        var worker = factory.newWorker(taskQueue, options);
        // This Worker hosts both Workflow and Activity implementations.
        // Workflows are stateful so a type is needed to create instances.
        worker.registerWorkflowImplementationTypes(OrderWorkflowImpl.class);
        // Activities are stateless and thread safe so a shared instance is used.
        worker.registerActivitiesImplementations(fraudActivityBean,
                paymentActivityBean,
                productActivityBean);
        // Start listening to the Task Queue.
        // factory.start();
        return factory;
    }
}
