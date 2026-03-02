package com.water.data.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for list uploads endpoint with pagination.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadListResponse {

    private List<UploadSummaryDto> uploads;
    private PaginationMetadata pagination;

    /**
     * Pagination metadata.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationMetadata {
        private Integer page;           // Current page (0-indexed)
        private Integer pageSize;       // Items per page
        private Long totalItems;        // Total number of items
        private Integer totalPages;     // Total number of pages
        private Boolean hasNext;        // Has next page
        private Boolean hasPrevious;    // Has previous page

        /**
         * Creates pagination metadata.
         *
         * @param page Current page (0-indexed)
         * @param pageSize Items per page
         * @param totalItems Total number of items
         * @return PaginationMetadata
         */
        public static PaginationMetadata create(int page, int pageSize, long totalItems) {
            int totalPages = (int) Math.ceil((double) totalItems / pageSize);
            boolean hasNext = page < (totalPages - 1);
            boolean hasPrevious = page > 0;

            return new PaginationMetadata(
                page,
                pageSize,
                totalItems,
                totalPages,
                hasNext,
                hasPrevious
            );
        }
    }

    /**
     * Creates upload list response with pagination.
     *
     * @param uploads List of upload summaries
     * @param page Current page
     * @param pageSize Page size
     * @param totalItems Total items
     * @return UploadListResponse
     */
    public static UploadListResponse create(
        List<UploadSummaryDto> uploads,
        int page,
        int pageSize,
        long totalItems
    ) {
        PaginationMetadata pagination = PaginationMetadata.create(page, pageSize, totalItems);
        return new UploadListResponse(uploads, pagination);
    }
}
