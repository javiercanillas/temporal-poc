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
    public String authorize(String orderId) {
        log.trace("Running payment order authorization for {}", orderId);
        Utils.sleepSilently(100L + rnd.nextInt(2000));

        if (rnd.nextBoolean()) {
            log.trace("All payment intents for {} are authorized", orderId);
            return UUID.randomUUID().toString();
        } else {
            log.trace("Ups! Payment declined for {}", orderId);
            throw new ActivityException("0001", "Payment declined! Insufficient funds");
        }
    }

    @Override
    public boolean cancelAuthorization(String orderId, String authorizationId) {
        log.trace("Running payment order cancellation for {}", orderId);
        Utils.sleepSilently(100L + rnd.nextInt(2000));

        if (rnd.nextBoolean()) {
            log.trace("All payment intents for {} are cancelled", orderId);
            return true;
        } else {
            log.trace("Ups! Payment cancellation error for {}", orderId);
            throw new ActivityException("0001", "Payment cancellation error!");
        }
    }

    @Override
    public boolean captureAuthorization(String orderId, String authorizationId) {
        log.trace("Running payment order capture for {}", orderId);
        Utils.sleepSilently(100L + rnd.nextInt(2000));

        if (rnd.nextBoolean()) {
            log.trace("All payment intents for {} are captured", orderId);
            return true;
        } else {
            log.trace("Ups! Payment capture error for {}", orderId);
            throw new ActivityException("0001", "Payment capture error!");
        }
    }
}
