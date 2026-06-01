package com.webselenium.models;

public class GameCheckResult {

    public final String lobby;
    public final String provider;
    public final String gameName;
    public final boolean playable;
    public final String reason;
    public final OpenMode mode;

    public enum OpenMode { NEW_TAB, IFRAME, NONE }

    public GameCheckResult(String lobby, String provider, String gameName,
                           boolean playable, OpenMode mode, String reason) {
        this.lobby = lobby;
        this.provider = provider;
        this.gameName = gameName;
        this.playable = playable;
        this.mode = mode;
        this.reason = reason == null ? "" : reason;
    }

    @Override
    public String toString() {
        return String.format("[%s / %s] %s -> %s (%s)%s",
                lobby,
                provider == null || provider.isBlank() ? "-" : provider,
                gameName == null || gameName.isBlank() ? "(unnamed)" : gameName,
                playable ? "PASS" : "FAIL",
                mode,
                playable ? "" : " | " + reason);
    }
}