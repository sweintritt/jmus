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
import org.apache.commons.lang3.tuple.Triple;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Slf4j
@Getter
@Setter
public class Application {

    private static final String STATUS = "[ jmus %s | %d files | vol:%d | %s ] (q)uit, (s)top, (p)lay, (n)ext, (+)volume, (-)volume";

    private final Random random = new Random();
    private final List<Triple<String, String, String>> titlelist = new LinkedList<>();
    private final List<Entry> entries = new ArrayList<>();
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
    private int index = 0;
    private MediaPlayer player;
    private Media media;
    private boolean running;

    public void run() {
        try {
            enableRawMode();
            //draw();
            log.info("scanning for files");
            loadFiles(directory);
            log.info("found {} files", entries.size());
            state = State.STOPPED;
            Collections.shuffle(entries);

            running = true;
            next();
            while (running) {
                draw();
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
        if(files != null) {
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
                break;
            case '-':
                player.setVolume(Math.max(player.getVolume() - 0.1, 0.0));
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
            final Entry entry = entries.get(getNextIndex());
            log.info("playing {}", entry.getFile().getName());
            final Media media = new Media(entry.getFile().toURI().toString());
            player = new MediaPlayer(media);
            player.setOnEndOfMedia(this::setOnEndOfMedia);
            player.setVolume(0.5);
            play();
            state = State.PLAYING;
            player.setOnReady(() -> {
                addMessage(entry);
                draw();
            });
        } catch (final MediaException e) {
            if (!StringUtils.equals(e.getMessage(), "Unrecognized file signature!")) {
                log.error("Error during playback: {} ", e.getMessage(), e);
            }
        } catch (final Exception e) {
            log.error("Error during playback: {} ", e.getMessage(), e);
        }
    }

    public int getNextIndex() {
        if (index > entries.size()) {
            index = 0;
        }

        return index++;
    }

    public void setOnEndOfMedia() {
        next();
        draw();
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

        Runtime.getRuntime().addShutdownHook(new Thread(this::disableRawMode));
    }

    public void disableRawMode() {
        log.debug("reset terminal");
        final int rc = LibC.INSTANCE.tcsetattr(LibC.SYSTEM_OUT_FD, LibC.TCSAFLUSH, backup);
        if (rc != 0) {
            throw new IllegalStateException("error calling libc.tcsetattr rc: " + rc);
        }
    }

    public void draw() {
        final LibC.Winsize winsize = getWindowsize();
        System.out.print("\033[2J");
        System.out.print("\033[H");
        final int start = Math.max(0, titlelist.size() - winsize.ws_row);

        for (int i = 0; i < Math.max(0, winsize.ws_row - titlelist.size()); ++i) {
            System.out.print("\r\n");
        }

        while (titlelist.size() > winsize.ws_row) {
            this.titlelist.removeFirst();
        }

        final int columnLength = Math.max(0, winsize.ws_col / 3);
        for (int i = start; i < titlelist.size(); ++i) {
            String fullTitle = getFullTitle(titlelist.get(i), columnLength);
            if (StringUtils.isBlank(fullTitle)) {
                log.warn("Message is empty: {}", titlelist.get(i));
            }
            log.debug("length full title: {}, column width: {}, window columns: {}", fullTitle.length(), columnLength, winsize.ws_col);
            if (i == titlelist.size() - 1) {
                fullTitle = "\033[1;44;1;37m" + fullTitle + "\033[0m";
            }

            System.out.print(fullTitle + "\r\n");
        }

        final String status = String.format(STATUS,
                getVersion(),
                entries.size(),
                (int) (Optional.ofNullable(player).map(MediaPlayer::getVolume).orElse(0d) * 100.0),
                state.toString().toLowerCase());
        System.out.print("\033[7m" + status + " ".repeat(Math.max(0, winsize.ws_col - status.length())) + "\033[0m");
    }

    public String getVersion() {
        if (version == null) {
            try {
                version = "v" + new String(IOUtils.toByteArray(this.getClass().getClassLoader().getResourceAsStream("version.txt")));
            } catch (final IOException e) {
                log.error("Unable to read version: {}", e.getMessage(), e);
                version = StringUtils.EMPTY;
            }
        }
        return version;
    }

    public String getFullTitle(final Triple<String, String, String> entry, final int columnLength) {
        return fitToWidth(entry.getLeft(), columnLength) + fitToWidth(entry.getMiddle(), columnLength) + fitToWidth(entry.getRight(), columnLength);
    }

    public String fitToWidth(final String message, final int width) {
        final String msg = StringUtils.trim(message);
        return StringUtils.abbreviate(StringUtils.trim(msg), "... ", width) 
            + StringUtils.SPACE.repeat(Math.max(0, width - StringUtils.length(msg)));
    }

    public void addMessage(final Entry e) {
        this.titlelist.add(Triple.of(e.getArtist(), e.getAlbum(), e.getTitle()));
    }
}