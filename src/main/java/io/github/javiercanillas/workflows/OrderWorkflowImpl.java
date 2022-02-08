package io.github.javiercanillas.workflows;

import io.github.javiercanillas.activities.FraudActivity;
import io.github.javiercanillas.activities.PaymentActivity;
import io.github.javiercanillas.activities.ProductActivity;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Slf4j
public class OrderWorkflowImpl implements OrderWorkflow {

    private final FraudActivity fraudActivity;
    private final PaymentActivity paymentActivity;
    private final ProductActivity productActivity;

    public OrderWorkflowImpl() {
        var retryOptions = RetryOptions.newBuilder()
                .setInitialInterval(Duration.ofSeconds(1))
                .setMaximumInterval(Duration.ofSeconds(100))
                .setBackoffCoefficient(2)
                .setMaximumAttempts(5)
                .build();
        var defaultActivityOptions = ActivityOptions.newBuilder()
                // Timeout options specify when to automatically timeout Activities if the process is taking too long.
                .setStartToCloseTimeout(Duration.ofSeconds(5))
                // Optionally provide customized RetryOptions.
                // Temporal retries failures by default, this is simply an example.
                .setRetryOptions(retryOptions)
                .build();

        this.fraudActivity = Workflow.newActivityStub(FraudActivity.class, defaultActivityOptions);
        this.paymentActivity = Workflow.newActivityStub(PaymentActivity.class, defaultActivityOptions);
        this.productActivity = Workflow.newActivityStub(ProductActivity.class, defaultActivityOptions);
    }

    @Override
    public void approve(String orderId) {
        log.info("Trying to approve orderId {}", orderId);
        var score = this.fraudActivity.checkOrderForFraud(orderId);
        if (score > 70) {
            log.info("fraud score: {}, for orderId {}", score, orderId);
            var authorization = this.paymentActivity.authorize(orderId);
            log.info("authorization: {}, for orderId {}", authorization, orderId);
            var nsu = this.productActivity.deliver(orderId);
            log.info("nsu: {}, for orderId {}", nsu, orderId);
        } else {
            log.warn("Not approving!");
        }

    }
}
