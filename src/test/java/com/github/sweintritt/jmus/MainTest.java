package com.github.sweintritt.jmus;

import static org.assertj.core.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.logging.log4j.core.util.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.TestUtils;

class MainTest {

    @BeforeAll 
    static void setup() { 
        TestUtils.addTestAppender();
    } 

    @BeforeEach
    void setupEach() {
        TestUtils.resetLog();
    }

    @AfterAll 
    static void reset() { 
        TestUtils.removeTestAppender();
    } 

    @Test
    void emptyDirectory() {
        Main.main(new String[]{""});
        assertThat(TestUtils.getLog().toString()).contains("No directory given");
    }

    @Test
    void noDirectory() {
        Main.main(new String[]{});
        assertThat(TestUtils.getLog().toString()).contains("No directory given");
    }

    @Test
    void notADirectory() {
        Main.main(new String[]{"foo"});
        assertThat(TestUtils.getLog().toString()).contains("foo is not a directory");
    }
}
