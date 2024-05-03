package com.github.sweintritt.jmus;

import java.util.LinkedList;
import java.util.List;

public class UI {

    private final String prompt = "jmus v1.0 - (q)uit, (s)top, (p)lay, (n)ext";
    private final int rows = 49;
    private final List<String> messages = new LinkedList<>();
    /**
     * Backup of the original values
     */
    private LibC.Termios backup;

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

        System.out.print(prompt);
    }

    public void addMessage(final String msg) {
        this.messages.add(msg);
        if (messages.size() > rows) {
            this.messages.removeFirst();
        }
    }
}
