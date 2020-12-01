package com.test.numsum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

/**
 * Class where each async POST request is handled
 */
public class SumServletWorker implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SumServletWorker.class);

    private final AsyncContext asyncContext;
    private final LongAccumulator sum;
    private final LongAdder nrThreadCount;

    /**
     * Creates a new instance of the handler
     * @param asyncContext the context of the async request
     * @param sum accumulator for the total sum of numbers
     * @param nrThreadCount counter for the threads with a valid number in the body
     */
    public SumServletWorker(AsyncContext asyncContext, LongAccumulator sum, LongAdder nrThreadCount) {
        this.asyncContext = asyncContext;
        this.sum = sum;
        this.nrThreadCount = nrThreadCount;
    }

    @Override
    public void run() {
        // Get request and response from the async context.
        HttpServletRequest request = (HttpServletRequest) asyncContext.getRequest();
        HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();

        try {
            // Read the body of the POST request.
            String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            if ("end".equals(body)) {
                // "end" keyword received,
                // create a new latch with the value being the number of threads with a number in the body.
                SumLatch.create(nrThreadCount.intValue());

                synchronized (sum) {
                    // Reset the thread counter, notifying the "number" threads to
                    // respond to their request with the "sum" value.
                    nrThreadCount.reset();
                    sum.notifyAll();
                }
                // Wait at the latch for the "number" threads to prepare their response.
                SumLatch.getLatch().await();
                // Respond to the request with the "sum" value and also reset the sum accumulator
                LOGGER.debug("Received \"end\", sum is {}", sum);
                response.getWriter().println(sum.getThenReset());
            } else {
                // Parse the request body. A number is expected.
                long number = Long.parseLong(body);
                LOGGER.debug("Received a number: {}", number);
                // Body appeared to be a valid number,
                // increment the counter counting the threads with a number in the body.
                nrThreadCount.increment();
                // Add the parsed number to the sum of received numbers
                sum.accumulate(number);

                synchronized (sum) {
                    // Wait until the "end" keyword is received and
                    // the final sum can be returned as the response of this request
                    while (nrThreadCount.intValue() != 0) {
                        sum.wait();
                    }
                    response.getWriter().println(sum.get());
                    // Count down the latch to notify that this request is ready to respond with the sum
                    SumLatch.getLatch().countDown();
                }
            }
            // Set response status to 200 OK, as everything went as expected
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (NumberFormatException e) {
            // The request body contained an invalid number,
            // respond with 400 Bad Request.
            LOGGER.debug("Received an invalid number/not \"end\", {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Unexpected exception", e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } finally {
            // End the async request operation
            asyncContext.complete();
        }
    }
}
