package com.stubdeck.support;

import com.stubdeck.model.pagination.PageItem;
import com.stubdeck.model.pagination.PageResult;

import java.util.ArrayList;
import java.util.List;

public final class PaginationUtils {
    private PaginationUtils() {
    }

    public static List<PageItem> createPageNumbers(int currentPage, int totalPages) {
        List<PageItem> result = new ArrayList<>();

        if (totalPages <= 7) {
            for (int i = 0; i < totalPages; i++) {
                result.add(PageItem.buildPage(i));
            }

            return result;
        }

        result.add(PageItem.buildPage(0));

        int start = Math.max(1, currentPage - 2);
        int end = Math.min(totalPages - 2, currentPage + 2);

        if (start > 1) {
            result.add(PageItem.buildEllipsis());
        }

        for (int i = start; i <= end; i++) {
            result.add(PageItem.buildPage(i));
        }

        if (end < totalPages - 2) {
            result.add(PageItem.buildEllipsis());
        }

        result.add(PageItem.buildPage(totalPages - 1));
        return result;
    }

    public static <T> PageResult<T> paginate(List<T> items, int requestedPage, int size) {
        int totalElements = items.size();
        int totalPages = totalElements == 0 ? 0 : (totalElements + size - 1) / size;
        int page = totalPages == 0 ? 0 : Math.min(requestedPage, totalPages - 1);

        int from = Math.min(page * size, totalElements);
        int to = Math.min(from + size, totalElements);

        return new PageResult<>(items.subList(from, to), page, size, totalPages, totalElements);
    }
}
