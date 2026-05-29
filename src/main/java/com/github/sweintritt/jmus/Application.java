package com.github.sweintritt.jmus;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.*;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;

@Slf4j
@Getter
@Setter
public final class Application {

    private enum Mode {
        HELP, ENTRIES
    }
    private enum Order {
        SORTED, RANDOM
    }

    private static final String STATUS = "[ jmus %s | %d files | vol:%d | %s ] press h for help";
    private static final MediaPlayerFactory MEDIA_FACTORY = new MediaPlayerFactory();

    private final List<Entry> entries = new LinkedList<>();
    private final Queue<Entry> playStack = new LimitedLiFoQueue<>(100);
    private final MediaPlayer player;
    private final Terminal terminal;

    private String version;
    private Entry entry;
    private Mode mode = Mode.ENTRIES;
    private Order order = Order.SORTED;
    private int volume = 50;
    private boolean running;
    private int index = -1;

    public Application() throws IOException {
        log.debug("init application");
        terminal = TerminalBuilder.builder().build();
        player = MEDIA_FACTORY.mediaPlayers().newMediaPlayer();
        try {
            version = "v"
                    + new String(IOUtils.toByteArray(Objects.requireNonNull(this.getClass().getClassLoader()
                            .getResourceAsStream("version.txt"))));
        } catch (final IOException e) {
            log.error("Unable to read version: {}", e.getMessage(), e);
            version = StringUtils.EMPTY;
        }
        log.debug("application ready");
    }

    /**
     * @param directory Root directory to scan for music
     */
    public void run(final File directory) {
        try {
            log.info("scanning {}", directory.getAbsoluteFile());
            loadFiles(directory);
            log.info("found {} files", entries.size());
            CompletableFuture.runAsync(() -> entries.forEach(Entry::loadMetadata));
            terminal.enterRawMode();
            running = true;
            toggleRandom();
            next();
            while (running) {
                draw();
                final int key = terminal.reader().read();
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
        log.debug("key:{}", key);
        switch (key) {
            case 'n' -> next();
            case 'b' -> back();
            case 'p' -> play();
            case 's' -> stop();
            case 'q' -> quit();
            case '+' -> setVolume(player.audio().volume() + 10);
            case '-' -> setVolume(player.audio().volume() - 10);
            case 'r' -> toggleRandom();
            case 'h' -> toggleHelp();
            // TODO esc should also exit help view
            default -> log.trace("unknown key {}", key);
        }
    }

    public void toggleHelp() {
        if (Mode.HELP == mode) {
            mode = Mode.ENTRIES;
        } else {
            mode = Mode.HELP;
        }
    }

    public void toggleRandom() {
        if (Order.RANDOM == order) {
            Collections.sort(entries);
            order = Order.SORTED;
        } else {
            Collections.shuffle(entries);
            order = Order.RANDOM;
        }
        index = entries.indexOf(entry);
    }

    public void setVolume(final int volume) {
        log.debug("set volume to {}", volume);
        this.volume = Math.clamp(volume, 0, 100);
        player.audio().setVolume(this.volume);
    }

    public void next() {
        log.debug("playing next song");
        if (entry != null) {
            playStack.add(entry);
        }

        index = (index >= entries.size() - 1) ? 0 : index + 1;
        play(index);
    }

    /**
     * Jump back one track in the list
     */
    public void back() {
        log.debug("playing previous song. index stack size: {}", playStack.size());
        if (!playStack.isEmpty()) {
            index = entries.indexOf(playStack.poll());
            play(index);
        }
    }

    public void play(final int index) {
        stop();
        try {
            entry = entries.get(index);
            log.info("playing {}, index:{}", entry.getFile().getName(), index);
            player.media().prepare(entry.getMedia().info().mrl());
            player.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
                @Override
                public void finished(final MediaPlayer player) {
                    // vlc controls cannot be called from the vlc event thread
                    CompletableFuture.runAsync(() -> {
                        next();
                        draw();
                    });
                }
            });
            play();
        } catch (final Exception e) {
            log.error("Error during playback: {} ", e.getMessage(), e);
        }
    }

    public void stop() {
        log.debug("stopping");
        player.controls().stop();
    }

    public void play() {
        log.debug("playing");
        player.controls().start();
    }

    public void quit(final Exception e) {
        log.debug("quiting");
        setRunning(false);
        stop();
        player.release();
        clearScreen();
        if (e != null) {
            log.error(e.getMessage(), e);
            System.err.println("Error: " + e.getMessage());
        }
        System.exit(0);
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
            log.debug("draw");
            var rows = terminal.getHeight();
            var columns = terminal.getWidth();
            clearScreen();

            switch (mode) {
                case Mode.ENTRIES -> drawEntries(columns, rows);
                case Mode.HELP -> drawHelp(rows);
                default -> drawEmpty(rows);
            }

            // Print status line
            var status = new AttributedString(getStatusLine(columns),
                    AttributedStyle.DEFAULT.foreground(AttributedStyle.BLACK).background(AttributedStyle.WHITE));
            status.print(terminal);
            terminal.flush();
        } catch (final Exception e) {
            quit(e);
        }
    }

    public void drawHelp(final int rows) {
        var help = List.of(
            "jmus - " + version,
            StringUtils.EMPTY,
            "--------------------------------------------------",
            StringUtils.EMPTY,
            "Simple audio player to play your local library.",
            "jmus is designed to be very easy to use, with just",
            "a few simple keys.",
            StringUtils.EMPTY,
            StringUtils.EMPTY,
            "Mappings",
            StringUtils.EMPTY,
            " b: play previous song",
            " h: show this help text",
            " n: play next song",
            " p: start playing",
            " q: quit jmus",
            " r: switch between random or sorted song order",
            " s: stop playing",
            " +: increase volume",
            " -: decrease volume"
        );

        help.forEach(l -> terminal.writer().println(l));
        for (var i = help.size(); i < rows - 1; ++i) {
            terminal.writer().println(StringUtils.EMPTY);
        }
    }

    public void drawEntries(final int columns, final int rows) {
        if (entries.isEmpty()) {
            drawEmpty(rows);
        } else {
            // Try to position the current title in the middle of the screen
            final int half = Math.floorDiv(rows, 2);
            int startIndex = index - half;
            if (index + half > entries.size()) {
                startIndex -= (index + half) - entries.size();
            }

            // Ensure that the start index is in bounds of the entry list
            startIndex = Math.clamp(startIndex, 0, entries.size());
            final int columnLength = Math.max(0, columns / 3);
            log.debug("rows: {}, half: {}, index: {}, startIndex: {}, entries: {}", rows, half, index,
                    startIndex, entries.size());
            for (int i = startIndex; i < startIndex + rows - 1; ++i) {
                final Entry current = (i > entries.size() - 1) ? null : entries.get(i);

                if (current == null) {
                    log.debug("no entry at {}", i);
                    terminal.writer().println(StringUtils.EMPTY);
                } else if (i == index) {
                    var styled = new AttributedString(getFullTitle(current, columnLength),
                            AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE)
                                    .background(AttributedStyle.BLUE));
                    styled.println(terminal);
                } else {
                    terminal.writer().println(getFullTitle(current, columnLength));
                }
            }
        }
    }

    private void drawEmpty(final int rows) {
        for (int i = 0; i < rows; ++i) {
            terminal.writer().println(StringUtils.EMPTY);
        }
    }

    public String getStatusLine(final int length) {
        final String status = String.format(STATUS,
                version,
                entries.size(),
                volume,
                player.status().isPlaying() ? "playing" : "stopped");
        return status + " ".repeat(Math.max(0, length - status.length()));
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
