package com.github.sweintritt.jmus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
        public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("No file or directory given");
            System.exit(1);
        }

        final File file = new File(args[0]);
        final List<File> files = new ArrayList<>();
        if (file.isFile()) {
            files.add(file);
        } else if (file.isDirectory()) {
            getAllFiles(file, files);
            log.info("found {} files", files.size());
        } else {
            log.error("{} is no file or directory", file.getName());
            System.exit(1);
        }

        final Application application = new Application();
        application.setFiles(files);
        log.info("starting");
        Platform.startup(() -> log.info("initializing javafx"));
        application.enableRawMode();
        application.run();
    }


    private static void getAllFiles(final File dir, final List<File> result) {
        log.info("searching {}", dir.getName());
        final File[] files = dir.listFiles();
        if(files != null) {
            for (final File file : files) {
                // For now just mp3s is fine
                if (file.isFile() && StringUtils.endsWithIgnoreCase(file.getName(), ".mp3")) {
                    result.add(file);
                } else if (file.isDirectory()) {
                    getAllFiles(file, result);
                }
            }
        }
    }
}
