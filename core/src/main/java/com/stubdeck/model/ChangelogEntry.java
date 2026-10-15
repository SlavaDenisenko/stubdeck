package com.stubdeck.model;

public record ChangelogEntry(String timestamp, String action, String id,
                             String filename, String endpoint, String username) {
}
