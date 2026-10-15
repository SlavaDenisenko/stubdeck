package com.stubdeck.service.context.instance;

import org.springframework.context.annotation.Fallback;
import org.springframework.stereotype.Component;

@Component
@Fallback
public class DefaultInstanceContext implements InstanceContext {

    @Override
    public String getCurrentInstanceId() {
        return "default";
    }
}
