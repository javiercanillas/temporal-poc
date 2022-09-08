package io.github.javiercanillas.activities;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
public class PaymentActivityImpl implements PaymentActivity {
    
    private final Random rnd;

    public PaymentActivityImpl() {
        this.rnd = new Random();
    }

    @Override
    public AuthorisationResult authorize(String orderId) {
        log.trace("Running payment order authorization for {}", orderId);
        Utils.sleepSilently(100L + rnd.nextInt(2000));

        if (rnd.nextBoolean()) {
            if (rnd.nextBoolean()) {
                log.trace("All payment intents for {} are authorized", orderId);
                return AuthorisationResult.builder()
                        .withAuthorizationId(UUID.randomUUID().toString())
                        .withStatus(Status.AUTHORISED)
                        .build();
            } else {
                log.trace("Ups! Payment declined for {}", orderId);
                return AuthorisationResult.builder()
                        .withStatus(Status.CC_ERROR)
                        .build();
            }
        } else {
            log.trace("Ups! There was an error processing payment method for {}", orderId);
            throw new ActivityException("0001", "There was an error processing payment method");
        }
    }

    @Override
    public boolean cancelAuthorization(String authorizationId) {
        log.trace("Running payment order cancellation for {}", authorizationId);
        Utils.sleepSilently(100L + rnd.nextInt(2000));

        if (rnd.nextBoolean()) {
            log.trace("All payment intents for {} are cancelled", authorizationId);
            return true;
        } else {
            log.trace("Ups! Payment cancellation error for {}", authorizationId);
            throw new ActivityException("0001", "Payment cancellation error!");
        }
    }

    @Override
    public void captureAuthorization(String authorizationId) {
        log.trace("Running payment order capture for {}", authorizationId);
        Utils.sleepSilently(100L + rnd.nextInt(2000));

        if (rnd.nextBoolean()) {
            log.trace("All payment intents for {} are captured", authorizationId);
        } else {
            log.trace("Ups! Payment capture error for {}", authorizationId);
            throw new ActivityException("0001", "Payment capture error!");
        }
    }
}
