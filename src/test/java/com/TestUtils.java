package com;

import java.io.StringWriter;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

public class TestUtils {

    private static final StringWriter TEST_WRITER = new StringWriter();
    private static final WriterAppender TEST_APPENDER = WriterAppender.newBuilder()
            .setTarget(TEST_WRITER)
            .setLayout(PatternLayout.newBuilder().withPattern("%p: %m%n").build())
            .setName("TestAppender")
            .build();

    public static void addTestAppender() {
        getRootLoggerConfig().addAppender(TEST_APPENDER, Level.ALL, null);
    }

    public static void removeTestAppender() {
        getRootLoggerConfig().removeAppender(TEST_APPENDER.getName());
    }

    private static LoggerConfig getRootLoggerConfig() {
        final LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        final Configuration configuration = loggerContext.getConfiguration();
        return configuration.getLoggerConfig(StringUtils.EMPTY);
    }

    public static StringBuffer getLog() {
        return TEST_WRITER.getBuffer();
    }

    public static void resetLog() {
        TEST_WRITER.getBuffer().delete(0, TEST_WRITER.getBuffer().length());
    } 
}
