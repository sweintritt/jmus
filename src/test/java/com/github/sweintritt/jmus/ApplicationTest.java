package com.github.sweintritt.jmus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

class ApplicationTest {

    private Application application;

    @BeforeEach
    void setup() {
        application = new Application();
    }

    @Test
    void fitToWidth() {
        assertThat(application.fitToWidth("Hello\n", 5)).isEqualTo("Hello");
        assertThat(application.fitToWidth("Hello\n", 6)).isEqualTo("Hello ");
        assertThat(application.fitToWidth("Hello\0", 6)).isEqualTo("Hello ");
    }

    @Test
    void setVolume() {
        assertThat(application.getVolume()).isEqualTo(0.5);
        application.setVolume(1.1);
        assertThat(application.getVolume()).isEqualTo(1.0);
        application.setVolume(8.1);
        assertThat(application.getVolume()).isEqualTo(1.0);
        application.setVolume(0.0);
        assertThat(application.getVolume()).isEqualTo(0.0);
        application.setVolume(-2.0);
        assertThat(application.getVolume()).isEqualTo(0.0);
    }

    @Test
    void getFullTitle() {
        assertThat(application
                .getFullTitle(entry("  Johnny Cash  ", "  American III: Solitary Man  ", "  I'm Leavin' Now  "), 30))
                .hasSize(90)
                .isEqualTo(
                        "Johnny Cash                   American III: Solitary Man    I'm Leavin' Now               ");
        assertThat(application.getFullTitle(entry("Johnny Cash\n", "American III: Solitary Man\0", "I'm Leavin' Now\t"),
                30))
                .hasSize(90)
                .isEqualTo(
                        "Johnny Cash                   American III: Solitary Man    I'm Leavin' Now               ");
        assertThat(application.getFullTitle(entry("Paul Gilbert ", "Flying Dog ", "Get It "), 30))
                .hasSize(90)
                .isEqualTo(
                        "Paul Gilbert                  Flying Dog                    Get It                        ");
    }

    private Entry entry(final String artist, final String album, final String title) {
        final Entry entry = new Entry(null);
        entry.setArtist(artist);
        entry.setAlbum(album);
        entry.setTitle(title);
        return entry;
    }

    @Test
    void getVersion() {
        assertThat(application.getVersion()).isEqualTo("v1.0.0-test");
        application.setVersion(null);
        try (final MockedStatic<IOUtils> ioUtils = Mockito.mockStatic(IOUtils.class)) {
            ioUtils.when(() -> IOUtils.toByteArray(any(InputStream.class)))
                    .thenThrow(new IOException("just no"));
            assertThat(application.getVersion()).isEmpty();
        }
    }
}
