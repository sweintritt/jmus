package com.github.sweintritt.jmus;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;

@Slf4j
@Getter
@Setter
public class Application implements Runnable {

    private List<Command> commands;
    private List<File> files;
    private MediaPlayer player;
    private Media media;
    private boolean running;

    public static void main(String[] args) {
        if (args.length < 1) {
            log.error("No file or directory given");
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
            // TODO Better return code
            System.exit(1);
        }

        final Application application = new Application();
        application.setFiles(files);
        application.commands = List.of(new QuitCommand(application), new NextCommand(application));
        log.info("starting");
        Platform.startup(application);
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

    @Override
    public void run() {
        running = true;
        nextFile();
        while (running) {
            final Scanner scanner = new Scanner(System.in);
            log.info("jmus> ");
            final String input = scanner.nextLine();
            final String[] chunks = input.split(" ");

            if (StringUtils.isNotBlank(input)) {
                final String cmd = chunks[0];
                for (final Command c : commands) {
                    if (c.getName().equals(cmd) || c.getAliases().contains(cmd)) {
                        try {
                            c.execute(Arrays.copyOfRange(chunks, 1, chunks.length));
                        } catch (final ParseException e) {
                            log.error(e.getMessage());
                        }
                    }
                }
            }
        }
    }

    public void nextFile() {
        log.debug("selecting next file");
        Optional.ofNullable(player).ifPresent(MediaPlayer::stop);
        Optional.ofNullable(player).ifPresent(MediaPlayer::dispose);
        play();
    }

    public void setOnEndOfMedia() {
        nextFile();
    }

    private void play() {
        try {
            final int index = (int) Math.floor(Math.random()*files.size());
            final File file = files.get(index);
            log.info("playing {}", file.getName());
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
}