package com.github.sweintritt.jmus;

import lombok.Getter;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;

import java.util.concurrent.CompletableFuture;

/**
 * Wrapper class for vlc player, to easily mock the player in unit tests.
 */
@Getter
public class Player {    

    private final MediaPlayer mediaPlayer;
    // If the player is paused, the volume is set to -1,
    // so we need to keep track of the volume separately.
    private int volume = 50;

    public Player() {
        mediaPlayer = new MediaPlayerFactory().mediaPlayers().newMediaPlayer();
    }

    public void prepare(String mediaPath) {
        mediaPlayer.media().prepare(mediaPath);
    }

    public void play() {
        mediaPlayer.controls().play();
    }

    public void pause() {
        mediaPlayer.controls().pause();
    }

    public void setVolume(int volume) {
        this.volume = Math.clamp(volume, 0, 100);
        mediaPlayer.audio().setVolume(volume);
    }

    public void stop() {
        mediaPlayer.controls().stop();
    }

    public boolean isPlaying() {
        return mediaPlayer.status().isPlaying();
    }

    public void release() {
        mediaPlayer.release();
    }

    public void onMediaEnd(final Runnable runnable) {
        mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void finished(MediaPlayer mediaPlayer) {
                CompletableFuture.runAsync(runnable);
            }
        });
    }
}
