package com.etoile.financial_consultation.domain.term.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TermUploadRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void 상품명과_시행일이_있으면_검증을_통과한다() {
        assertThat(validator.validate(request("테스트 상품", LocalDate.of(2026, 1, 1)))).isEmpty();
    }

    @Test
    void 상품명이_비어있으면_검증에_실패한다() {
        assertThat(validator.validate(request(" ", LocalDate.of(2026, 1, 1)))).hasSize(1);
    }

    @Test
    void 시행일이_없으면_검증에_실패한다() {
        assertThat(validator.validate(request("테스트 상품", null))).hasSize(1);
    }

    @Test
    void 상품명이_200자를_넘으면_검증에_실패한다() {
        assertThat(validator.validate(request("가".repeat(201), LocalDate.of(2026, 1, 1)))).hasSize(1);
    }

    private TermUploadRequest request(String productName, LocalDate effectiveDate) {
        TermUploadRequest request = new TermUploadRequest();
        request.setProductName(productName);
        request.setEffectiveDate(effectiveDate);
        return request;
    }
}
