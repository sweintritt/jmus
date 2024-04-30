package com.github.sweintritt.jmus;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import java.util.Set;

public class NextCommand extends Command {

    public NextCommand(final Application application) {
        super("next", Set.of("n"), application);
    }

    @Override
    protected void executeImpl(CommandLine commandLine) {
        // TODO (weintrit) Implemen
        this.application.nextFile();
    }
}
