package com.etoile.financial_consultation.domain.term.parser;

/**
 * 파싱된 조항 한 건. 번호 체계는 term_clauses 컬럼과 동일하다.
 *
 * @param articleNo    제n조
 * @param articleSubNo 제n조의m (없으면 0)
 * @param paragraphNo  항 (조 본문이면 0)
 * @param content      조 제목 + 본문
 */
public record ParsedClause(int articleNo, int articleSubNo, int paragraphNo, String content) {
}
