package com.stubdeck.model;

public record SaveResult(boolean saved, boolean hasConflict, String conflictKey, String error) {

    public static SaveResult ok() {
        return new SaveResult(true, false, null, null);
    }

    public static SaveResult conflict(String key) {
        return new SaveResult(false, true, key, null);
    }

    public static SaveResult warnAndSave(String key) {
        return new SaveResult(true, true, key, null);
    }

    public static SaveResult error(String msg) {
        return new SaveResult(false, false, null, msg);
    }
}
