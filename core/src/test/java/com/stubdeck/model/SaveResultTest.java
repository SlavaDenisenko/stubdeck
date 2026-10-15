package com.stubdeck.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SaveResultTest {

    @Test
    void ok_savedWithNoConflict() {
        SaveResult r = SaveResult.ok();
        assertThat(r.saved()).isTrue();
        assertThat(r.hasConflict()).isFalse();
        assertThat(r.conflictKey()).isNull();
        assertThat(r.error()).isNull();
    }

    @Test
    void conflict_notSaved() {
        SaveResult r = SaveResult.conflict("GET /api/users");
        assertThat(r.saved()).isFalse();
        assertThat(r.hasConflict()).isTrue();
        assertThat(r.conflictKey()).isEqualTo("GET /api/users");
        assertThat(r.error()).isNull();
    }

    @Test
    void warnAndSave_savedWithConflict() {
        SaveResult r = SaveResult.warnAndSave("POST /api/items");
        assertThat(r.saved()).isTrue();
        assertThat(r.hasConflict()).isTrue();
        assertThat(r.conflictKey()).isEqualTo("POST /api/items");
        assertThat(r.error()).isNull();
    }

    @Test
    void error_notSavedWithMessage() {
        SaveResult r = SaveResult.error("connection refused");
        assertThat(r.saved()).isFalse();
        assertThat(r.hasConflict()).isFalse();
        assertThat(r.conflictKey()).isNull();
        assertThat(r.error()).isEqualTo("connection refused");
    }
}
