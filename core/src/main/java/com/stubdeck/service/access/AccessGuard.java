package com.stubdeck.service.access;

public interface AccessGuard {

    boolean canEdit();

    boolean canDelete();

    boolean canPersist();

    boolean canDiscard();

    boolean canManageSettings();
}
