package com.test.numsum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Servlet context listener class.
 * Creates a new {@link ExecutorService} when context is initialized
 * and shuts it down when context is destroyed
 */
@WebListener
public class SumServletContextListener implements ServletContextListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SumServletContextListener.class);

    public void contextInitialized(ServletContextEvent servletContextEvent) {
        LOGGER.debug("Servlet context initialized");
        ExecutorService executor = Executors.newCachedThreadPool();
        servletContextEvent.getServletContext().setAttribute("executor", executor);

    }

    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        LOGGER.debug("Servlet context destroyed");
        ThreadPoolExecutor executor = (ThreadPoolExecutor) servletContextEvent
                .getServletContext().getAttribute("executor");
        executor.shutdown();
    }

}
