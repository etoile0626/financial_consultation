package com.etoile.financial_consultation.domain.term.parser;

import java.util.List;

/**
 * 한 문서 안에서 제1조부터 번호가 새로 시작하는 약관 한 덩어리 (보통약관, 특별약관 등).
 *
 * @param title   약관 이름 (찾지 못하면 빈 문자열)
 * @param clauses 조항 목록
 */
public record ParsedTerm(String title, List<ParsedClause> clauses) {
}
