package com.test.numsum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * Helper class for creating (and resetting) an application-wide {@link CountDownLatch}
 */
public class SumLatch {

    private static final Logger LOGGER = LoggerFactory.getLogger(SumLatch.class);

    private static CountDownLatch latch;

    private SumLatch() {
    }

    /**
     * Getter for the CountDownLatch
     * @return the latch
     */
    public static CountDownLatch getLatch() { return latch; }

    /**
     * Creates a new {@link CountDownLatch} with the specified count.
     * Basically resets the application-wide latch.
     * Must be called before first using this latch
     * @param nr the new count for the latch
     */
    public static void create(int nr) {
        LOGGER.debug("Created a new latch with value: {}", nr);
        latch = new CountDownLatch(nr);
    }
}
