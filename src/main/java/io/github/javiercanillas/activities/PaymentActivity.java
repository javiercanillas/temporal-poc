package io.github.javiercanillas.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface PaymentActivity {

    @ActivityMethod
    String authorize(String orderId);

    @ActivityMethod
    boolean cancelAuthorization(String orderId, String authorizationId);

    @ActivityMethod
    boolean captureAuthorization(String orderId, String authorizationId);
}
