package com.github.sweintritt.jmus;

import java.io.File;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
    public static void main(final String[] args) {
        try {
            log.debug("checking parameters");
            if (args.length < 1 || StringUtils.isBlank(args[0])) {
                throw new IllegalArgumentException("No directory given");
            }

            final File file = new File(args[0]);
            if (!file.isDirectory()) {
                throw new IllegalArgumentException(file.getName() + " is not a directory");
            }

            final Application application = new Application();
            log.info("starting...");
            application.run(file);
        } catch (final Throwable e) {
            System.err.println(e.getMessage());
            log.error(e.getMessage(), e);
        }
    }
}
