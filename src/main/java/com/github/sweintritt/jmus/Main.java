package com.github.sweintritt.jmus;

import java.io.File;

import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
    public static void main(String[] args) {
        try {
            log.debug("checking parameters");
            if (args.length < 1) {
                System.err.println("No directory given");
                System.exit(1);
            }

            final File file = new File(args[0]);
            if (!file.isDirectory()) {
                log.error("{} is no directory", file.getName());
                System.exit(1);
            }

            final Application application = new Application();
            application.setDirectory(file);
            log.info("starting...");
            Platform.startup(() -> log.info("initializing javafx"));
            application.run();
        }    catch (final Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();    
        }
    }
}
