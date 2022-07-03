package com.test.numsum;

import javax.servlet.AsyncContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAccumulator;

/**
 * The main servlet class. Receives POST requests and asynchronously handles them.
 */
@WebServlet(name = "SumServlet", loadOnStartup = 1, asyncSupported = true, value = "/")
public class SumServlet extends HttpServlet {

    private final LongAccumulator sum = new LongAccumulator(Long::sum, 0L);
    private final String id = "";
    private final AtomicReference<String> idRef = new AtomicReference<>(id);
    private final Phaser ph = new Phaser(1);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {

        final AsyncContext asyncContext = req.startAsync(req, resp);
        asyncContext.setTimeout(-1);
        asyncContext.start(new SumServletWorker(asyncContext, sum, ph, idRef));
    }
}
