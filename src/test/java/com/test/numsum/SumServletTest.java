package com.test.numsum;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SumServletTest extends Mockito {

    private static final Logger LOGGER = LoggerFactory.getLogger(SumServletTest.class);

    private LongAccumulator sum;
    private LongAdder nrThreadCount;
    private ExecutorService executorService;
    private CountDownLatch latch;
    private long expectedSum;

    @Before
    public void setup() {
        sum = new LongAccumulator(Long::sum, 0L);
        nrThreadCount = new LongAdder();
        executorService = Executors.newCachedThreadPool();
    }

    @Test
    public void testBasicPositive() throws Exception {
        latch = new CountDownLatch(3);
        expectedSum = 11;

        executeRequest("4", expectedSum, 0, false);
        executeRequest("7", expectedSum, 0, false);
        executeRequest("end", expectedSum, 1000, false);

        assertTrue("Some requests did not complete before timeout (5s)", latch.await(5, TimeUnit.SECONDS));

        latch = new CountDownLatch(1);
        expectedSum = 0;
        executeRequest("end", expectedSum, 1000, false);

        assertTrue("The request did not complete before timeout (2s)", latch.await(2, TimeUnit.SECONDS));
    }

    @Test
    public void testBasicNegative() throws Exception {
        latch = new CountDownLatch(3);
        expectedSum = -1;

        executeRequest("-8", expectedSum, 0, false);
        executeRequest("7", expectedSum, 0, false);
        executeRequest("end", expectedSum, 1000, false);

        assertTrue("Some requests did not complete before timeout (5s)", latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testNonNumber() throws Exception {
        latch = new CountDownLatch(3);
        expectedSum = 7;

        executeRequest("8s", expectedSum, 0, true);
        executeRequest("7", expectedSum, 0, false);
        executeRequest("end", expectedSum, 1000, false);

        assertTrue("Some requests did not complete before timeout (5s)", latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testEndNotFinalRequest() throws Exception {
        latch = new CountDownLatch(3);
        expectedSum = 8;

        executeRequest("8", expectedSum, 0, false);
        executeRequest("end", expectedSum, 1000, false);
        executeRequest("7", expectedSum, 1000, false);

        assertFalse("Some requests did not complete before timeout (5s)", latch.await(5, TimeUnit.SECONDS));
    }

    /**
     * Test that executes a random number of requests between 1 and 50.
     * Each request (last, "end" keyword request excluded) contains a number between -10000 and 10000 in their body.
     * Before each request, the thread is put to sleep for 0 to 1000 milliseconds,
     * to simulate the requests not arriving all at once
     * @throws Exception an exception
     */
    @Test
    public void testRandom() throws Exception {
        int nrOfRequests = 1 + (int) (Math.random() * (50 - 1));
        latch = new CountDownLatch(nrOfRequests + 1);
        List<Long> numbers = new ArrayList<>();

        IntStream.range(0, nrOfRequests).forEach(i -> {
            long randomNr = -10000 + (long) (Math.random() * (10000 - (-10000)));
            numbers.add(randomNr);
        });
        expectedSum = numbers.stream().mapToLong(Long::longValue).sum();
        numbers.forEach(nr -> {
            try {
                int sleepTime = (int) (Math.random() * 1000);
                executeRequest(String.valueOf(nr), expectedSum, sleepTime, false);
            } catch (IOException | InterruptedException e) {
                fail("Exception while executing request");
            }
        });
        executeRequest("end", expectedSum, 1000, false);
        assertTrue("Some requests did not complete before timeout (60s)", latch.await(60, TimeUnit.SECONDS));
        LOGGER.info("Number of requests: {}", nrOfRequests);
        LOGGER.info("Numbers of the requests: {}", numbers);
        LOGGER.info("Expected sum of numbers: {}", expectedSum);
    }

    /**
     * Method for creating and executing a simulated request
     * @param body the body of the request
     * @param expectedSum the value that is expected to be responded to the request
     * @param sleepTime the wait time before creating and executing the request
     * @param expectedFail is the request expected to fail with 400 Bad Request
     * @throws IOException an exception
     * @throws InterruptedException an exception
     */
    private void executeRequest(String body, long expectedSum, int sleepTime, boolean expectedFail) throws IOException, InterruptedException {
        // Wait a bit to simulate the request not arriving instantly
        Thread.sleep(sleepTime);

        // Create required mock objects
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        BufferedReader reader = new BufferedReader(new StringReader(body));
        PrintWriter writer = mock(PrintWriter.class);
        AsyncContext asyncContext = mock(AsyncContext.class);

        // Mock some required return values for the mock objects
        when(request.getReader()).thenReturn(reader);
        when(response.getWriter()).thenReturn(writer);
        when(asyncContext.getRequest()).thenReturn(request);
        when(asyncContext.getResponse()).thenReturn(response);

        if (expectedFail) {
            // This request is expected to fail with 400 Bad Request
            // When setStatus() is called on the mock response object, check the argument, it should be 400.
            doAnswer(invocation -> {
                assertEquals("Response status was expected to be 400", (int) invocation.getArgument(0), HttpServletResponse.SC_BAD_REQUEST);
                latch.countDown();
                return null;
            }).when(response).setStatus(ArgumentMatchers.anyInt());
        } else {
            // This request is expected to receive the sum as the response
            // When printLn() is called on the mock writer, check the argument, it should be the expected sum.
            doAnswer(invocation -> {
                assertEquals("The returned sum was not as expected", (long) invocation.getArgument(0), expectedSum);
                latch.countDown();
                return null;
            }).when(writer).println(ArgumentMatchers.anyLong());
        }
        // Run the simulated request
        executorService.execute(new Thread(() -> new SumServletWorker(asyncContext, sum, nrThreadCount).run()));
    }
}
