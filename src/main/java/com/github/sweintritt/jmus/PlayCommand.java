package com.github.sweintritt.jmus;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import java.util.Set;

public class PlayCommand extends Command {

    public PlayCommand(final Application application) {
        super("play", Set.of("p"), application);
        this.options.addOption(Option.builder()
                .option("f")
                .longOpt("file")
                .hasArg()
                .optionalArg(true)
                .build());
    }

    @Override
    protected void executeImpl(CommandLine commandLine) {
        // TODO (weintrit) Implement
    }
}
