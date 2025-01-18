package com.github.sweintritt.jmus;

import com.mpatric.mp3agic.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("Just used to create test files")
class TestFileCreator {

    private static Mp3File mp3;

    @BeforeAll
    public static void setup() throws URISyntaxException, InvalidDataException, UnsupportedTagException, IOException {
        final URL resource = TestFileCreator.class.getClassLoader().getResource("tags_test/source.mp3");
        assertThat(resource).isNotNull();
        final File file = new File(resource.toURI());
        mp3 = new Mp3File(file);
        mp3.setId3v1Tag(null);
        mp3.setId3v2Tag(null);
    }

    @Test
    void v1AndV2Tags() throws IOException, NotSupportedException {
        final ID3v1 v1 = new ID3v1Tag();
        v1.setAlbum("v1 album");
        v1.setArtist("v1 artist");
        v1.setTitle("v1 title");
        mp3.setId3v1Tag(v1);

        final ID3v2 v2 = new ID3v24Tag();
        v2.setAlbum("v2 album");
        v2.setArtist("v2 artist");
        v2.setTitle("v2 title");
        mp3.setId3v2Tag(v2);

        mp3.save("v1_and_v2_tags.mp3");
    }

    @Test
    void v1Tags() throws IOException, NotSupportedException {
        final ID3v1 v1 = new ID3v1Tag();
        v1.setAlbum("v1 album");
        v1.setArtist("v1 artist");
        v1.setTitle("v1 title");
        mp3.setId3v1Tag(v1);

        mp3.save("v1_tags.mp3");
    }

    @Test
    void v2Tags() throws IOException, NotSupportedException {
        final ID3v2 v2 = new ID3v24Tag();
        v2.setAlbum("v2 album");
        v2.setArtist("v2 artist");
        v2.setTitle("v2 title");
        mp3.setId3v2Tag(v2);

        mp3.save("v2_tags.mp3");
    }
}
