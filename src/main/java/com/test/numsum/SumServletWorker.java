package com.test.numsum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.stream.Collectors;

/**
 * Class where each async POST request is handled
 */
public class SumServletWorker implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SumServletWorker.class);

    private final AsyncContext asyncContext;
    private final LongAccumulator sum;
    private final Phaser ph;
    private final AtomicReference<String> idRef;

    /**
     * Creates a new instance of the handler
     *
     * @param asyncContext the context of the async request
     * @param sum          accumulator for the total sum of numbers
     * @param ph           phaser for controlling the flow of responses
     * @param idRef        atomic reference to the "id" string
     */
    public SumServletWorker(AsyncContext asyncContext, LongAccumulator sum, Phaser ph, AtomicReference<String> idRef) {
        this.asyncContext = asyncContext;
        this.sum = sum;
        this.ph = ph;
        this.idRef = idRef;
    }

    @Override
    public void run() {
        HttpServletRequest request = (HttpServletRequest) asyncContext.getRequest();
        HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();

        try {
            String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            if (body.startsWith("end ")) {
                String id = body.substring(3);
                idRef.set(id);
                // Notify the "number" threads to respond to their request with the "sum" value
                ph.arriveAndAwaitAdvance();
                // Wait until "number" threads have returned the "sum" value
                ph.arriveAndAwaitAdvance();
                // Respond to the request with the "sum" value and also reset the sum accumulator
                LOGGER.debug("Received \"end\", sum is {}, id is {}", sum, id);
                response.getWriter().print(sum.getThenReset() + idRef.getAndSet(""));
                ph.arrive();
            } else {
                ph.register();
                long number = Long.parseLong(body);
                LOGGER.debug("Received a number: {}", number);
                // Body appeared to be a valid number,
                // Add the parsed number to the sum of received numbers
                sum.accumulate(number);
                // Wait until the "end" keyword is received and
                // the final sum can be returned as the response of this request
                ph.arriveAndAwaitAdvance();
                response.getWriter().print(sum.get() + idRef.get());
                ph.arriveAndDeregister();
            }
            // Set response status to 200 OK, as everything went as expected
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (NumberFormatException e) {
            // The request body contained an invalid number,
            // respond with 400 Bad Request.
            LOGGER.debug("Received an invalid number/not \"end\", {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ph.arriveAndDeregister();
        } catch (IOException e) {
            LOGGER.error("Unexpected exception", e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ph.arriveAndDeregister();
        } finally {
            // End the async request operation
            asyncContext.complete();
        }
    }
}
