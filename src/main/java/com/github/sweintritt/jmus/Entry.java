package com.github.sweintritt.jmus;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


import javafx.scene.media.Media;
import lombok.Data;

@Data
public class Entry {

    private final File file;
    private final Media media;
    private final Metadata metadata;

    public Entry(final File file) throws IOException, SAXException, TikaException {
        this.file = file;
        this.media = new Media(file.toURI().toString());
        this.metadata = new Metadata();
        try (final InputStream input = new FileInputStream(file)) {
            final ContentHandler handler = new DefaultHandler();
            final Parser parser = new Mp3Parser();
            final ParseContext context = new ParseContext();
            parser.parse(input, handler, metadata, context);
        }
    }

    public String getArtist() {
        return Optional.ofNullable(metadata.get("artists")).orElse(StringUtils.EMPTY);
    }

    public String getAlbum() {
        return Optional.ofNullable(metadata.get("album")).orElse(StringUtils.EMPTY);
    }

    public String getTitle() {
        return Optional.ofNullable(metadata.get("title")).orElse(StringUtils.EMPTY);
    }

    public static Comparator<Entry> orderByArtistAblumName() {
        return Comparator.comparing(Entry::getArtist)
            .thenComparing(Entry::getAlbum)
            .thenComparing(Entry::getTitle);
    }
}
