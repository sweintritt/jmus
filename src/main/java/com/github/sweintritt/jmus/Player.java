package com.github.sweintritt.jmus;

import lombok.Getter;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;

import java.util.concurrent.CompletableFuture;

/**
 * Wrapper class for vlc player, to easily mock the player in unit tests.
 */
@Getter
public class Player {    

    private final MediaPlayer player;
    // If the player is paused, the volume is set to -1,
    // so we need to keep track of the volume separately.
    private int volume = 50;

    public Player() {
        this.player = new MediaPlayerFactory().mediaPlayers().newMediaPlayer();
    }

    public void prepare(String mediaPath) {
        player.media().prepare(mediaPath);
    }

    public void play() {
        player.controls().play();
    }

    public void pause() {
        player.controls().pause();
    }

    public void setVolume(int volume) {
        this.volume = Math.clamp(volume, 0, 100);
        player.audio().setVolume(volume);
    }

    public void stop() {
        player.controls().stop();
    }

    public boolean isPlaying() {
        return player.status().isPlaying();
    }

    public void release() {
        player.release();
    }

    public void onMediaEnd(final Runnable runnable) {
        // vlc controls cannot be called from the vlc event thread
        CompletableFuture.runAsync(runnable);
    }
}
