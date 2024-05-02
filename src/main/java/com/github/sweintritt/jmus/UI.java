package com.github.sweintritt.jmus;

import java.util.LinkedList;
import java.util.List;

public class UI {

    private final String prompt = "jmus> ";
    private final int rows = 49;
    private final List<String> messages = new LinkedList<>();

    public void draw() {
        System.out.println("\033[2J");
        final int start = Math.max(0, messages.size() - rows);

        for (int i = 0; i < Math.max(0, rows - messages.size()); ++i) {
            System.out.println("");
        }

        for (int i = start; i < messages.size(); ++i) {
            System.out.println(messages.get(i));
        }

        System.out.print(prompt);
    }

    public void addMessage(final String msg) {
        this.messages.add(msg);
        if (messages.size() > rows) {
            this.messages.removeFirst();
        }
    }
}
