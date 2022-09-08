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
    AuthorisationResult authorize(String transactionId);

    @ActivityMethod
    boolean cancelAuthorisation(String authorisationId);

    @ActivityMethod
    void captureAuthorisation(String authorisationId);

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    @Builder(setterPrefix = "with")
    class AuthorisationResult {
        private String authorisationId;
        private Status status;
    }

    enum Status {
        AUTHORISED,
        CC_ERROR
    }
}
