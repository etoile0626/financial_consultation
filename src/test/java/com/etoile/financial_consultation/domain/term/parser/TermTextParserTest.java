package com.etoile.financial_consultation.domain.term.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class TermTextParserTest {

    private final TermTextParser parser = new TermTextParser();

    private static final String BASIC_TERM = """
            테스트상품 보통약관

            제1조(목적)
            이 약관은 테스트 목적으로 작성되었습니다.

            제2조(보험료의 납입)
            ① 계약자는 보험료를 납입하여야 합니다.
            다만 예외가 있습니다.
            ② 회사는 영수증을 발행합니다.
            1. 첫째 호
            2. 둘째 호
            ③ 제1조(목적)에 따라 처리합니다.

            제2조의2(특례)
            ① 특례 내용입니다.
            """;

    @Test
    void 항이_없는_조는_조_본문_한_건으로_분리한다() {
        ParsedTerm term = parser.parse(BASIC_TERM).get(0);

        ParsedClause clause = term.clauses().get(0);
        assertThat(clause.articleNo()).isEqualTo(1);
        assertThat(clause.paragraphNo()).isZero();
        assertThat(clause.content()).isEqualTo("제1조(목적)\n이 약관은 테스트 목적으로 작성되었습니다.");
    }

    @Test
    void 항은_원문자_기준으로_나누고_호는_해당_항에_포함한다() {
        List<ParsedClause> clauses = parser.parse(BASIC_TERM).get(0).clauses();

        List<ParsedClause> article2 = clauses.stream().filter(c -> c.articleNo() == 2 && c.articleSubNo() == 0).toList();
        assertThat(article2).extracting(ParsedClause::paragraphNo).containsExactly(1, 2, 3);
        assertThat(article2.get(0).content()).isEqualTo(
                "제2조(보험료의 납입) ① 계약자는 보험료를 납입하여야 합니다.\n다만 예외가 있습니다.");
        assertThat(article2.get(1).content()).contains("② 회사는 영수증을 발행합니다.", "1. 첫째 호", "2. 둘째 호");
    }

    @Test
    void 제n조의m_조항을_구분한다() {
        List<ParsedClause> clauses = parser.parse(BASIC_TERM).get(0).clauses();

        ParsedClause branch = clauses.get(clauses.size() - 1);
        assertThat(branch.articleNo()).isEqualTo(2);
        assertThat(branch.articleSubNo()).isEqualTo(2);
        assertThat(branch.paragraphNo()).isEqualTo(1);
        assertThat(branch.content()).startsWith("제2조의2(특례) ① ");
    }

    @Test
    void 줄_시작의_참조_문구는_조_제목으로_보지_않는다() {
        String text = """
                제1조(목적)
                ① 내용입니다.
                제19조(보험료의 환급)에 따라 이를 지급합니다.
                제6조(계약내용의 변경 등) 제1항의 절차에 따릅니다.
                제2조(다음 조)
                본문
                """;

        List<ParsedClause> clauses = parser.parse(text).get(0).clauses();

        assertThat(clauses).extracting(ParsedClause::articleNo).containsExactly(1, 2);
        assertThat(clauses.get(0).content()).contains("제19조(보험료의 환급)에 따라", "제6조(계약내용의 변경 등) 제1항");
    }

    @Test
    void 대괄호_제목도_인식한다() {
        List<ParsedClause> clauses = parser.parse("제7조[납입연체시 납입최고(독촉)와 해지]\n본문").get(0).clauses();

        assertThat(clauses.get(0).content()).startsWith("제7조(납입연체시 납입최고(독촉)와 해지)");
    }

    @Test
    void 제1조가_다시_나오면_별도_약관으로_분리하고_제목을_붙인다() {
        String text = """
                테스트상품 보통약관
                제1조(목적)
                보통약관 본문
                제2조(범위)
                보통약관 범위
                테스트상품 특별약관
                전기위험 특별약관
                제1조(보상하는 손해)
                특별약관 본문
                """;

        List<ParsedTerm> terms = parser.parse(text);

        assertThat(terms).extracting(ParsedTerm::title).containsExactly("테스트상품 보통약관", "전기위험 특별약관");
        assertThat(terms.get(0).clauses()).hasSize(2);
        assertThat(terms.get(0).clauses().get(1).content()).isEqualTo("제2조(범위)\n보통약관 범위");
        assertThat(terms.get(1).clauses()).hasSize(1);
    }

    @Test
    void 괄호가_붙은_약관_제목도_인식한다() {
        String text = """
                제1조(목적)
                보통약관 본문
                이 특별약관에 정하지 아니한 사항은 보통약관을 따릅니다.
                실화(대물)배상책임 특별약관 (특수건물)
                제1조(보상하는 손해)
                특별약관 본문
                확장위험 특별약관 (I)
                제1조(범위)
                또 다른 본문
                """;

        List<ParsedTerm> terms = parser.parse(text);

        assertThat(terms).extracting(ParsedTerm::title)
                .containsExactly("", "실화(대물)배상책임 특별약관 (특수건물)", "확장위험 특별약관 (I)");
        assertThat(terms.get(0).clauses().get(0).content()).endsWith("따릅니다.");
    }

    @Test
    void 본문이_없는_목차_항목은_버리고_본문이_있는_조항을_채택한다() {
        String text = """
                제1조(목적)
                제2조(범위)
                제1조(목적)
                실제 본문
                제2조(범위)
                실제 범위
                """;

        List<ParsedClause> clauses = parser.parse(text).get(0).clauses();

        assertThat(clauses).hasSize(2);
        assertThat(clauses.get(0).content()).isEqualTo("제1조(목적)\n실제 본문");
    }

    @Test
    void 조가_없으면_빈_목록을_반환한다() {
        assertThat(parser.parse("아무 조항도 없는 텍스트")).isEmpty();
    }
}
