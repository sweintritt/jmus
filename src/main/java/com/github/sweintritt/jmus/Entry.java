package com.github.sweintritt.jmus;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.Optional;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@RequiredArgsConstructor
public class Entry {

    private final File file;
    private String artist;
    private String album;
    private String title;

    private void loadMp3Tags() {
        try {
            final Mp3File mp3 = new Mp3File(file);
            if (mp3.hasId3v1Tag()) {
                final ID3v1 tag = mp3.getId3v1Tag();
                this.artist = tag.getArtist();
                this.album = tag.getAlbum();
                this.title = tag.getTitle();
            } else if (mp3.hasId3v2Tag()) {
                final ID3v2 tag = mp3.getId3v2Tag();
                this.artist = tag.getArtist();
                this.album = tag.getAlbum();
                this.title = tag.getTitle();
            } else {
                log.error("no id3v1 or id3v2 tags found in {]", file.getName());
                setDefaults();
            }
        } catch (final Exception e) {
            log.error("unable to read mp3 tags from {}: {}", file.getName(), e.getMessage());
            setDefaults();
        }
    }

    private void setDefaults() {
        this.artist = "unknown artist";
        this.album = "unknown album";
        this.title = file.getName();
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
