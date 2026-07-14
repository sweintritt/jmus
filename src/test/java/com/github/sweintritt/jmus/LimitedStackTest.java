package com.github.sweintritt.jmus;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Arrays;
import java.util.List;

class LimitedStackTest {

    @Test
    void testLimitReached() {
        var queue = new LimitedStack<>(3);
        queue.add(1);
        queue.add(2);
        queue.add(3);
        assertThat(queue)
                .hasSize(3)
                .contains(3);
        queue.add(4);
        assertThat(queue)
                .hasSize(3)
                .doesNotContain(1)
                .contains(4);
        assertThat(queue.peek()).isEqualTo(4);
    }

    @Test
    void testOfferEviction() {
        var queue = new LimitedStack<>(2);
        queue.offer("A");
        queue.offer("B");
        assertThat(queue).hasSize(2);
        queue.offer("C");
        assertThat(queue).hasSize(2);
        assertThat(queue.peek()).isEqualTo("C");
        assertThat(queue).doesNotContain("A");
    }

    @Test
    void testAddAllEviction() {
        var queue = new LimitedStack<>(2);
        queue.addAll(Arrays.asList(1, 2, 3));
        assertThat(queue).hasSize(2);
        assertThat(queue.peek()).isEqualTo(3);
        assertThat(queue).containsOnly(2, 3);
    }

    @Test
    void testIllegalArgument() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new LimitedStack<>(0));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new LimitedStack<>(-1));
    }

    @Test
    void testQueueOperations() {
        var queue = new LimitedStack<>(5);
        assertThat(queue).isEmpty();
        queue.add(1);
        assertThat(queue).isNotEmpty();
        assertThat(queue.poll()).isEqualTo(1);
        assertThat(queue).isEmpty();
        assertThat(queue.poll()).isNull();
    }

    @Test
    void testAlwaysInsertFirst() {
        var stack = new LimitedStack<>(10);
        assertThat(stack.peek()).isNull();
        stack.add(1);
        assertThat(stack.peek()).isEqualTo(1);
        stack.offer(2);
        assertThat(stack.peek()).isEqualTo(2);
        stack.addAll(List.of(3, 4, 5));
        assertThat(stack.peek()).isEqualTo(5);
    }
}
