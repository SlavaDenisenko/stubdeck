package com.stubdeck.model.pagination;

public record PageItem(Integer page, boolean ellipsis) {

    public static PageItem buildPage(int page) {
        return new PageItem(page, false);
    }

    public static PageItem buildEllipsis() {
        return new PageItem(null, true);
    }
}
