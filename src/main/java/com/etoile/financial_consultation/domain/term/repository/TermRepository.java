package com.etoile.financial_consultation.domain.term.repository;

import com.etoile.financial_consultation.domain.term.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermRepository extends JpaRepository<Term, Long> {
}
