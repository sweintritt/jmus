package com.github.sweintritt.jmus;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import com.mpatric.mp3agic.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class EntryTest {

    @Test
    void v1AndV2Tags() throws Exception {
        final URL url = this.getClass().getClassLoader().getResource("tags_test/v1_and_v2_tags.mp3");
        assertThat(url).isNotNull();
        final File file = new File(url.toURI());
        assertThat(file).exists();
        final Entry entry = new Entry(file);
        assertThat(entry.getArtist()).isEqualTo("v2 artist");
        assertThat(entry.getAlbum()).isEqualTo("v2 album");
        assertThat(entry.getTitle()).isEqualTo("v2 title");
    }

    @Test
    void v1Tags() throws Exception {
        final URL url = this.getClass().getClassLoader().getResource("tags_test/v1_tags.mp3");
        assertThat(url).isNotNull();
        final File file = new File(url.toURI());
        assertThat(file).exists();
        final Entry entry = new Entry(file);
        assertThat(entry.getArtist()).isEqualTo("v1 artist");
        assertThat(entry.getAlbum()).isEqualTo("v1 album");
        assertThat(entry.getTitle()).isEqualTo("v1 title");
    }

    @Test
    void v2Tags() throws Exception {
        final URL url = this.getClass().getClassLoader().getResource("tags_test/v2_tags.mp3");
        assertThat(url).isNotNull();
        final File file = new File(url.toURI());
        assertThat(file).exists();
        final Entry entry = new Entry(file);
        assertThat(entry.getArtist()).isEqualTo("v2 artist");
        assertThat(entry.getAlbum()).isEqualTo("v2 album");
        assertThat(entry.getTitle()).isEqualTo("v2 title");
    }
}
