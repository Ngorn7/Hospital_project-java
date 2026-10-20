package com.setec.hms.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Builds a Pageable from the page, size and "field,asc|desc" sort parameters sent by the UI. */
final class Paging {
  static final int MAX_SIZE = 100;

  private Paging() {}

  static Pageable of(int page, int size, String sort, String fallbackSort) {
    String spec = (sort == null || sort.isBlank()) ? fallbackSort : sort;
    String[] parts = spec.split(",");
    String field = parts[0].trim();
    Sort.Direction direction =
        parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc")
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;
    return PageRequest.of(
        Math.max(page, 0), Math.min(Math.max(size, 1), MAX_SIZE), Sort.by(direction, field));
  }
}
