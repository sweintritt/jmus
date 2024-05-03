package com.github.sweintritt.jmus;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Slf4j
@Getter
@Setter
public class Application {

    private final Random random = new Random();
    private final UI ui = new UI();
    private List<File> files;
    private MediaPlayer player;
    private Media media;
    private boolean running;

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("No file or directory given");
            System.exit(1);
        }

        final File file = new File(args[0]);
        final List<File> files = new ArrayList<>();
        if (file.isFile()) {
            files.add(file);
        } else if (file.isDirectory()) {
            log.info("searching {}", file.getName());
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
        application.ui.enableRawMode();
        application.run();
    }

    private static void getAllFiles(final File dir, final List<File> result) {
        final File[] files = dir.listFiles();
        if(files != null) {
            for (final File file : files) {
                if (file.isFile()) {
                    result.add(file);
                } else if (file.isDirectory()) {
                    log.info("searching {}", file.getName());
                    getAllFiles(file, result);
                }
            }
        }
    }

    public void run() throws IOException {
        running = true;
        ui.addMessage("found " + files.size() + " audio files");
        next();
        while (running) {
            ui.draw();

            final Scanner scanner = new Scanner(System.in);
            final int key = System.in.read();

            switch (key) {
                case 'n':
                    next();
                    break;
                case 'p':
                    play();
                    break;
                case 's':
                    stop();
                    break;
                case 'q':
                    quit();
                    break;
                default:
                    ui.addMessage("unknown key " + key);
                    break;
            }
        }
    }

    private void next() {
        log.debug("selecting next file");
        Optional.ofNullable(player).ifPresent(MediaPlayer::stop);
        Optional.ofNullable(player).ifPresent(MediaPlayer::dispose);
        try {
            final int index = random.nextInt(files.size());
            final File file = files.get(index);
            log.info("playing {}", file.getName());
            ui.addMessage("playing " + file.getName());
            media = new Media(file.toURI().toString());
            player = new MediaPlayer(media);
            player.setOnEndOfMedia(this::setOnEndOfMedia);
            player.setVolume(0.5);
            player.play();
        } catch (final MediaException e) {
            if (!StringUtils.equals(e.getMessage(), "Unrecognized file signature!")) {
                log.error("Error during playback: {} ", e.getMessage(), e);
            }
        } catch (final Exception e) {
            log.error("Error during playback: {} ", e.getMessage(), e);
        }
    }

    private void setOnEndOfMedia() {
        next();
    }

    private void stop() {
        Optional.ofNullable(player).ifPresent(MediaPlayer::pause);
    }

    private void play() {
        Optional.ofNullable(player).ifPresent(MediaPlayer::play);
    }

    private void quit() {
        setRunning(false);
        Optional.ofNullable(player).ifPresent(MediaPlayer::stop);
        Platform.exit();
    }
}