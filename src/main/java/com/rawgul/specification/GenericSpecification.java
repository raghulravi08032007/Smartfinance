package com.rawgul.specification;

import com.rawgul.dto.SearchCriteria;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;

/**
 * Generic JPA Specification for dynamic query building.
 * Implements the Specification pattern for type-safe criteria queries.
 *
 * @param <T> The entity type
 */
public class GenericSpecification<T> implements Specification<T> {

    private final SearchCriteria criteria;

    public GenericSpecification(SearchCriteria criteria) {
        this.criteria = criteria;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        try {
            // Handle join if specified
            Path<?> path;
            if (criteria.getJoinTable() != null && !criteria.getJoinTable().isEmpty()) {
                Join<Object, Object> join = root.join(criteria.getJoinTable(), JoinType.LEFT);
                path = join.get(criteria.getKey());
            } else {
                path = root.get(criteria.getKey());
            }

            // Handle different operations
            switch (criteria.getOperation()) {
                case ":": // EQUALS
                    return builder.equal(path, criteria.getValue());

                case "!:": // NOT_EQUAL
                    return builder.notEqual(path, criteria.getValue());

                case ">": // GREATER_THAN
                    return handleGreaterThan(path, builder);

                case "<": // LESS_THAN
                    return handleLessThan(path, builder);

                case ">=": // GREATER_THAN_OR_EQUAL
                    return handleGreaterThanOrEqual(path, builder);

                case "<=": // LESS_THAN_OR_EQUAL
                    return handleLessThanOrEqual(path, builder);

                case "~": // LIKE (contains)
                    return builder.like(
                            builder.lower(path.as(String.class)),
                            "%" + criteria.getValue().toString().toLowerCase() + "%"
                    );

                case "^": // STARTS_WITH
                    return builder.like(
                            builder.lower(path.as(String.class)),
                            criteria.getValue().toString().toLowerCase() + "%"
                    );

                case "$": // ENDS_WITH
                    return builder.like(
                            builder.lower(path.as(String.class)),
                            "%" + criteria.getValue().toString().toLowerCase()
                    );

                case "in": // IN
                    if (criteria.getValue() instanceof Collection) {
                        return path.in((Collection<?>) criteria.getValue());
                    }
                    return builder.equal(path, criteria.getValue());

                case "!in": // NOT_IN
                    if (criteria.getValue() instanceof Collection) {
                        return builder.not(path.in((Collection<?>) criteria.getValue()));
                    }
                    return builder.notEqual(path, criteria.getValue());

                default:
                    return null;
            }
        } catch (Exception e) {
            // Log error and return null predicate
            System.err.println("Error building predicate for key: " + criteria.getKey() + ", error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Handle greater than comparison for different types
     */
    @SuppressWarnings("unchecked")
    private Predicate handleGreaterThan(Path<?> path, CriteriaBuilder builder) {
        if (criteria.getValue() instanceof Number) {
            return builder.gt((Expression<Number>) path, (Number) criteria.getValue());
        } else if (criteria.getValue() instanceof Date) {
            return builder.greaterThan((Expression<Date>) path, (Date) criteria.getValue());
        } else if (criteria.getValue() instanceof LocalDate) {
            return builder.greaterThan((Expression<LocalDate>) path, (LocalDate) criteria.getValue());
        } else if (criteria.getValue() instanceof LocalDateTime) {
            return builder.greaterThan((Expression<LocalDateTime>) path, (LocalDateTime) criteria.getValue());
        } else if (criteria.getValue() instanceof Comparable) {
            return builder.greaterThan((Expression<Comparable>) path, (Comparable) criteria.getValue());
        }
        return null;
    }

    /**
     * Handle less than comparison for different types
     */
    @SuppressWarnings("unchecked")
    private Predicate handleLessThan(Path<?> path, CriteriaBuilder builder) {
        if (criteria.getValue() instanceof Number) {
            return builder.lt((Expression<Number>) path, (Number) criteria.getValue());
        } else if (criteria.getValue() instanceof Date) {
            return builder.lessThan((Expression<Date>) path, (Date) criteria.getValue());
        } else if (criteria.getValue() instanceof LocalDate) {
            return builder.lessThan((Expression<LocalDate>) path, (LocalDate) criteria.getValue());
        } else if (criteria.getValue() instanceof LocalDateTime) {
            return builder.lessThan((Expression<LocalDateTime>) path, (LocalDateTime) criteria.getValue());
        } else if (criteria.getValue() instanceof Comparable) {
            return builder.lessThan((Expression<Comparable>) path, (Comparable) criteria.getValue());
        }
        return null;
    }

    /**
     * Handle greater than or equal comparison for different types
     */
    @SuppressWarnings("unchecked")
    private Predicate handleGreaterThanOrEqual(Path<?> path, CriteriaBuilder builder) {
        if (criteria.getValue() instanceof Number) {
            return builder.ge((Expression<Number>) path, (Number) criteria.getValue());
        } else if (criteria.getValue() instanceof Date) {
            return builder.greaterThanOrEqualTo((Expression<Date>) path, (Date) criteria.getValue());
        } else if (criteria.getValue() instanceof LocalDate) {
            return builder.greaterThanOrEqualTo((Expression<LocalDate>) path, (LocalDate) criteria.getValue());
        } else if (criteria.getValue() instanceof LocalDateTime) {
            return builder.greaterThanOrEqualTo((Expression<LocalDateTime>) path, (LocalDateTime) criteria.getValue());
        } else if (criteria.getValue() instanceof Comparable) {
            return builder.greaterThanOrEqualTo((Expression<Comparable>) path, (Comparable) criteria.getValue());
        }
        return null;
    }

    /**
     * Handle less than or equal comparison for different types
     */
    @SuppressWarnings("unchecked")
    private Predicate handleLessThanOrEqual(Path<?> path, CriteriaBuilder builder) {
        if (criteria.getValue() instanceof Number) {
            return builder.le((Expression<Number>) path, (Number) criteria.getValue());
        } else if (criteria.getValue() instanceof Date) {
            return builder.lessThanOrEqualTo((Expression<Date>) path, (Date) criteria.getValue());
        } else if (criteria.getValue() instanceof LocalDate) {
            return builder.lessThanOrEqualTo((Expression<LocalDate>) path, (LocalDate) criteria.getValue());
        } else if (criteria.getValue() instanceof LocalDateTime) {
            return builder.lessThanOrEqualTo((Expression<LocalDateTime>) path, (LocalDateTime) criteria.getValue());
        } else if (criteria.getValue() instanceof Comparable) {
            return builder.lessThanOrEqualTo((Expression<Comparable>) path, (Comparable) criteria.getValue());
        }
        return null;
    }
}
