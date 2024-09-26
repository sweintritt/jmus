package com.github.sweintritt.jmus;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
@Setter
public class Application {

    private static final String STATUS = "[ jmus %s | %d files | vol:%d | %s ] (q)uit, (s)top, (p)lay, (n)ext, (+)volume, (-)volume";

    private final Random random = new Random();
    private final List<Entry> entries = new LinkedList<>();
    private Entry entry;
    private State state = State.SEARCHING;
    private String version;
    /**
     * Backup of the original values
     */
    private LibC.Termios backup;
    /**
     * Root directory to scan for music
     */
    private File directory;
    private MediaPlayer player;
    private Media media;
    private boolean running;

    public void run() {
        try {
            enableRawMode();
            log.info("scanning for files");
            loadFiles(directory);

            // Load Mp3 id tags async
            // TODO parallel with more threads
            CompletableFuture.runAsync(() -> entries.forEach(Entry::loadMp3Tags));

            log.info("found {} files", entries.size());
            state = State.STOPPED;

            running = true;
            next();
            while (running) {
                final int key = System.in.read();
                handleKey(key);
            }
        } catch (final Exception e) {
            quit();
            log.error(e.getMessage(), e);
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void loadFiles(final File dir) {
        log.info("searching {}", dir.getName());
        final File[] files = dir.listFiles();
        if (files != null) {
            for (final File file : files) {
                // For now just mp3s is fine
                if (file.isFile() && StringUtils.endsWithIgnoreCase(file.getName(), ".mp3")) {
                    entries.add(new Entry(file));
                } else if (file.isDirectory()) {
                    loadFiles(file);
                }
            }
        }
    }

    public void handleKey(final int key) {
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
            case '+':
                player.setVolume(Math.min(player.getVolume() + 0.1, 1.0));
                draw();
                break;
            case '-':
                player.setVolume(Math.max(player.getVolume() - 0.1, 0.0));
                draw();
                break;
            default:
                break;
        }
    }

    public void next() {
        log.debug("selecting next file");
        Optional.ofNullable(player).ifPresent(MediaPlayer::stop);
        Optional.ofNullable(player).ifPresent(MediaPlayer::dispose);
        try {
            entry = getNextEntry();
            log.info("playing {}", entry.getFile().getName());
            player = new MediaPlayer(new Media(entry.getFile().toURI().toString()));
            player.setOnEndOfMedia(this::next);
            player.setVolume(0.5);
            play();
            state = State.PLAYING;
            player.setOnReady(this::draw);
        } catch (final MediaException e) {
            if (!StringUtils.equals(e.getMessage(), "Unrecognized file signature!")) {
                log.error("Error during playback: {} ", e.getMessage(), e);
            }
        } catch (final Exception e) {
            log.error("Error during playback: {} ", e.getMessage(), e);
        }
    }

    public Entry getNextEntry() {
        return entries.stream()
                .skip(random.nextInt(entries.size()))
                .findFirst()
                .orElse(null);
    }

    public void stop() {
        Optional.ofNullable(player).ifPresent(MediaPlayer::pause);
        state = State.STOPPED;
    }

    public void play() {
        Optional.ofNullable(player).ifPresent(MediaPlayer::play);
        state = State.PLAYING;
    }

    public void quit() {
        disableRawMode();
        setRunning(false);
        Optional.ofNullable(player).ifPresent(MediaPlayer::stop);
        Platform.exit();
    }

    public LibC.Winsize getWindowsize() {
        final LibC.Winsize winsize = new LibC.Winsize();
        final int rc = LibC.INSTANCE.ioctl(LibC.SYSTEM_OUT_FD, LibC.TIOCGWINSZ, winsize);
        if (rc != 0) {
            throw new IllegalStateException("error calling libc.ioctl rc: " + rc);
        }
        return winsize;
    }

    public void enableRawMode() {
        log.debug("enable terminal raw mode");
        final LibC.Termios termios = new LibC.Termios();
        int rc = LibC.INSTANCE.tcgetattr(LibC.SYSTEM_OUT_FD, termios);
        if (rc != 0) {
            throw new IllegalStateException("error calling libc.tcgetattr rc: " + rc);
        }
        backup = LibC.Termios.of(termios);
        termios.c_lflag &= ~(LibC.ECHO | LibC.ICANON | LibC.IEXTEN | LibC.ISIG);
        termios.c_iflag &= ~(LibC.IXON | LibC.ICRNL);
        termios.c_oflag &= ~(LibC.OPOST);

        rc = LibC.INSTANCE.tcsetattr(LibC.SYSTEM_OUT_FD, LibC.TCSAFLUSH, termios);
        if (rc != 0) {
            throw new IllegalStateException("error calling libc.tcsetattr rc: " + rc);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            disableRawMode();
            clearScreen();
        }));
    }

    public void disableRawMode() {
        log.debug("reset terminal");
        final int rc = LibC.INSTANCE.tcsetattr(LibC.SYSTEM_OUT_FD, LibC.TCSAFLUSH, backup);
        if (rc != 0) {
            throw new IllegalStateException("error calling libc.tcsetattr rc: " + rc);
        }
    }

    public void clearScreen() {
        log.debug("clear screen");
        System.out.print("\033[2J");
        System.out.print("\033[H");
    }

    public void draw() {
        final LibC.Winsize winsize = getWindowsize();
        clearScreen();

        if (entries.isEmpty()) {
            for (int i = 0; i < winsize.ws_row; ++i) {
                System.out.println("\r\n");
            }
        } else {
            final int index = entries.indexOf(entry);
            // Try to position the current title in the middle of the screen
            final int half = Math.floorDiv(winsize.ws_row, 2);

            // TODO rows: 29, half: 14, index: 115, startIndex: 114, entries: 116
            int startIndex = Math.max(0, index - half);
            if (index + half > entries.size()) {
                startIndex += winsize.ws_row - (index + half) - entries.size();
            }
            final int columnLength = Math.max(0, winsize.ws_col / 3);
            log.debug("rows: {}, half: {}, index: {}, startIndex: {}, entries: {}", winsize.ws_row, half, index, startIndex, entries.size());
            for (int i = startIndex; i < startIndex + winsize.ws_row; ++i) {
                final Entry current = (i > entries.size() - 1) ? null : entries.get(i);

                if (current == null) {
                    log.debug("no entry at {}", i);
                    System.out.print("\r\n");
                } else if (i == index) {
                    final String fullTitle = "\033[1;44;1;37m" + getFullTitle(current, columnLength) + "\033[0m";
                    System.out.print(fullTitle + "\r\n");
                } else {
                    System.out.print(getFullTitle(current, columnLength) + "\r\n");
                }
            }
        }

        // Print status line
        System.out.print("\033[7m" + getStatusLine(winsize.ws_col) + "\033[0m");
    }

    public String getStatusLine(final int length) {
        final String status = String.format(STATUS,
                getVersion(),
                entries.size(),
                (int) (Optional.ofNullable(player).map(MediaPlayer::getVolume).orElse(0d) * 100.0),
                state.toString().toLowerCase());
        return status + " ".repeat(Math.max(0, length - status.length()));
    }

    public String getVersion() {
        if (version == null) {
            try {
                version = "v"
                        + new String(IOUtils
                                .toByteArray(this.getClass().getClassLoader().getResourceAsStream("version.txt")));
            } catch (final IOException e) {
                log.error("Unable to read version: {}", e.getMessage(), e);
                version = StringUtils.EMPTY;
            }
        }
        return version;
    }

    public String getFullTitle(final Entry entry, final int columnLength) {
        return fitToWidth(entry.getArtist(), columnLength) + fitToWidth(entry.getAlbum(), columnLength)
                + fitToWidth(entry.getTitle(), columnLength);
    }

    public String fitToWidth(final String message, final int width) {
        final String msg = StringUtils.trim(message);
        return StringUtils.abbreviate(StringUtils.trim(msg), "... ", width)
                + StringUtils.SPACE.repeat(Math.max(0, width - StringUtils.length(msg)));
    }
}
