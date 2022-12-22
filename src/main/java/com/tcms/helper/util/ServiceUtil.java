package com.tcms.helper.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class ServiceUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceUtil.class);

    public static Specification<Specification> withValue(String key, String value) {
        return (root, query, builder) -> builder.like(root.get(key).as(String.class), "%" + value + "%");
    }

    public static Specification<Specification> withValueEquals(String key, String value) {
        return (root, query, builder) -> builder.equal(root.get(key), value);
    }

    public static Specification<Specification> withValueNotEquals(String key, String value) {
        return (root, query, builder) -> builder.notEqual(root.get(key), value);
    }

    public static Specification<Specification> withMaxValue(String key, String value) {
        return (root, query, builder) -> builder.like(root.get(key), "%" + value + "%");
    }

    public static Specification<Specification> withDate(String key, String value) {
        try {
            Date plusOne = (new SimpleDateFormat("yyyy-MM-dd").parse(value));
            Calendar c = Calendar.getInstance();
            c.setTime(plusOne);
            c.add(Calendar.DATE, 1);
            return (root, query, builder) -> {
                try {
                    return builder.between(root.get(key).as(Date.class), new SimpleDateFormat("yyyy-MM-dd").parse(value), c.getTime());
                } catch (ParseException e) {
                    LOGGER.error(e.getMessage(), e);
                }
                return null;
            };
        } catch (ParseException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    public static Pageable sortData(String sortBy, String orderBy, Integer page, Integer size, String defaultSortValue) {
        Pageable perPageRecords;
        if (sortBy == null || "".equalsIgnoreCase(sortBy)) {
            perPageRecords = PageRequest
                    .of(page, size, Sort.by(defaultSortValue).descending());
        } else {
            if ("asc".equalsIgnoreCase(orderBy)) {
                perPageRecords = PageRequest.of(page, size, Sort.by(sortBy).ascending());
            } else {
                perPageRecords = PageRequest.of(page, size, Sort.by(sortBy).descending());
            }
        }
        return perPageRecords;
    }
}
