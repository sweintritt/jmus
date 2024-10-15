package com.github.sweintritt.jmus;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import java.io.File;
import java.util.Comparator;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@Data
@RequiredArgsConstructor
public class Entry {

    private final File file;
    private String artist = StringUtils.EMPTY;
    private String album = StringUtils.EMPTY;
    private String title = StringUtils.EMPTY;

    public void loadMp3Tags() {
        try {
            final Mp3File mp3 = new Mp3File(file);
            if (mp3.hasId3v1Tag()) {
                final ID3v1 tag = mp3.getId3v1Tag();
                this.artist = StringUtils.trimToEmpty(tag.getArtist());
                this.album = StringUtils.trimToEmpty(tag.getAlbum());
                this.title = StringUtils.trimToEmpty(tag.getTitle());
            } else if (mp3.hasId3v2Tag()) {
                final ID3v2 tag = mp3.getId3v2Tag();
                this.artist = StringUtils.trimToEmpty(tag.getArtist());
                this.album = StringUtils.trimToEmpty(tag.getAlbum());
                this.title = StringUtils.trimToEmpty(tag.getTitle());
            } else {
                log.error("no id3v1 or id3v2 tags found in {}", file.getName());
            }

            checkAndSetDefaults();
        } catch (final Exception e) {
            log.error("unable to read mp3 tags from {}: {}", file.getName(), e.getMessage());
            checkAndSetDefaults();
        }
    }

    private void checkAndSetDefaults() {
        if (StringUtils.isBlank(this.artist)) {
            this.artist = "unknown artist";
        }

        if (StringUtils.isBlank(this.album)) {
            this.album = "unknown album";
        }

        if (StringUtils.isBlank(this.title)) {
            this.title = file.getName();
        }
    }

    public String getArtist() {
        if (StringUtils.isEmpty(artist)) {
            loadMp3Tags();
        }
        return artist;
    }

    public String getAlbum() {
        if (StringUtils.isEmpty(artist)) {
            loadMp3Tags();
        }
        return album;
    }

    public String getTitle() {
        if (StringUtils.isEmpty(artist)) {
            loadMp3Tags();
        }
        return title;
    }

    public static Comparator<Entry> orderByArtistAblumName() {
        return Comparator.comparing(Entry::getArtist)
                .thenComparing(Entry::getAlbum)
                .thenComparing(Entry::getTitle);
    }
}
