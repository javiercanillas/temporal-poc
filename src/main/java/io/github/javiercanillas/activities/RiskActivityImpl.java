package io.github.javiercanillas.activities;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;

@Slf4j
@Component
public class RiskActivityImpl implements RiskActivity {

    private final Random rnd;

    public RiskActivityImpl() {
        this.rnd = new Random();
    }
    
    @Override
    public int scoreTransactionRisk(String transactionId) {
        log.trace("Running risk checks for {}", transactionId);
        Utils.sleepSilently(100L + rnd.nextInt(2000));

        if (rnd.nextBoolean()) {
            log.trace("Nothing wrong found on risk checks for {}", transactionId);
            return 1 + rnd.nextInt(99);
        } else {
            log.trace("Ups! Something wrong found on risk checks for {}", transactionId);
            throw new ActivityException("0001", "Couldn't run risk checks!");
        }
    }
}
