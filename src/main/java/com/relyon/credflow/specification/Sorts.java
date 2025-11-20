package com.relyon.credflow.specification;

import org.springframework.data.domain.Sort;

public final class Sorts {
    private Sorts() {
    }

    public static Sort resolve(Sort sort) {
        return (sort == null || sort.isUnsorted())
                ? Sort.by(Sort.Order.desc("date"))
                : sort;
    }
}