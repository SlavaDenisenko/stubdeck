package com.stubdeck.model.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeleteResponse {
    private String error;
    private String lockedBy;
    private String lockedByEmail;
}
