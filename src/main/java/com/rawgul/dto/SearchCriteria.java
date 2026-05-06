package com.rawgul.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Search criteria for dynamic query building.
 * Represents a single search condition with key, operation, and value.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchCriteria {
    
    /**
     * The field name to search on (e.g., "email", "status", "createdDate")
     */
    private String key;
    
    /**
     * The comparison operation to perform.
     * Supported operations:
     * - ":" (EQUALS) - Exact match
     * - ">" (GREATER_THAN) - Greater than comparison
     * - "<" (LESS_THAN) - Less than comparison
     * - ">=" (GREATER_THAN_OR_EQUAL) - Greater than or equal comparison
     * - "<=" (LESS_THAN_OR_EQUAL) - Less than or equal comparison
     * - "~" (LIKE) - Case-insensitive partial match (contains)
     * - "!:" (NOT_EQUAL) - Not equal comparison
     * - "^" (STARTS_WITH) - Starts with comparison (case-insensitive)
     * - "$" (ENDS_WITH) - Ends with comparison (case-insensitive)
     * - "in" (IN) - Value is in a list
     * - "!in" (NOT_IN) - Value is not in a list
     */
    private String operation;
    
    /**
     * The value to compare against.
     * Can be a String, Number, Boolean, Date, or Collection for IN operations.
     */
    private Object value;
    
    /**
     * Optional: Join type for associated entities (e.g., "user", "ticket")
     */
    private String joinTable;
    
    /**
     * Constructor for simple criteria without join.
     *
     * @param key The field name
     * @param operation The comparison operation
     * @param value The value to compare
     */
    public SearchCriteria(String key, String operation, Object value) {
        this.key = key;
        this.operation = operation;
        this.value = value;
    }
}
