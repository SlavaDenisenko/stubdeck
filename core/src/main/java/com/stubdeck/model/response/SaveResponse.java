package com.stubdeck.model.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SaveResponse {
    private boolean saved;
    private boolean hasConflict;
    private String conflictKey;
    private String error;
    private String strategy;
}
