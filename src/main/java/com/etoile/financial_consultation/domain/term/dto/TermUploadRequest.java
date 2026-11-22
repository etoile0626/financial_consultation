package com.etoile.financial_consultation.domain.term.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** 약관 업로드 시 함께 전달되는 메타데이터. (FR-1.4) */
@Getter
@Setter
public class TermUploadRequest {

    /** 상품명 */
    @NotBlank
    @Size(max = 200)
    private String productName;

    /** 약관 시행일 */
    @NotNull
    private LocalDate effectiveDate;
}
