package io.github.javiercanillas.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface RiskActivity {

    @ActivityMethod
    int scoreTransactionRisk(String transactionId);
}
