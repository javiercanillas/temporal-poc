package io.github.javiercanillas.workflows;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import lombok.Getter;

@WorkflowInterface
public interface TransactionWorkflow {

    @WorkflowMethod
    Status approve(String transactionId);

    @SignalMethod
    void notifyFulfilledSignal(Signal signal);

    @QueryMethod
    TransactionInformation getTransactionInformation();

    @Getter
    enum Status {
        UNKNOWN(false),
        IN_PROGRESS(false),
        APPROVED(true),
        AWAITING_SIGNAL(false),
        DECLINED(true),
        ;

        private boolean terminal;

        private Status(boolean terminal) {
            this.terminal = terminal;
        }
    }

    enum Signal {
        NONE,
        PENDING_RISK_VALIDATION,
        NEW_PAYMEMT_METHOD
    }
}
