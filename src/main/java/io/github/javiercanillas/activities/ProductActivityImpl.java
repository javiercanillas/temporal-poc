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
    public String deliver(String orderId) {
        log.trace("Running product order delivery for {}", orderId);
        Utils.sleepSilently(100L + rnd.nextInt(2000));
        if (rnd.nextBoolean()) {
            log.trace("Successful product delivery for {}", orderId);
            return UUID.randomUUID().toString();
        } else {
            log.trace("Ups! Product delivery failure for {}", orderId);
            throw new ActivityException("0001", "This product cannot be delivered");
        }
    }
}
