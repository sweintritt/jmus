package com.github.sweintritt.jmus;

import javafx.scene.media.MediaPlayer;
import org.apache.commons.cli.CommandLine;

import java.util.Optional;
import java.util.Set;

public class QuitCommand extends Command {

    public QuitCommand(final Application application) {
        super("quit", Set.of("q"), application);
    }

    @Override
    protected void executeImpl(CommandLine commandLine) {
        this.application.setRunning(false);
        Optional.ofNullable(this.application.getPlayer()).ifPresent(MediaPlayer::stop);
    }
}
