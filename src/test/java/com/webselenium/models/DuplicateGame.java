package com.webselenium.models;

import java.util.ArrayList;
import java.util.List;

public class DuplicateGame {
    public final String gameName;
    public final List<String> providers = new ArrayList<>();

    public DuplicateGame(String gameName) {
        this.gameName = gameName;
    }

    public int count() {
        return providers.size();
    }

    @Override
    public String toString() {
        return gameName + " (x" + count() + " — " + String.join(", ", providers) + ")";
    }
}