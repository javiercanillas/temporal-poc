package io.github.javiercanillas.activities;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
public class ProductActivityImpl implements ProductActivity {

    private final Random rnd;

    public ProductActivityImpl() {
        this.rnd = new Random();
    }

    @Override
    public String deliver(String transactionId) {
        log.trace("Running product transaction delivery for {}", transactionId);
        Utils.sleepSilently(100L + rnd.nextInt(2000));
        if (rnd.nextBoolean()) {
            log.trace("Successful product delivery for {}", transactionId);
            return UUID.randomUUID().toString();
        } else {
            log.trace("Ups! Product delivery failure for {}", transactionId);
            throw new ActivityException("0001", "This product cannot be delivered");
        }
    }

    @Override
    public void recall(String nsu) {
        log.trace("Running product transaction recall for {}", nsu);
        Utils.sleepSilently(100L + rnd.nextInt(2000));
        if (rnd.nextBoolean()) {
            log.trace("Successful product recall for {}", nsu);
        } else {
            log.trace("Ups! Product recall failure for {}", nsu);
            throw new ActivityException("0002", "This product doesn't support recall");
        }
    }
}
