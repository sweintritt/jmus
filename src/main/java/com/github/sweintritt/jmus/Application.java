package com.github.sweintritt.jmus;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;

import java.io.File;
import java.util.*;

@Slf4j
@Getter
@Setter
public class Application {

    private static final String STATUS = "[ jmus v1.0 | %d files | vol:%d | %s ] (q)uit, (s)top, (p)lay, (n)ext, (+)volume, (-)volume";

    private final Random random = new Random();
    private final List<Triple<String, String, String>> titlelist = new LinkedList<>();
    private State state = State.SEARCHING;
    /**
     * Backup of the original values
     */
    private LibC.Termios backup;
    private List<File> files;
    private MediaPlayer player;
    private Media media;
    private boolean running;

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

    public void run() {
        try {
            running = true;
            next();
            while (running) {
                draw();
                final int key = System.in.read();
                handleKey(key);
            }
        } catch (final Exception e) {
            log.error(e.getMessage(), e);
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void handleKey(final int key) {
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

    private void next() {
        log.debug("selecting next file");
        Optional.ofNullable(player).ifPresent(MediaPlayer::stop);
        Optional.ofNullable(player).ifPresent(MediaPlayer::dispose);
        try {
            final int index = random.nextInt(files.size());
            final File file = files.get(index);
            log.info("playing {}", file.getName());

            media = new Media(file.toURI().toString());
            player = new MediaPlayer(media);
            player.setOnEndOfMedia(this::setOnEndOfMedia);
            player.setVolume(0.5);
            play();
            state = State.PLAYING;
            player.setOnReady(() -> {
                log.debug("metadata: {}", player.getMedia().getMetadata());
                addMessage(media);
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

    private void setOnEndOfMedia() {
        next();
        draw();
    }

    private void stop() {
        Optional.ofNullable(player).ifPresent(MediaPlayer::pause);
        state = State.STOPPED;
    }

    private void play() {
        Optional.ofNullable(player).ifPresent(MediaPlayer::play);
        state = State.PLAYING;
    }

    private void quit() {
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

        final int length = Math.max(0, winsize.ws_col / 3);
        for (int i = start; i < titlelist.size(); ++i) {
            String fullTitle = fitToWidth(titlelist.get(i).getLeft(), length) +
                    fitToWidth(titlelist.get(i).getMiddle(), length) +
                    fitToWidth(titlelist.get(i).getRight(), length);

            log.debug("length full title: {}, column width: {}, window columns: {}", fullTitle.length(), length, winsize.ws_col);
            if (i == titlelist.size() - 1) {
                fullTitle = "\033[1;44;1;37m" + fullTitle + "\033[0m";
            }

            System.out.print(fullTitle + "\r\n");
        }

        final String status = String.format(STATUS,
                files.size(),
                (int) (player.getVolume() * 100.0),
                state.toString().toLowerCase());
        System.out.print("\033[7m" + status + " ".repeat(Math.max(0, winsize.ws_col - status.length())) + "\033[0m");
    }

    private String fitToWidth(final String message, final int width) {
        return StringUtils.abbreviate(message, width) + " ".repeat(Math.max(0, width - StringUtils.length(message)));
    }

    public void addMessage(final Media media) {
        this.titlelist.add(Triple.of(
                (String) media.getMetadata().get("artist"),
                (String) media.getMetadata().get("album"),
                (String) media.getMetadata().get("title")));
    }
}