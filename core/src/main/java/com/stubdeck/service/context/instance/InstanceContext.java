package com.stubdeck.service.context.instance;

public interface InstanceContext {

    String getCurrentInstanceId();

    default boolean isAvailable() {
        return false;
    }
}
