package com.etoile.financial_consultation.domain.term.repository;

import com.etoile.financial_consultation.domain.term.entity.TermClause;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermClauseRepository extends JpaRepository<TermClause, Long> {
}
