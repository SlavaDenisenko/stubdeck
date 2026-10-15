package com.stubdeck.service.access;

public class OpenAccessGuard implements AccessGuard {

    @Override
    public boolean canEdit() {
        return true;
    }

    @Override
    public boolean canDelete() {
        return true;
    }

    @Override
    public boolean canPersist() {
        return true;
    }

    @Override
    public boolean canDiscard() {
        return true;
    }

    @Override
    public boolean canManageSettings() {
        return true;
    }
}
