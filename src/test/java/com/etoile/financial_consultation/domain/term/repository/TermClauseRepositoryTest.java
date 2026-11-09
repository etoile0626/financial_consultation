package com.etoile.financial_consultation.domain.term.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.etoile.financial_consultation.domain.term.entity.Term;
import com.etoile.financial_consultation.domain.term.entity.TermClause;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

/** 로컬 Docker PostgreSQL(pgvector)에 연결해 매핑을 검증한다. 각 테스트는 롤백된다. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TermClauseRepositoryTest {

    @Autowired
    private TermRepository termRepository;

    @Autowired
    private TermClauseRepository termClauseRepository;

    @Autowired
    private EntityManager em;

    @Test
    void 임베딩_벡터를_저장하고_다시_읽을_수_있다() {
        Term term = termRepository.save(newTerm());
        float[] vector = new float[TermClause.EMBEDDING_DIMENSION];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = i / 1000f;
        }

        Long id = termClauseRepository.save(TermClause.builder()
                .term(term).articleNo(12).paragraphNo(2)
                .content("제12조(테스트) 테스트 본문").embedding(vector).build()).getId();
        em.flush();
        em.clear();

        TermClause found = termClauseRepository.findById(id).orElseThrow();
        assertThat(found.getEmbedding()).hasSize(TermClause.EMBEDDING_DIMENSION).containsExactly(vector);
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    void 임베딩_없이도_저장할_수_있다() {
        Term term = termRepository.save(newTerm());

        Long id = termClauseRepository.save(TermClause.builder()
                .term(term).articleNo(1).content("제1조(목적) 테스트").build()).getId();
        em.flush();
        em.clear();

        TermClause found = termClauseRepository.findById(id).orElseThrow();
        assertThat(found.getEmbedding()).isNull();
        assertThat(found.getArticleSubNo()).isZero();
    }

    private Term newTerm() {
        return Term.builder()
                .productName("테스트상품").version(1).effectiveDate(LocalDate.of(2025, 1, 1)).build();
    }
}
