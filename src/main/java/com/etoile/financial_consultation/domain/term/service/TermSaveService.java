package com.etoile.financial_consultation.domain.term.service;

import com.etoile.financial_consultation.domain.term.dto.TermUploadRequest;
import com.etoile.financial_consultation.domain.term.entity.Term;
import com.etoile.financial_consultation.domain.term.entity.TermClause;
import com.etoile.financial_consultation.domain.term.parser.ParsedClause;
import com.etoile.financial_consultation.domain.term.parser.ParsedTerm;
import com.etoile.financial_consultation.domain.term.repository.TermClauseRepository;
import com.etoile.financial_consultation.domain.term.repository.TermRepository;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 파싱 결과와 업로드 메타데이터를 Term / TermClause로 저장한다. (FR-1.4)
 * 임베딩은 이후 단계(FR-1.5)에서 채우므로 여기서는 null로 저장된다.
 */
@Service
@RequiredArgsConstructor
public class TermSaveService {

    /** 신규 등록 시 버전. 개정 버전 관리는 FR-1.6에서 다룬다. */
    static final int INITIAL_VERSION = 1;

    private final TermRepository termRepository;
    private final TermClauseRepository termClauseRepository;

    /**
     * 파싱된 약관 덩어리마다 Term 한 건을 만들어 조항과 함께 저장한다.
     * 한 문서에 보통약관/특별약관처럼 제1조가 다시 시작되는 덩어리가 여럿이면
     * 조항 번호가 겹치므로 Term을 나누고, 상품명 뒤에 약관 제목을 붙여 구분한다.
     */
    @Transactional
    public List<Term> save(TermUploadRequest request, List<ParsedTerm> parsedTerms) {
        if (parsedTerms.isEmpty()) {
            throw new IllegalArgumentException("파싱된 약관 조항이 없습니다.");
        }

        boolean multiple = parsedTerms.size() > 1;
        return IntStream.range(0, parsedTerms.size())
                .mapToObj(i -> saveOne(request, parsedTerms.get(i), multiple ? i + 1 : 0))
                .toList();
    }

    private Term saveOne(TermUploadRequest request, ParsedTerm parsed, int order) {
        Term term = termRepository.save(Term.builder()
                .productName(resolveProductName(request.getProductName(), parsed.title(), order))
                .version(INITIAL_VERSION)
                .effectiveDate(request.getEffectiveDate())
                .build());

        List<TermClause> clauses = parsed.clauses().stream()
                .map(clause -> toEntity(term, clause))
                .toList();
        termClauseRepository.saveAll(clauses);
        return term;
    }

    private TermClause toEntity(Term term, ParsedClause clause) {
        return TermClause.builder()
                .term(term)
                .articleNo(clause.articleNo())
                .articleSubNo(clause.articleSubNo())
                .paragraphNo(clause.paragraphNo())
                .content(clause.content())
                .build();
    }

    /** order가 0이면 단일 약관이므로 상품명 그대로, 아니면 제목(없으면 순번)을 붙인다. */
    private String resolveProductName(String productName, String title, int order) {
        if (order == 0) {
            return productName;
        }
        String suffix = title.isBlank() ? "약관 " + order : title;
        return productName + " - " + suffix;
    }
}
