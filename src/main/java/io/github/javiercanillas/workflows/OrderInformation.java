package io.github.javiercanillas.workflows;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@ToString
@Builder(setterPrefix = "with")
public final class OrderInformation {
    @Builder.Default
    private OrderWorkflow.Status status = OrderWorkflow.Status.UNKNOWN;
    @Builder.Default
    private Set<OrderWorkflow.Signal> awaitingSignals = new HashSet<>();
}
