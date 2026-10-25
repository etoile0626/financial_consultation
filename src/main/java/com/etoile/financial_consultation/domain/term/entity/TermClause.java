package com.etoile.financial_consultation.domain.term.entity;

import com.etoile.financial_consultation.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "term_clauses")
public class TermClause extends BaseEntity {

    public static final int EMBEDDING_DIMENSION = 1024;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    /** 제n조 */
    @Column(name = "article_no", nullable = false)
    private int articleNo;

    /** 제n조의m (없으면 0) */
    @Column(name = "article_sub_no", nullable = false)
    private int articleSubNo;

    /** 항 (조 본문이면 0) */
    @Column(name = "paragraph_no", nullable = false)
    private int paragraphNo;

    /** 조 제목 + 본문 */
    @Column(nullable = false, columnDefinition = "text")
    private String content;

    /** bge-m3 임베딩. 파싱 저장 후 채워지므로 null 허용 */
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = EMBEDDING_DIMENSION)
    @Column(name = "embedding")
    private float[] embedding;

    public void updateEmbedding(float[] embedding) {
        this.embedding = embedding;
    }
}
