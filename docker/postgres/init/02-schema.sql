CREATE TABLE IF NOT EXISTS terms (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_name   VARCHAR(200) NOT NULL,
    version        INT          NOT NULL DEFAULT 1,
    effective_date DATE         NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uk_terms_product_name_version UNIQUE (product_name, version)
);

CREATE TABLE IF NOT EXISTS term_clauses (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    term_id        BIGINT    NOT NULL,
    article_no     INT       NOT NULL,           -- 제n조
    article_sub_no INT       NOT NULL DEFAULT 0, -- 제n조의m (없으면 0)
    paragraph_no   INT       NOT NULL DEFAULT 0, -- 항 (조 본문이면 0)
    content        TEXT      NOT NULL,           -- 조 제목 + 본문
    embedding      vector(1024),                 -- bge-m3, 파싱 저장 후 채워짐
    created_at     TIMESTAMP NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_term_clauses_terms FOREIGN KEY (term_id) REFERENCES terms (id),
    CONSTRAINT uk_term_clauses_term_id_article_paragraph
        UNIQUE (term_id, article_no, article_sub_no, paragraph_no)
);

CREATE INDEX IF NOT EXISTS idx_term_clauses_term_id ON term_clauses (term_id);
CREATE INDEX IF NOT EXISTS idx_term_clauses_embedding ON term_clauses
    USING hnsw (embedding vector_cosine_ops);
