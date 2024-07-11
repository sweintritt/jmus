package com.github.sweintritt.jmus;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;

class ApplicationTest {

    private final Application application = new Application();

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
}
