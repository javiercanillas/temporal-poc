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
        log.trace("Running payment order authorisation for {}", orderId);
        Utils.sleepSilently(100L + rnd.nextInt(2000));

        if (rnd.nextBoolean()) {
            if (rnd.nextBoolean()) {
                log.trace("All payment intents for {} are authorized", orderId);
                return AuthorisationResult.builder()
                        .withAuthorisationId(UUID.randomUUID().toString())
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
    public boolean cancelAuthorisation(String authorisationId) {
        log.trace("Running payment order cancellation for {}", authorisationId);
        Utils.sleepSilently(100L + rnd.nextInt(2000));

        if (rnd.nextBoolean()) {
            log.trace("All payment intents for {} are cancelled", authorisationId);
            return true;
        } else {
            log.trace("Ups! Payment cancellation error for {}", authorisationId);
            throw new ActivityException("0001", "Payment cancellation error!");
        }
    }

    @Override
    public void captureAuthorisation(String authorisationId) {
        log.trace("Running payment order capture for {}", authorisationId);
        Utils.sleepSilently(100L + rnd.nextInt(2000));

        if (rnd.nextBoolean()) {
            log.trace("All payment intents for {} are captured", authorisationId);
        } else {
            log.trace("Ups! Payment capture error for {}", authorisationId);
            throw new ActivityException("0001", "Payment capture error!");
        }
    }
}
