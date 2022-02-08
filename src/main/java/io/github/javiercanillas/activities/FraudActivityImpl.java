package io.github.javiercanillas.activities;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;

@Slf4j
@Component
public class FraudActivityImpl implements FraudActivity {

    private final Random rnd;

    public FraudActivityImpl() {
        this.rnd = new Random();
    }
    
    @Override
    public int checkOrderForFraud(String orderId) {
        log.trace("Running fraud checks for {}", orderId);
        Utils.sleepSilently(100L + rnd.nextInt(2000));

        if (rnd.nextBoolean()) {
            log.trace("Nothing wrong found on fraud checks for {}", orderId);
            return 1 + rnd.nextInt(99);
        } else {
            log.trace("Ups! Something wrong found on fraud checks for {}", orderId);
            throw new ActivityException("0001", "Couldn't run fraud checks!");
        }
    }
}
