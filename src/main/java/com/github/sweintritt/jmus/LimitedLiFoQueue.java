package com.github.sweintritt.jmus;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * A LIFO queue with a limited size.
 * If the limit is reached, the oldest element is removed when a new element is added.
 * The most recently added element is the first to be returned.
 *
 * @param <E> the type of elements held in this queue
 */
public class LimitedLiFoQueue<E> implements Queue<E> {

    private final int limit;
    private final LinkedList<E> list = new LinkedList<>();

    /**
     * Constructs a new LimitedLiFoQueue with the specified limit.
     *
     * @param limit the maximum number of elements this queue can hold
     * @throws IllegalArgumentException if the limit is less than or equal to 0
     */
    public LimitedLiFoQueue(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than 0");
        }
        this.limit = limit;
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return list.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return list.iterator();
    }

    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return list.toArray(a);
    }

    @Override
    public boolean add(E e) {
        if (list.size() >= limit) {
            list.removeLast();
        }
        list.addFirst(e);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        return list.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return list.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean modified = false;
        for (E e : c) {
            if (add(e)) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return list.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return list.retainAll(c);
    }

    @Override
    public void clear() {
        list.clear();
    }

    @Override
    public boolean offer(E e) {
        if (list.size() >= limit) {
            list.removeLast();
        }
        list.addFirst(e);
        return true;
    }

    @Override
    public E remove() {
        return list.removeFirst();
    }

    @Override
    public E poll() {
        return list.pollFirst();
    }

    @Override
    public E element() {
        return list.getFirst();
    }

    @Override
    public E peek() {
        return list.peekFirst();
    }

    @Override
    public String toString() {
        return list.toString();
    }
}
