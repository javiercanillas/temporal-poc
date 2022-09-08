package io.github.javiercanillas.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ActivityInterface
public interface PaymentActivity {

    @ActivityMethod
    AuthorisationResult authorize(String orderId);

    @ActivityMethod
    boolean cancelAuthorization(String authorizationId);

    @ActivityMethod
    void captureAuthorization(String authorizationId);

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    @Builder(setterPrefix = "with")
    class AuthorisationResult {
        private String authorizationId;
        private Status status;
    }

    enum Status {
        AUTHORISED,
        CC_ERROR
    }
}
