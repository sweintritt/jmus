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

    private static final String PROMPT = "[ jmus v1.0 | vol:%d | %s] (q)uit, (s)top, (p)lay, (n)ext, (+)volume, (-)volume";

    private final Random random = new Random();
    private final int rows = 49;
    private final List<String> messages = new LinkedList<>();
    private State state = State.SEARCHING;
    /**
     * Backup of the original values
     */
    private LibC.Termios backup;
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
                if (file.isFile()) {
                    result.add(file);
                } else if (file.isDirectory()) {
                    getAllFiles(file, result);
                }
            }
        }
    }

    public void run() throws IOException {
        running = true;
        addMessage("found " + files.size() + " audio files");
        next();
        while (running) {
            draw();

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
    }

    private void next() {
        log.debug("selecting next file");
        Optional.ofNullable(player).ifPresent(MediaPlayer::stop);
        Optional.ofNullable(player).ifPresent(MediaPlayer::dispose);
        try {
            final int index = random.nextInt(files.size());
            final File file = files.get(index);
            log.info("playing {}", file.getName());
            addMessage("playing " + file.getName());
            media = new Media(file.toURI().toString());
            player = new MediaPlayer(media);
            player.setOnEndOfMedia(this::setOnEndOfMedia);
            player.setVolume(0.5);
            player.play();
            state = State.PLAYING;
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
//        termios.c_cc[LibC.VMIN] = 0;
//        termios.c_cc[LibC.VTIME] = 1;
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
        System.out.print("\033[2J");
        final int start = Math.max(0, messages.size() - rows);

        for (int i = 0; i < Math.max(0, rows - messages.size()); ++i) {
            System.out.print("\r\n");
        }

        for (int i = start; i < messages.size(); ++i) {
            System.out.print(messages.get(i) + "\r\n");
        }

        System.out.print(String.format(PROMPT, (int) (player.getVolume() * 100.0), state.toString().toLowerCase()));
    }

    public void addMessage(final String msg) {
        this.messages.add(msg);
        if (messages.size() > rows) {
            this.messages.removeFirst();
        }
    }
}