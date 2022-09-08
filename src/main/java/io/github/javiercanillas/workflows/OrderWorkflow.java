package io.github.javiercanillas.workflows;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import lombok.Getter;

@WorkflowInterface
public interface OrderWorkflow {

    @WorkflowMethod
    Status approve(String orderId);

    @SignalMethod
    void notifyFulfilledSignal(Signal signal);

    @QueryMethod
    OrderInformation getOrderInformation();

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
        PENDING_KYC,
        NEW_PAYMEMT_METHOD
    }
}
