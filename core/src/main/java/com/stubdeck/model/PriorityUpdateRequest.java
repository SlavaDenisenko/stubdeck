package com.stubdeck.model;

import lombok.Data;

import java.util.List;

@Data
public class PriorityUpdateRequest {
    private String group;
    private List<String> orderedIds;
}
