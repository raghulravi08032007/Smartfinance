package com.rawgul.specification;

import com.rawgul.dto.SearchCriteria;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for combining multiple search criteria into a single Specification.
 * Supports AND/OR operations for complex queries.
 *
 * @param <T> The entity type
 */
public class SpecificationBuilder<T> {

    private final List<SearchCriteria> criteriaList;
    private boolean orOperation = false;

    public SpecificationBuilder() {
        this.criteriaList = new ArrayList<>();
    }

    /**
     * Add a search criterion with AND operation
     *
     * @param key The field name
     * @param operation The comparison operation
     * @param value The value to compare
     * @return This builder instance
     */
    public SpecificationBuilder<T> with(String key, String operation, Object value) {
        criteriaList.add(new SearchCriteria(key, operation, value));
        return this;
    }

    /**
     * Add a search criterion with join
     *
     * @param key The field name
     * @param operation The comparison operation
     * @param value The value to compare
     * @param joinTable The table to join
     * @return This builder instance
     */
    public SpecificationBuilder<T> with(String key, String operation, Object value, String joinTable) {
        criteriaList.add(new SearchCriteria(key, operation, value, joinTable));
        return this;
    }

    /**
     * Add a complete SearchCriteria object
     *
     * @param criteria The search criteria
     * @return This builder instance
     */
    public SpecificationBuilder<T> with(SearchCriteria criteria) {
        criteriaList.add(criteria);
        return this;
    }

    /**
     * Set operation mode to OR instead of AND
     *
     * @return This builder instance
     */
    public SpecificationBuilder<T> withOr() {
        this.orOperation = true;
        return this;
    }

    /**
     * Build the final Specification from all added criteria
     *
     * @return The combined Specification, or null if no criteria
     */
    public Specification<T> build() {
        if (criteriaList.isEmpty()) {
            return null;
        }

        Specification<T> result = new GenericSpecification<>(criteriaList.get(0));

        for (int i = 1; i < criteriaList.size(); i++) {
            SearchCriteria criteria = criteriaList.get(i);
            Specification<T> spec = new GenericSpecification<>(criteria);

            result = orOperation
                    ? Specification.where(result).or(spec)
                    : Specification.where(result).and(spec);
        }

        return result;
    }

    /**
     * Clear all criteria
     */
    public void clear() {
        criteriaList.clear();
        orOperation = false;
    }

    /**
     * Get the number of criteria
     */
    public int size() {
        return criteriaList.size();
    }
}
