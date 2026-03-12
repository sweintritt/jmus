package com.github.sweintritt.jmus;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

@Slf4j
@Getter
@Setter
public class Application {

    private static final String STATUS = "[ jmus %s | %d files | vol:%d | %s ] (q)uit, (s)top, (p)lay, (b)ack, (n)ext, (+)volume, (-)volume";

    private final Random random = new Random();
    private final List<Entry> entries = new LinkedList<>();
    // TODO Use a buffer with fixed size
    private final Deque<Integer> indexStack = new LinkedList<>();

    private Entry entry;
    private State state = State.SEARCHING;
    private String version;
    private double volume = 0.5;
    /**
     * Root directory to scan for music
     */
    private File directory;
    private MediaPlayer player;
    private Media media;
    private boolean running;

    private Terminal terminal;

    public Application() throws IOException {
       terminal = TerminalBuilder.builder().build();
    }

    public void run() {
        try {
            log.info("scanning for files");
            loadFiles(directory);
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
            quit(e);
        }
    }

    private void loadFiles(final File dir) {
        log.info("searching {}", dir.getName());
        final File[] files = dir.listFiles();
        if (files != null) {
            for (final File file : files) {
                // For now just mp3s is fine
                if (file != null && file.isFile() && Strings.CI.endsWith(file.getName(), ".mp3")) {
                    entries.add(new Entry(file));
                } else if (file != null && file.isDirectory()) {
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
            case 'b':
                back();
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
                setVolume(player.getVolume() + 0.1);
                draw();
                break;
            case '-':
                setVolume(player.getVolume() - 0.1);
                draw();
                break;
            default:
                break;
        }
    }

    public void setVolume(final double volume) {
        this.volume = Math.min(1.0, Math.max(0.0, volume));
        Optional.ofNullable(player).ifPresent(p -> p.setVolume(this.volume));
    }

    public void next() {
        play(random.nextInt(entries.size()));
    }

    /**
     * Jump back one track in the list
     */
    public void back() {
        if (indexStack.size() > 1) {
            indexStack.pop();
            play(indexStack.pop());
        }
    }

    public void play(final int index) {
        indexStack.push(index);
        Optional.ofNullable(player).ifPresent(MediaPlayer::stop);
        Optional.ofNullable(player).ifPresent(MediaPlayer::dispose);
        try {
            entry = entries.get(index);
            log.info("playing {}", entry.getFile().getName());
            player = new MediaPlayer(new Media(entry.getFile().toURI().toString()));
            player.setOnEndOfMedia(this::next);
            player.setVolume(volume);
            player.setAutoPlay(true);
            player.setOnReady(this::draw);
        } catch (final MediaException e) {
            if (!Strings.CS.equals(e.getMessage(), "Unrecognized file signature!")) {
                log.error("Error during playback: {} ", e.getMessage(), e);
            }
        } catch (final Exception e) {
            log.error("Error during playback: {} ", e.getMessage(), e);
        }
    }

    public void stop() {
        Optional.ofNullable(player).ifPresent(MediaPlayer::pause);
        state = State.STOPPED;
    }

    public void play() {
        Optional.ofNullable(player).ifPresent(MediaPlayer::play);
        state = State.PLAYING;
    }

    public void quit(final Exception e) {
        setRunning(false);
        Optional.ofNullable(player).ifPresent(MediaPlayer::stop);
        clearScreen();
        if (e != null) {
            log.error(e.getMessage(), e);
            System.err.println("Error: " + e.getMessage());
        }
        Platform.exit();
    }

    public void quit() {
        quit(null);
    }

    public void clearScreen() {
        log.debug("clear screen");
        terminal.puts(InfoCmp.Capability.clear_screen);
        terminal.flush();
    }

    public void draw() {
        try {
            var rows = terminal.getHeight();
            var columns = terminal.getWidth();
            clearScreen();

            if (entries.isEmpty()) {
                for (int i = 0; i < rows; ++i) {
                    System.out.println("\r\n");
                }
            } else {
                final int index = entries.indexOf(entry);
                // Try to position the current title in the middle of the screen
                final int half = Math.floorDiv(rows, 2);
                int startIndex = index - half;
                if (index + half > entries.size()) {
                    startIndex -= (index + half) - entries.size();
                }

                // Ensure that the start index is in bounds of the entry list
                startIndex = Math.min(entries.size(), Math.max(0, startIndex));
                final int columnLength = Math.max(0, columns / 3);
                log.debug("rows: {}, half: {}, index: {}, startIndex: {}, entries: {}", rows, half, index,
                        startIndex, entries.size());
                for (int i = startIndex; i < startIndex + rows; ++i) {
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
            System.out.print("\033[7m" + getStatusLine(columns) + "\033[0m");
        } catch (final Exception e) {
            quit(e);
        }
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
                        + new String(IOUtils.toByteArray(Objects.requireNonNull(this.getClass().getClassLoader()
                                .getResourceAsStream("version.txt"))));
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
