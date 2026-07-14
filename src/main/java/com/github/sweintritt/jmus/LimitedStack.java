package com.github.sweintritt.jmus;

import java.util.ArrayDeque;
import java.util.Collection;

/**
 * A stack with a limited size. If the limit is reached, the oldest element is removed
 * when a new element is added. The most recently added element is the first to be returned.
 *
 * @param <E> the type of elements held in this queue
 */
public class LimitedStack<E> extends ArrayDeque<E> {

    private final int limit;

    /**
     * Constructs a new LimitedArrayQueue with the specified limit.
     *
     * @param limit the maximum number of elements this queue can hold
     * @throws IllegalArgumentException if the limit is less than or equal to 0
     */
    public LimitedStack(final int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than 0");
        }
        this.limit = limit;
    }

    @Override
    public void addFirst(final E e) {
        while (size() >= limit) {
            removeLast();
        }
        super.addFirst(e);
    }

    @Override
    public boolean offer(final E e) {
       return this.offerFirst(e);
    }

    @Override
    public boolean add(final E e) {
        this.addFirst(e);
        return true;
    }

    @Override
    public boolean addAll(final Collection<? extends E> c) {
        boolean modified = false;
        for (final E e : c) {
            modified |= add(e);
        }
        return modified;
    }
}
