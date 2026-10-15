package com.stubdeck.model.pagination;

public record MappingQuery(String search, String method, int page, int size) {

    public MappingQuery(String search, String method, int page, int size) {
        this.search = search;
        this.method = method;
        this.page = Math.max(page, 0);
        this.size = size > 0 ? size : 20;
    }
}
