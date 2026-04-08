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
import uk.co.caprica.vlcj.media.Media;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;

@Slf4j
@Getter
@Setter
public class Application {

    private static final String STATUS = "[ jmus %s | %d files | vol:%d | %s ] (q)uit, (s)top, (p)lay, (b)ack, (n)ext, (+)volume, (-)volume";
    private static final MediaPlayerFactory MEDIA_FACTORY = new MediaPlayerFactory();

    private final Random random = new Random();
    private final List<Entry> entries = new LinkedList<>();
    // TODO Use a buffer with fixed size
    private final Deque<Integer> indexStack = new LinkedList<>();

    private Entry entry;
    private State state = State.SEARCHING;
    private String version;
    private int volume = 50;
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
            log.info("scanning {}", directory.getAbsoluteFile());
            loadFiles(directory);
            log.info("found {} files", entries.size());
            CompletableFuture.runAsync(() -> entries.forEach(Entry::loadMetadata));
            terminal.enterRawMode();
            state = State.STOPPED;
            running = true;
            next();
            while (running) {
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
                setVolume(player.audio().volume() + 10);
                draw();
                break;
            case '-':
                setVolume(player.audio().volume() - 10);
                draw();
                break;
            default:
                break;
        }
    }

    public void setVolume(final int volume) {
        log.debug("set volume to {}", volume);
        this.volume = Math.clamp(volume, 0, 100);
        Optional.ofNullable(player).ifPresent(p -> p.audio().setVolume(this.volume));
    }

    public void next() {
        log.debug("playing next song");
        play(random.nextInt(entries.size()));
    }

    /**
     * Jump back one track in the list
     */
    public void back() {
        if (indexStack.size() > 1) {
            log.debug("playing previous song");
            indexStack.pop();
            play(indexStack.pop());
        }
    }

    public void play(final int index) {
        indexStack.push(index);
        Optional.ofNullable(player).ifPresent(p -> p.controls().stop());
        Optional.ofNullable(player).ifPresent(MediaPlayer::release);
        try {
            entry = entries.get(index);
            log.info("playing {}", entry.getFile().getName());
            player = MEDIA_FACTORY.mediaPlayers().newMediaPlayer();
            media = entry.getMedia();
            player.media().play(entry.getMedia().info().mrl());
            player.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
                @Override
                public void finished(final MediaPlayer player) {
                    next();
                }
            });
            player.audio().setVolume(volume);
            this.play();
            this.draw();
        } catch (final Exception e) {
            log.error("Error during playback: {} ", e.getMessage(), e);
        }
    }

    public void stop() {
        log.debug("stopping");
        Optional.ofNullable(player).ifPresent(p -> p.controls().stop());
        state = State.STOPPED;
    }

    public void play() {
        log.debug("playing");
        Optional.ofNullable(player).ifPresent(p -> p.controls().start());
        state = State.PLAYING;
    }

    public void quit(final Exception e) {
        log.debug("quiting");
        setRunning(false);
        stop();
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

            if (entries.isEmpty()) {
                for (int i = 0; i < rows; ++i) {
                    terminal.writer().println(StringUtils.EMPTY);
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
                startIndex = Math.clamp(startIndex, 0, entries.size());
                final int columnLength = Math.max(0, columns / 3);
                log.debug("rows: {}, half: {}, index: {}, startIndex: {}, entries: {}", rows, half, index,
                        startIndex, entries.size());
                for (int i = startIndex; i < startIndex + rows; ++i) {
                    final Entry current = (i > entries.size() - 1) ? null : entries.get(i);

                    if (current == null) {
                        log.debug("no entry at {}", i);
                        terminal.writer().println(StringUtils.EMPTY);
                    } else if (i == index) {
                        var styled = new AttributedString(getFullTitle(current, columnLength),
                                AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE).background(AttributedStyle.BLUE));
                        styled.println(terminal);
                    } else {
                        terminal.writer().println(getFullTitle(current, columnLength));
                    }
                }
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

    public String getStatusLine(final int length) {
        final String status = String.format(STATUS,
                getVersion(),
                entries.size(),
                Optional.ofNullable(player).map(p -> p.audio().volume()).orElse(0),
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
