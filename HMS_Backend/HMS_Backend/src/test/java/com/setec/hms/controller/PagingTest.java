package com.setec.hms.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class PagingTest {

  @Test
  void parsesFieldAndDirection() {
    var pageable = Paging.of(2, 10, "fullName,desc", "id,asc");
    assertEquals(2, pageable.getPageNumber());
    assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("fullName").getDirection());
  }

  @Test
  void fallsBackWhenSortMissingAndClampsSize() {
    var pageable = Paging.of(-1, 5000, null, "admittedAt,desc");
    assertEquals(0, pageable.getPageNumber());
    assertEquals(Paging.MAX_SIZE, pageable.getPageSize());
    assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("admittedAt").getDirection());
  }
}
