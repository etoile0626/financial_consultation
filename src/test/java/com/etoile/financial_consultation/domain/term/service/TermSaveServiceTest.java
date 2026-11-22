package com.etoile.financial_consultation.domain.term.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.etoile.financial_consultation.domain.term.dto.TermUploadRequest;
import com.etoile.financial_consultation.domain.term.entity.Term;
import com.etoile.financial_consultation.domain.term.parser.ParsedClause;
import com.etoile.financial_consultation.domain.term.parser.ParsedTerm;
import com.etoile.financial_consultation.domain.term.repository.TermClauseRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** 로컬/CI의 PostgreSQL(pgvector)에 연결해 저장 결과를 검증한다. 각 테스트는 롤백된다. */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TermSaveService.class)
class TermSaveServiceTest {

    @Autowired
    private TermSaveService termSaveService;

    @Autowired
    private TermClauseRepository termClauseRepository;

    @Test
    void 파싱_결과가_메타데이터와_함께_저장된다() {
        ParsedTerm parsed = new ParsedTerm("테스트 보통약관", List.of(
                new ParsedClause(1, 0, 0, "제1조(목적)\n본문"),
                new ParsedClause(12, 0, 2, "제12조(테스트) ② 본문"),
                new ParsedClause(12, 3, 0, "제12조의3(추가)\n본문")));

        List<Term> saved = termSaveService.save(request("테스트 상품", LocalDate.of(2026, 1, 1)), List.of(parsed));

        assertThat(saved).hasSize(1);
        Term term = saved.get(0);
        assertThat(term.getProductName()).isEqualTo("테스트 상품");
        assertThat(term.getVersion()).isEqualTo(1);
        assertThat(term.getEffectiveDate()).isEqualTo(LocalDate.of(2026, 1, 1));

        var clauses = termClauseRepository.findAll().stream()
                .filter(c -> c.getTerm().getId().equals(term.getId())).toList();
        assertThat(clauses).hasSize(3);
        assertThat(clauses).anySatisfy(c -> {
            assertThat(c.getArticleNo()).isEqualTo(12);
            assertThat(c.getParagraphNo()).isEqualTo(2);
            assertThat(c.getEmbedding()).isNull();
        });
        assertThat(clauses).anySatisfy(c -> assertThat(c.getArticleSubNo()).isEqualTo(3));
    }

    @Test
    void 제1조가_다시_시작되는_약관은_Term을_나눠_저장한다() {
        ParsedTerm general = new ParsedTerm("테스트 보통약관", List.of(new ParsedClause(1, 0, 0, "제1조(목적)\n보통")));
        ParsedTerm special = new ParsedTerm("", List.of(new ParsedClause(1, 0, 0, "제1조(목적)\n특별")));

        List<Term> saved = termSaveService.save(request("테스트 상품", LocalDate.of(2026, 1, 1)), List.of(general, special));

        assertThat(saved).extracting(Term::getProductName)
                .containsExactly("테스트 상품 - 테스트 보통약관", "테스트 상품 - 약관 2");
    }

    @Test
    void 파싱된_조항이_없으면_예외가_발생한다() {
        assertThatThrownBy(() -> termSaveService.save(request("테스트 상품", LocalDate.of(2026, 1, 1)), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private TermUploadRequest request(String productName, LocalDate effectiveDate) {
        TermUploadRequest request = new TermUploadRequest();
        request.setProductName(productName);
        request.setEffectiveDate(effectiveDate);
        return request;
    }
}
