package io.github.javiercanillas.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface ProductActivity {

    @ActivityMethod
    String deliver(String orderId);
}
