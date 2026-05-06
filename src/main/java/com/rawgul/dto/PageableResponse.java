package com.rawgul.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic pageable response wrapper for API responses.
 * Contains paginated data along with pagination metadata.
 *
 * @param <T> The type of data in the response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageableResponse<T> {
    
    /**
     * The list of items for the current page
     */
    private List<T> content;
    
    /**
     * Current page number (0-indexed)
     */
    private int page;
    
    /**
     * Number of items per page
     */
    private int size;
    
    /**
     * Total number of items across all pages
     */
    private long totalElements;
    
    /**
     * Total number of pages
     */
    private int totalPages;
    
    /**
     * Whether this is the first page
     */
    private boolean first;
    
    /**
     * Whether this is the last page
     */
    private boolean last;
    
    /**
     * Whether there is a next page
     */
    private boolean hasNext;
    
    /**
     * Whether there is a previous page
     */
    private boolean hasPrevious;
    
    /**
     * Number of items in the current page
     */
    private int numberOfElements;
    
    /**
     * Whether the page is empty
     */
    private boolean empty;

    /**
     * Create a PageableResponse from Spring Data Page object
     */
    public static <T> PageableResponse<T> fromPage(org.springframework.data.domain.Page<T> page) {
        PageableResponse<T> response = new PageableResponse<>();
        response.setContent(page.getContent());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setFirst(page.isFirst());
        response.setLast(page.isLast());
        response.setHasNext(page.hasNext());
        response.setHasPrevious(page.hasPrevious());
        response.setNumberOfElements(page.getNumberOfElements());
        response.setEmpty(page.isEmpty());
        return response;
    }
}
