package com.northbank.customerservice.dto.response;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    void from_copiesContentAndPagingMetadata() {
        Page<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(2, 2), 7);

        PageResponse<String> response = PageResponse.from(page);

        assertThat(response.getContent()).containsExactly("a", "b");
        assertThat(response.getPage()).isEqualTo(2);
        assertThat(response.getSize()).isEqualTo(2);
        assertThat(response.getTotalElements()).isEqualTo(7);
        assertThat(response.getTotalPages()).isEqualTo(4);
    }

    @Test
    void from_withEmptyPage_returnsEmptyContentAndZeroTotals() {
        PageResponse<String> response =
                PageResponse.from(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
        assertThat(response.getTotalPages()).isZero();
    }
}
