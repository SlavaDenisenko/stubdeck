package com.stubdeck.model.changes;

import com.stubdeck.model.ChangeAction;
import com.stubdeck.model.WireMockMapping;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PendingChange {
    private String id;
    private String name;
    private String method;
    private String url;
    private ChangeAction action;
    /**
     * Snapshot before the change - null for CREATE. Used to revert (discard).
     */
    private WireMockMapping previousSnapshot;
    /**
     * Snapshot after the change - null for DELETE.
     */
    private WireMockMapping currentSnapshot;
    private String changedBy;
    private String changedByDisplay;
    private String changedByEmail;
    private String changedAt;
}
