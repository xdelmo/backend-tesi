package com.apexflow.util;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

public class QueryFilterParser {

    private static final Set<String> RESERVED_KEYS = Set.of(
            "_page", "_limit", "_sort", "_order", "_embed", "_expand", "_refresh", "q"
    );

    private QueryFilterParser() {
        // Utility class
    }

    public static Pageable getPageable(Map<String, String> params) {
        if (params == null) {
            params = Collections.emptyMap();
        }

        int page = 1;
        if (params.containsKey("_page")) {
            try {
                page = Math.max(1, Integer.parseInt(params.get("_page")));
            } catch (NumberFormatException ignored) {}
        }

        int limit = 10;
        if (params.containsKey("_limit")) {
            try {
                limit = Math.max(1, Integer.parseInt(params.get("_limit")));
            } catch (NumberFormatException ignored) {}
        }

        Sort sort = Sort.unsorted();
        if (params.containsKey("_sort")) {
            String sortField = params.get("_sort");
            if (sortField != null && !sortField.contains(".")) {
                Sort.Direction direction = "desc".equalsIgnoreCase(params.get("_order"))
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;
                sort = Sort.by(direction, sortField);
            }
        }

        return PageRequest.of(page - 1, limit, sort);
    }

    public static <T> Specification<T> parse(Map<String, String> params) {
        final Map<String, String> queryParams = (params == null) ? Collections.emptyMap() : params;
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (RESERVED_KEYS.contains(key) || value == null || value.trim().isEmpty()) {
                    continue;
                }

                if (key.endsWith("_like")) {
                    String field = key.substring(0, key.length() - 5);
                    Path<String> path = getPath(root, field);
                    if (path != null) {
                        predicates.add(cb.like(cb.lower(path), "%" + value.toLowerCase() + "%"));
                    }
                } else if (!key.contains(".")) {
                    try {
                        Path<Object> path = root.get(key);
                        predicates.add(cb.equal(path, value));
                    } catch (IllegalArgumentException ignored) {}
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> Path<String> getPath(jakarta.persistence.criteria.Root<T> root, String field) {
        try {
            return (Path<String>) (Path<?>) root.get(field);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
