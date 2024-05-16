package com.github.sweintritt.jmus;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import lombok.NoArgsConstructor;
import lombok.ToString;

public interface LibC extends Library {

    LibC INSTANCE = Native.load("c", LibC.class);

    int SYSTEM_OUT_FD = 0;
    int ISIG = 1;
    int ICANON = 2;
    int ECHO = 10;
    int TCSAFLUSH = 2;
    int IXON = 2000;
    int ICRNL = 400;
    int IEXTEN = 100000;
    int OPOST = 1;
    int VMIN = 6;
    int VTIME = 5;
    int TIOCGWINSZ = 0x5413;

    @NoArgsConstructor
    @ToString
    @Structure.FieldOrder({"c_iflag", "c_oflag", "c_cflag", "c_lflag", "c_cc"})
    class Termios extends Structure {

        private static final int NCCS = 19;

        /**
         * input modes
         */
        public int c_iflag;
        /**
         * output modes
         */
        public int c_oflag;
        /**
         * control modes
         */
        public int c_cflag;
        /**
         * local modes
         */
        public int c_lflag;
        /**
         * special characters
         */
        public byte[] c_cc = new byte[NCCS];

        public static Termios of(final Termios t) {
            final Termios termios = new Termios();
            termios.c_iflag = t.c_iflag;
            termios.c_oflag = t.c_oflag;
            termios.c_cflag = t.c_cflag;
            termios.c_lflag = t.c_lflag;
            termios.c_cc = t.c_cc.clone();
            return termios;
        }
    }

    @Structure.FieldOrder({"ws_row", "ws_col", "ws_xpixel", "ws_ypixel"})
    class Winsize extends Structure {
        public short ws_row;
        public short ws_col;
        public short ws_xpixel;
        public short ws_ypixel;
    }

    int tcgetattr(int fd, Termios termios);
    int tcsetattr(int fd, int optional_actions, Termios termios);
    int ioctl(int fd, int opt, Winsize winsize);
}
