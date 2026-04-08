package com.github.sweintritt.jmus;

import java.io.File;
import java.util.Comparator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.media.*;

@Slf4j
@Data
public class Entry {

    private static final MediaPlayerFactory MEDIA_FACTORY = new MediaPlayerFactory();

    private final File file;
    private final Media media;

    private CountDownLatch latch;

    private String artist = "unknown artist";
    private String album = "unknown album";
    private String title = "unknown title";

    public Entry(final File file) {
        this.file = file;
        this.media = MEDIA_FACTORY.media().newMedia(file.toPath().toString());
    }

    public void loadMetadata() {
        if (media.parsing().status() != MediaParsedStatus.DONE) {
            synchronized (this) {
                latch = new CountDownLatch(1);

                media.events().addMediaEventListener(new MediaEventAdapter() {
                    @Override
                    public void mediaParsedChanged(final Media media, final MediaParsedStatus status) {
                        switch (status) {
                            case SKIPPED, FAILED, TIMEOUT:
                                log.error("Unable to parse metadata for {}", media.info().mrl());
                                latch.countDown();
                                break;
                            case DONE:
                                log.debug("Parsed metadata for {}", media.info().mrl());
                                setMetadata(media.meta());
                                latch.countDown();
                                break;
                        }
                    }
                });

                if (media.parsing().status() != MediaParsedStatus.DONE) {
                    media.parsing().parse();
                }

                try {
                    if (media.parsing().status() != MediaParsedStatus.DONE && !latch.await(30, TimeUnit.SECONDS)) {
                        log.error("parsing metadata of {} did reach the timeout", media.info().mrl());
                    }
                } catch (final Exception e) {
                    log.error("unable to read mp3 tags from {}: {}", file.getName(), e.getMessage());
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    private void setMetadata(final MetaApi meta) {
        artist = StringUtils.trimToEmpty(meta.get(Meta.ARTIST));
        album = StringUtils.trimToEmpty(meta.get(Meta.ALBUM));
        title = StringUtils.trimToEmpty(meta.get(Meta.TITLE));
        if (StringUtils.isEmpty(title)) {
            title = file.getName();
        }
    }

    public String getArtist() {
        loadMetadata();
        return artist;
    }

    public String getAlbum() {
        loadMetadata();
        return album;
    }

    public String getTitle() {
        loadMetadata();
        return title;
    }

    public static Comparator<Entry> orderByArtistAblumName() {
        return Comparator.comparing(Entry::getArtist)
                .thenComparing(Entry::getAlbum)
                .thenComparing(Entry::getTitle);
    }
}
