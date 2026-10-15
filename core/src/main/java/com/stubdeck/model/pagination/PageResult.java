package com.stubdeck.model.pagination;

import java.util.List;

public record PageResult<T>(List<T> content, int page, int size, int totalPages, long totalElements) {

    public boolean isFirst() {
        return page == 0;
    }

    public boolean isLast() {
        return totalPages == 0 || page >= totalPages - 1;
    }

    public long getFromElement() {
        return totalElements == 0 ? 0 : (long) page * size + 1;
    }

    public long getToElement() {
        return Math.min((long) (page + 1) * size, totalElements);
    }
}
