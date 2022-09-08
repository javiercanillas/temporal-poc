package io.github.javiercanillas.workflows;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@ToString
@Builder(setterPrefix = "with")
public final class TransactionInformation {
    @Builder.Default
    private TransactionWorkflow.Status status = TransactionWorkflow.Status.UNKNOWN;
    @Builder.Default
    private Set<TransactionWorkflow.Signal> awaitingSignals = new HashSet<>();
}
