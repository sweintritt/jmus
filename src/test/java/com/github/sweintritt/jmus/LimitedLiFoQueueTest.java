package com.github.sweintritt.jmus;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

public class LimitedLiFoQueueTest {

    @Test
    public void testLimitReached() {
        LimitedLiFoQueue<Integer> queue = new LimitedLiFoQueue<>(3);
        queue.add(1);
        queue.add(2);
        queue.add(3);
        assertEquals(3, queue.size());
        assertTrue(queue.contains(3));

        queue.add(4);
        assertEquals(3, queue.size());
        assertFalse(queue.contains(1));
        assertTrue(queue.contains(4));
        assertEquals(4, queue.peek());
    }

    @Test
    public void testOfferEviction() {
        LimitedLiFoQueue<String> queue = new LimitedLiFoQueue<>(2);
        queue.offer("A");
        queue.offer("B");
        assertEquals(2, queue.size());

        queue.offer("C");
        assertEquals(2, queue.size());
        assertEquals("C", queue.peek());
        assertFalse(queue.contains("A"));
    }

    @Test
    public void testAddAllEviction() {
        LimitedLiFoQueue<Integer> queue = new LimitedLiFoQueue<>(2);
        queue.addAll(Arrays.asList(1, 2, 3));
        assertEquals(2, queue.size());
        assertEquals(3, queue.peek());
        assertTrue(queue.contains(3));
        assertTrue(queue.contains(2));
        assertFalse(queue.contains(1));
    }

    @Test
    public void testIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> new LimitedLiFoQueue<>(0));
        assertThrows(IllegalArgumentException.class, () -> new LimitedLiFoQueue<>(-1));
    }

    @Test
    public void testQueueOperations() {
        LimitedLiFoQueue<Integer> queue = new LimitedLiFoQueue<>(5);
        assertTrue(queue.isEmpty());
        queue.add(1);
        assertFalse(queue.isEmpty());
        assertEquals(1, queue.poll());
        assertTrue(queue.isEmpty());
        assertNull(queue.poll());
    }
}
