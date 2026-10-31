package com.apexflow.util;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class QueryFilterParserTest {

    @Test
    void testPageableExtractionDefaults() {
        Map<String, String> params = new HashMap<>();
        Pageable pageable = QueryFilterParser.getPageable(params);

        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().isUnsorted()).isTrue();
    }

    @Test
    void testPageableExtractionCustom() {
        Map<String, String> params = new HashMap<>();
        params.put("_page", "2");
        params.put("_limit", "25");
        params.put("_sort", "name");
        params.put("_order", "desc");

        Pageable pageable = QueryFilterParser.getPageable(params);

        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(25);
        assertThat(pageable.getSort().getOrderFor("name")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void testPageableExtractionInvalidNumbersFallback() {
        Map<String, String> params = new HashMap<>();
        params.put("_page", "invalid");
        params.put("_limit", "-5");
        params.put("_sort", "category");
        params.put("_order", "asc");

        Pageable pageable = QueryFilterParser.getPageable(params);

        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(1);
        assertThat(pageable.getSort().getOrderFor("category")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("category").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void testPageableExtractionNestedSortIgnored() {
        Map<String, String> params = new HashMap<>();
        params.put("_sort", "address.city");

        Pageable pageable = QueryFilterParser.getPageable(params);

        assertThat(pageable.getSort().isUnsorted()).isTrue();
    }

    @Test
    void testSpecificationIgnoresReservedKeysAndBlankValues() {
        Map<String, String> params = new HashMap<>();
        params.put("_page", "1");
        params.put("_limit", "10");
        params.put("_sort", "id");
        params.put("_order", "asc");
        params.put("_embed", "orders");
        params.put("_expand", "customer");
        params.put("_refresh", "true");
        params.put("q", "search");
        params.put("blankField", "   ");
        params.put("nullField", null);

        Specification<Object> spec = QueryFilterParser.parse(params);
        assertThat(spec).isNotNull();

        Root<Object> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate dummyPredicate = mock(Predicate.class);
        when(cb.and(any(Predicate[].class))).thenReturn(dummyPredicate);

        Predicate result = spec.toPredicate(root, query, cb);
        assertThat(result).isEqualTo(dummyPredicate);
        verify(root, never()).get(anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSpecificationWithExactMatchAndLikeFilters() {
        Map<String, String> params = new HashMap<>();
        params.put("status", "Attivo");
        params.put("name_like", "Acme");

        Specification<Object> spec = QueryFilterParser.parse(params);
        assertThat(spec).isNotNull();

        Root<Object> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Path statusPath = mock(Path.class);
        Path namePath = mock(Path.class);
        when(root.get("status")).thenReturn(statusPath);
        when(root.get("name")).thenReturn(namePath);

        jakarta.persistence.criteria.Expression<String> lowerExpr = mock(jakarta.persistence.criteria.Expression.class);
        when(cb.lower(namePath)).thenReturn(lowerExpr);

        Predicate likePredicate = mock(Predicate.class);
        when(cb.like(lowerExpr, "%acme%")).thenReturn(likePredicate);

        Predicate equalPredicate = mock(Predicate.class);
        when(cb.equal(statusPath, "Attivo")).thenReturn(equalPredicate);

        Predicate combinedPredicate = mock(Predicate.class);
        when(cb.and(any(Predicate[].class))).thenReturn(combinedPredicate);

        Predicate result = spec.toPredicate(root, query, cb);
        assertThat(result).isEqualTo(combinedPredicate);

        verify(cb).equal(statusPath, "Attivo");
        verify(cb).like(lowerExpr, "%acme%");
    }
}
