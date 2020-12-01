package com.test.numsum;

import javax.servlet.AsyncContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

/**
 * The main servlet class. Receives POST requests and asynchronously handles them.
 */
@WebServlet(name = "SumServlet", loadOnStartup = 1, asyncSupported = true, value = "/")
public class SumServlet extends HttpServlet {

    private final LongAccumulator sum = new LongAccumulator(Long::sum, 0L);
    private final LongAdder nrThreadCount = new LongAdder();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {

        final AsyncContext asyncContext = req.startAsync(req, resp);
        ExecutorService executor = (ExecutorService) req.getServletContext().getAttribute("executor");
        executor.execute(new SumServletWorker(asyncContext, sum, nrThreadCount));
    }
}
