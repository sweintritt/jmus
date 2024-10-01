jmus - *J*ava *Mus*ic Player
=============================

A very simple music player to olay your local music library on shuffel,
in the console using libc (Thanks [Marco](https://www.youtube.com/watch?v=kT4JYQi9w4w)!)

jmus uses *libc* for drawing.

# Play

Provide the folder to your music library on start. jmus willl search all of its subfolders
for mp3 files. For now only mp3-Files are played.

```bash
$ java -jar jmus.jar ~/Music
```

After scanning for files, jmus starts playing a random song. While playing jmus reads all
track infos, this may take a while, depending on the size of your music library.

On the bottom of the screen is the status line. It shows the version, the number of found
files, the current volume, the current state and all available commands. The commands are
executed by pressing the button in brackets.

![Main screen](doc/screen01.png)

## Commands

* `q` - Quit jmus
* `s` - Stop playing
* `p` - Start playing
* `b` - Play the previous song
* `n` - Play the next song
* `+` - Increase the volume
* `-` - Decrease the columne

# Build

To build jmus you need [Maven](https://maven.apache.org/) and a [Java JDK](https://openjdk.org/)
(min. Version 21).

```bash
$ mvn package
```

# Log

jmus creates a log file in your home folder `.jmus.log`. The file is cleared on every start.

# Known Issues

* Somtimes the app crashes on start. Just close it with CTRL-C and start again.