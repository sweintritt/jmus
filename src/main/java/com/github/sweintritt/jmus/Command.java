package com.github.sweintritt.jmus;

import lombok.*;
import org.apache.commons.cli.*;

import java.util.Set;

@Data
@RequiredArgsConstructor
public abstract class Command {

    protected final String name;
    protected final Set<String> aliases;
    protected final Application application;
    protected final Options options = new Options();

    public void execute(final String[] args) throws ParseException {
        final CommandLineParser parser = new DefaultParser();
        final CommandLine commandLine = parser.parse(options, args);
        executeImpl(commandLine);
    }

    protected abstract void executeImpl(final CommandLine commandLine);
}
