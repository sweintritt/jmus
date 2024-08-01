package com.github.sweintritt.jmus;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.tika.exception.TikaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

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
    void getFullTitle() {
        assertThat(application.getFullTitle(Triple.of("  Johnny Cash  ", "  American III: Solitary Man  ", "  I'm Leavin' Now  "), 30))
            .hasSize(90)
            .isEqualTo("Johnny Cash                   American III: Solitary Man    I'm Leavin' Now               ");
        assertThat(application.getFullTitle(Triple.of("Johnny Cash\n", "American III: Solitary Man\0", "I'm Leavin' Now\t"), 30))
            .hasSize(90)
            .isEqualTo("Johnny Cash                   American III: Solitary Man    I'm Leavin' Now               ");
        assertThat(application.getFullTitle(Triple.of("Paul Gilbert ", "Flying Dog ", "Get It "), 30))
            .hasSize(90)
            .isEqualTo("Paul Gilbert                  Flying Dog                    Get It                        ");
    }

    @Test
    void getIndex() throws IOException, SAXException, TikaException {
        application.getEntries().addAll(Arrays.asList(
            new Entry(new File(this.getClass().getClassLoader().getResource("Free_Test_Data_100KB_MP3.mp3").getFile())), 
            new Entry(new File(this.getClass().getClassLoader().getResource("Free_Test_Data_500KB_MP3.mp3").getFile()))));
        assertThat(application.getIndex()).isZero();
        assertThat(application.getIndex()).isZero();
        assertThat(application.getNextIndex()).isZero();
        assertThat(application.getNextIndex()).isOne();
    }
}
