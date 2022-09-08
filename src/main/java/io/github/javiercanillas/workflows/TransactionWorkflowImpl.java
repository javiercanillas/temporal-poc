package io.github.javiercanillas.workflows;

import io.github.javiercanillas.activities.RiskActivity;
import io.github.javiercanillas.activities.PaymentActivity;
import io.github.javiercanillas.activities.ProductActivity;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

public class TransactionWorkflowImpl implements TransactionWorkflow {

    private static final int MAX_PAYMENT_METHOD_CHANGE = 2;

    private final RiskActivity riskActivity;
    private final PaymentActivity paymentActivity;
    private final ProductActivity productActivity;
    private final Logger log;

    private TransactionInformation transactionInformation;

    public TransactionWorkflowImpl() {
        this.transactionInformation = TransactionInformation.builder().build();
        this.log = Workflow.getLogger(TransactionWorkflowImpl.class);
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

        this.riskActivity = Workflow.newActivityStub(RiskActivity.class, defaultActivityOptions);
        this.paymentActivity = Workflow.newActivityStub(PaymentActivity.class, defaultActivityOptions);
        this.productActivity = Workflow.newActivityStub(ProductActivity.class, defaultActivityOptions);
    }

    @Override
    public Status approve(String transactionId) {
        this.transactionInformation.setStatus(Status.IN_PROGRESS);
        log.info("Trying to approve transactionId {}", transactionId);
        var saga = new Saga(new Saga.Options.Builder()
                .setContinueWithError(false)
                .setParallelCompensation(true)
                .build());

        var score = this.riskActivity.scoreTransactionRisk(transactionId);
        log.info("risk score: {}, for transactionId {}", score, transactionId);

        if (score > 70) {
            collectAndDeliver(transactionId, saga);
        } else if (score > 30) {
            log.info("Risk score low! Requiring validation to approve transactionId: {}", transactionId);
            this.transactionInformation.setStatus(Status.AWAITING_SIGNAL);
            this.transactionInformation.getAwaitingSignals().add(Signal.PENDING_RISK_VALIDATION);

            Workflow.await(Duration.ofMinutes(1L), () -> !this.transactionInformation.getAwaitingSignals().contains(Signal.PENDING_RISK_VALIDATION));

            if (!this.transactionInformation.getAwaitingSignals().contains(Signal.PENDING_RISK_VALIDATION)) {
                this.transactionInformation.setStatus(Status.IN_PROGRESS);
                collectAndDeliver(transactionId, saga);
            } else {
                log.warn("Didn't complete risk validation! Not approving transactionId: {}", transactionId);
                this.transactionInformation.setStatus(Status.DECLINED);
            }
        } else {
            log.warn("Risk score too low! Not approving transactionId: {}", transactionId);
            this.transactionInformation.setStatus(Status.DECLINED);
        }

        return this.transactionInformation.getStatus();
    }

    @Override
    public void notifyFulfilledSignal(Signal signal) {
        if (Status.AWAITING_SIGNAL.equals(this.transactionInformation.getStatus())) {
            this.transactionInformation.getAwaitingSignals().remove(signal);
        }
    }

    @Override
    public TransactionInformation getTransactionInformation() {
        return this.transactionInformation;
    }

    private void collectAndDeliver(String transactionId, Saga saga) {
        PaymentActivity.AuthorisationResult authorisationResult = null;
        try {
            authorisationResult = authorizePayment(transactionId, saga);
            log.info("authorisationResult: {}, for transactionId {}", authorisationResult, transactionId);

            if (PaymentActivity.Status.AUTHORISED.equals(authorisationResult.getStatus())) {
                var nsu = deliverProduct(transactionId, saga);
                log.info("nsu: {}, for transactionId {}", nsu, transactionId);
                this.transactionInformation.setStatus(Status.APPROVED);
            } else {
                log.info("CC not authorised! Not delivering product and declining transactionId: {}", transactionId);
                this.transactionInformation.setStatus(Status.DECLINED);
            }
        } catch (RuntimeException e) {
            log.error("Ups! Something went wrong", e);
            saga.compensate();
            this.transactionInformation.setStatus(Status.DECLINED);
        }

        if (Status.APPROVED.equals(this.transactionInformation.getStatus()) && (authorisationResult != null)) {
            captureAuthorisation(authorisationResult.getAuthorisationId());
            log.info("Captured authorisationResult {}, for transactionId {}", authorisationResult, transactionId);
        }

    }

    private void captureAuthorisation(String authorisationId) {
        this.paymentActivity.captureAuthorisation(authorisationId);
    }

    private String deliverProduct(String transactionId, Saga saga) {
        var nsu = this.productActivity.deliver(transactionId);
        saga.addCompensation(this.productActivity::recall, nsu);
        return nsu;
    }

    private PaymentActivity.AuthorisationResult authorizePayment(String transactionId, Saga saga) {
        var tries = 0;
        PaymentActivity.AuthorisationResult authorisationResult = new PaymentActivity.AuthorisationResult();
        do {
            tries++;
            Workflow.await(Duration.ofMinutes(1L), () -> !this.transactionInformation.getAwaitingSignals().contains(Signal.NEW_PAYMEMT_METHOD));
            if (!this.transactionInformation.getAwaitingSignals().contains(Signal.NEW_PAYMEMT_METHOD)) {
                this.transactionInformation.setStatus(Status.IN_PROGRESS);
                authorisationResult = this.paymentActivity.authorize(transactionId);
                if (PaymentActivity.Status.AUTHORISED.equals(authorisationResult.getStatus())) {
                    saga.addCompensation(this.paymentActivity::cancelAuthorisation, authorisationResult.getAuthorisationId());
                } else {
                    log.info("CC error! Requiring new payment method to approve transactionId: {}", transactionId);
                    this.transactionInformation.setStatus(Status.AWAITING_SIGNAL);
                    this.transactionInformation.getAwaitingSignals().add(Signal.NEW_PAYMEMT_METHOD);
                }
            }
        } while (tries < MAX_PAYMENT_METHOD_CHANGE && !PaymentActivity.Status.AUTHORISED.equals(authorisationResult.getStatus()));

        if (tries < MAX_PAYMENT_METHOD_CHANGE && !PaymentActivity.Status.AUTHORISED.equals(authorisationResult.getStatus())) {
            log.info("Reached max attempts to authorize payment to approve transactionId: {}", transactionId);
        }

        return authorisationResult;
    }
}
