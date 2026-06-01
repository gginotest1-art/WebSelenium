package com.webselenium.models;

public class GameInventoryItem {

    public final String lobby;
    public final String provider;
    public final String gameName;

    public GameInventoryItem(String lobby, String provider, String gameName) {
        this.lobby = lobby;
        this.provider = provider == null || provider.isBlank() ? "-" : provider;
        this.gameName = gameName == null || gameName.isBlank() ? "(unnamed)" : gameName;
    }

    @Override
    public String toString() {
        return String.format("[%s / %s] %s", lobby, provider, gameName);
    }
}