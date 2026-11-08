package com.etoile.financial_consultation.domain.term.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 약관 텍스트를 제n조 / 항 단위로 분리한다. 텍스트만 입력으로 받으므로 PDF 없이 테스트할 수 있다.
 *
 * <p>조 제목은 "제n조(제목)" 또는 "제n조[제목]"으로 줄이 끝나는 줄만 인정한다.
 * "제19조(보험료의 환급)에 따라 …"처럼 본문 중 참조 문구가 줄 시작에 오는 경우를 걸러내기 위함이다.
 */
public class TermTextParser {

    private static final Pattern ARTICLE_HEADING =
            Pattern.compile("^제\\s*(\\d+)\\s*조(?:의\\s*(\\d+))?\\s*[(\\[](.+)[)\\]]$");
    private static final Pattern TERM_TITLE = Pattern.compile("^[^.\\[\\]]{2,50}약관\\s*(\\([^()]{1,20}\\))?$");
    private static final int TITLE_LOOKBACK_LINES = 8;
    private static final char FIRST_CIRCLED_NUMBER = '①'; // ①
    private static final char LAST_CIRCLED_NUMBER = '⑳';  // ⑳

    public List<ParsedTerm> parse(String text) {
        List<String> lines = text.lines().map(String::strip).filter(line -> !line.isEmpty()).toList();
        List<Heading> headings = findHeadings(lines);
        if (headings.isEmpty()) {
            return List.of();
        }

        List<List<Heading>> groups = groupByTerm(headings);
        List<ParsedTerm> terms = new ArrayList<>();
        int previousGroupLastHeading = -1;
        for (int g = 0; g < groups.size(); g++) {
            List<Heading> group = groups.get(g);
            int firstHeadingLine = group.get(0).lineIndex();
            TitleSearch title = findTermTitle(lines, previousGroupLastHeading + 1, firstHeadingLine);

            int groupEnd = lines.size();
            if (g + 1 < groups.size()) {
                int nextFirst = groups.get(g + 1).get(0).lineIndex();
                groupEnd = findTermTitle(lines, group.get(group.size() - 1).lineIndex() + 1, nextFirst).cutIndex();
            }
            List<ParsedClause> clauses = buildClauses(lines, group, groupEnd);
            if (!clauses.isEmpty()) { // 본문 없는 목차만 있는 구간은 약관으로 보지 않는다
                terms.add(new ParsedTerm(title.title(), clauses));
            }
            previousGroupLastHeading = group.get(group.size() - 1).lineIndex();
        }
        return terms;
    }

    private List<Heading> findHeadings(List<String> lines) {
        List<Heading> headings = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            Matcher m = ARTICLE_HEADING.matcher(lines.get(i));
            if (m.matches()) {
                int subNo = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
                headings.add(new Heading(i, Integer.parseInt(m.group(1)), subNo, m.group(3).strip()));
            }
        }
        return headings;
    }

    /** 제1조가 다시 나오면 새 약관으로 본다. */
    private List<List<Heading>> groupByTerm(List<Heading> headings) {
        List<List<Heading>> groups = new ArrayList<>();
        List<Heading> current = new ArrayList<>();
        for (Heading heading : headings) {
            boolean restarts = heading.articleNo() == 1 && heading.articleSubNo() == 0 && !current.isEmpty();
            if (restarts) {
                groups.add(current);
                current = new ArrayList<>();
            }
            current.add(heading);
        }
        groups.add(current);
        return groups;
    }

    /**
     * [from, to) 구간의 끝쪽 몇 줄에서 "…약관"으로 끝나는 제목 줄을 찾는다.
     * 마지막 후보가 제목이고, 첫 후보부터는 앞 약관의 본문이 아니므로 잘라낸다.
     */
    private TitleSearch findTermTitle(List<String> lines, int from, int to) {
        int windowStart = Math.max(from, to - TITLE_LOOKBACK_LINES);
        int firstCandidate = -1;
        int lastCandidate = -1;
        for (int i = windowStart; i < to; i++) {
            if (TERM_TITLE.matcher(lines.get(i)).matches()) {
                if (firstCandidate < 0) {
                    firstCandidate = i;
                }
                lastCandidate = i;
            }
        }
        if (lastCandidate < 0) {
            return new TitleSearch("", to);
        }
        return new TitleSearch(lines.get(lastCandidate), firstCandidate);
    }

    private List<ParsedClause> buildClauses(List<String> lines, List<Heading> group, int groupEnd) {
        Map<String, ParsedClause> clauses = new LinkedHashMap<>();
        for (int h = 0; h < group.size(); h++) {
            Heading heading = group.get(h);
            int bodyEnd = h + 1 < group.size() ? group.get(h + 1).lineIndex() : groupEnd;
            List<String> body = lines.subList(heading.lineIndex() + 1, Math.max(heading.lineIndex() + 1, bodyEnd));
            for (ParsedClause clause : splitParagraphs(heading, body)) {
                String key = clause.articleNo() + "-" + clause.articleSubNo() + "-" + clause.paragraphNo();
                clauses.remove(key); // 중복 번호는 나중 것을 채택 (목차 등)
                clauses.put(key, clause);
            }
        }
        return new ArrayList<>(clauses.values());
    }

    /** 본문을 ①②… 기준으로 항 단위로 나눈다. 호(1. 2.)와 목(가. 나.)은 해당 항 안에 그대로 둔다. */
    private List<ParsedClause> splitParagraphs(Heading heading, List<String> body) {
        String prefix = heading.label();
        List<ParsedClause> result = new ArrayList<>();

        int paragraphNo = 0;
        StringBuilder current = new StringBuilder();
        for (String line : body) {
            if (isParagraphStart(line)) {
                addClause(result, heading, paragraphNo, prefix, current);
                paragraphNo = line.charAt(0) - FIRST_CIRCLED_NUMBER + 1;
                current = new StringBuilder();
            }
            if (!current.isEmpty()) {
                current.append('\n');
            }
            current.append(line);
        }
        addClause(result, heading, paragraphNo, prefix, current);
        return result;
    }

    private void addClause(List<ParsedClause> result, Heading heading, int paragraphNo,
                           String prefix, StringBuilder text) {
        if (text.isEmpty()) {
            return;
        }
        String separator = paragraphNo == 0 ? "\n" : " ";
        result.add(new ParsedClause(heading.articleNo(), heading.articleSubNo(), paragraphNo,
                prefix + separator + text));
    }

    private boolean isParagraphStart(String line) {
        char first = line.charAt(0);
        return first >= FIRST_CIRCLED_NUMBER && first <= LAST_CIRCLED_NUMBER;
    }

    private record Heading(int lineIndex, int articleNo, int articleSubNo, String title) {
        String label() {
            String sub = articleSubNo == 0 ? "" : "의" + articleSubNo;
            return "제" + articleNo + "조" + sub + "(" + title + ")";
        }
    }

    private record TitleSearch(String title, int cutIndex) {
    }
}
