package com.etoile.financial_consultation.domain.term.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * 실제 약관 PDF 검증용. 샘플은 레포에 포함하지 않으므로 TERM_SAMPLE_PDF 환경변수로 로컬 경로를 줄 때만 실행된다.
 */
@EnabledIfEnvironmentVariable(named = "TERM_SAMPLE_PDF", matches = ".+")
class TermTextParserSampleTest {

    @Test
    void 실제_약관_샘플을_파싱한다() throws Exception {
        String text;
        try (var in = Files.newInputStream(Path.of(System.getenv("TERM_SAMPLE_PDF")))) {
            text = new PdfTextExtractor().extract(in);
        }

        var terms = new TermTextParser().parse(text);

        int total = 0;
        for (ParsedTerm term : terms) {
            Set<String> keys = new HashSet<>();
            for (ParsedClause clause : term.clauses()) {
                assertThat(keys.add(clause.articleNo() + "-" + clause.articleSubNo() + "-" + clause.paragraphNo()))
                        .as("%s 중복 번호", term.title()).isTrue();
                assertThat(clause.content()).isNotBlank();
            }
            // 다음 약관의 제목 줄은 앞 약관의 마지막 조항 끝에 섞이면 안 된다
            String lastContent = term.clauses().get(term.clauses().size() - 1).content();
            String lastLine = lastContent.lines().reduce((first, second) -> second).orElseThrow().strip();
            assertThat(lastLine).as("%s 마지막 조항 끝줄", term.title()).doesNotContain("약관 (").doesNotEndWith("특별약관")
                    .doesNotEndWith("추가약관").doesNotEndWith("추가특별약관");
            total += term.clauses().size();
            System.out.printf("SAMPLE | %-40s | clauses=%d%n", term.title().isEmpty() ? "(제목 없음)" : term.title(),
                    term.clauses().size());
        }
        System.out.printf("SAMPLE | terms=%d clauses=%d%n", terms.size(), total);
        assertThat(terms).isNotEmpty();
    }
}
