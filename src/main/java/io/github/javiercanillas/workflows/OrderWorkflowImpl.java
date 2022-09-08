package io.github.javiercanillas.workflows;

import io.github.javiercanillas.activities.FraudActivity;
import io.github.javiercanillas.activities.PaymentActivity;
import io.github.javiercanillas.activities.ProductActivity;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

public class OrderWorkflowImpl implements OrderWorkflow {

    private static final int MAX_PAYMENT_METHOD_CHANGE = 2;

    private final FraudActivity fraudActivity;
    private final PaymentActivity paymentActivity;
    private final ProductActivity productActivity;
    private final Logger log;

    private OrderInformation orderInformation;

    public OrderWorkflowImpl() {
        this.orderInformation = OrderInformation.builder().build();
        this.log = Workflow.getLogger(OrderWorkflowImpl.class);
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
    public Status approve(String orderId) {
        this.orderInformation.setStatus(Status.IN_PROGRESS);
        log.info("Trying to approve orderId {}", orderId);
        var saga = new Saga(new Saga.Options.Builder()
                .setContinueWithError(false)
                .setParallelCompensation(true)
                .build());

        var score = this.fraudActivity.checkOrderForFraud(orderId);
        log.info("fraud score: {}, for orderId {}", score, orderId);

        if (score > 70) {
            collectAndDeliver(orderId, saga);
        } else if (score > 30) {
            log.info("Fraud score low! Requiring KYC to approve orderId: {}", orderId);
            this.orderInformation.setStatus(Status.AWAITING_SIGNAL);
            this.orderInformation.getAwaitingSignals().add(Signal.PENDING_KYC);

            Workflow.await(Duration.ofMinutes(1L), () -> !this.orderInformation.getAwaitingSignals().contains(Signal.PENDING_KYC));

            if (!this.orderInformation.getAwaitingSignals().contains(Signal.PENDING_KYC)) {
                this.orderInformation.setStatus(Status.IN_PROGRESS);
                collectAndDeliver(orderId, saga);
            } else {
                log.warn("Didn't complete KYC! Not approving orderId: {}", orderId);
                this.orderInformation.setStatus(Status.DECLINED);
            }
        } else {
            log.warn("Fraud score too low! Not approving orderId: {}", orderId);
            this.orderInformation.setStatus(Status.DECLINED);
        }

        return this.orderInformation.getStatus();
    }

    @Override
    public void notifyFulfilledSignal(Signal signal) {
        if (Status.AWAITING_SIGNAL.equals(this.orderInformation.getStatus())) {
            this.orderInformation.getAwaitingSignals().remove(signal);
        }
    }

    @Override
    public OrderInformation getOrderInformation() {
        return this.orderInformation;
    }

    private void collectAndDeliver(String orderId, Saga saga) {
        PaymentActivity.AuthorisationResult authorisationResult = null;
        try {
            authorisationResult = authorizePayment(orderId, saga);
            log.info("authorisationResult: {}, for orderId {}", authorisationResult, orderId);

            if (PaymentActivity.Status.AUTHORISED.equals(authorisationResult.getStatus())) {
                var nsu = deliverProduct(orderId, saga);
                log.info("nsu: {}, for orderId {}", nsu, orderId);
                this.orderInformation.setStatus(Status.APPROVED);
            } else {
                log.info("CC not authorised! Not delivering product and declining orderId: {}", orderId);
                this.orderInformation.setStatus(Status.DECLINED);
            }
        } catch (RuntimeException e) {
            log.error("Ups! Something went wrong", e);
            saga.compensate();
            this.orderInformation.setStatus(Status.DECLINED);
        }

        if (Status.APPROVED.equals(this.orderInformation.getStatus()) && (authorisationResult != null)) {
            captureAuthorisation(authorisationResult.getAuthorisationId());
            log.info("Captured authorisationResult {}, for orderId {}", authorisationResult, orderId);
        }

    }

    private void captureAuthorisation(String authorisationId) {
        this.paymentActivity.captureAuthorisation(authorisationId);
    }

    private String deliverProduct(String orderId, Saga saga) {
        var nsu = this.productActivity.deliver(orderId);
        saga.addCompensation(this.productActivity::recall, nsu);
        return nsu;
    }

    private PaymentActivity.AuthorisationResult authorizePayment(String orderId, Saga saga) {
        var tries = 0;
        PaymentActivity.AuthorisationResult authorisationResult = new PaymentActivity.AuthorisationResult();
        do {
            tries++;
            Workflow.await(Duration.ofMinutes(1L), () -> !this.orderInformation.getAwaitingSignals().contains(Signal.NEW_PAYMEMT_METHOD));
            if (!this.orderInformation.getAwaitingSignals().contains(Signal.NEW_PAYMEMT_METHOD)) {
                this.orderInformation.setStatus(Status.IN_PROGRESS);
                authorisationResult = this.paymentActivity.authorize(orderId);
                if (PaymentActivity.Status.AUTHORISED.equals(authorisationResult.getStatus())) {
                    saga.addCompensation(this.paymentActivity::cancelAuthorisation, authorisationResult.getAuthorisationId());
                } else {
                    log.info("CC error! Requiring new payment method to approve orderId: {}", orderId);
                    this.orderInformation.setStatus(Status.AWAITING_SIGNAL);
                    this.orderInformation.getAwaitingSignals().add(Signal.NEW_PAYMEMT_METHOD);
                }
            }
        } while (tries < MAX_PAYMENT_METHOD_CHANGE && !PaymentActivity.Status.AUTHORISED.equals(authorisationResult.getStatus()));

        if (tries < MAX_PAYMENT_METHOD_CHANGE && !PaymentActivity.Status.AUTHORISED.equals(authorisationResult.getStatus())) {
            log.info("Reached max attempts to authorize payment to approve orderId: {}", orderId);
        }

        return authorisationResult;
    }
}
