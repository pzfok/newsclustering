package com.dfsx.newsclustering.singlepass;

import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.summary.TextRankSentenceEx;
import com.hankcs.hanlp.tokenizer.NotionalTokenizer;

import java.util.ArrayList;
import java.util.List;

public class JavaUtils {
    public static List<String> segment(String line) {
        List<String> words = new ArrayList<>();

        if (line == null || line.trim().length() == 0) {
            return words;
        }

        List<Term> terms = NotionalTokenizer.segment(line);
        for (Term term:
             terms) {
            words.add(term.word);
        }

        return words;
    }

    public static List<Term> segmentAndNature(String line) {
        return NotionalTokenizer.segment(line);
    }

    public static List<String> extractSummaries(String document, int size) {
        if (document == null || document.trim().length() == 0) {
            return new ArrayList<>();
        }

        return TextRankSentenceEx.getTopSentenceList(document, size);
    }

    public static String getSummary(String document, int count) {
        if (document == null || document.trim().length() == 0 || count == 0) {
            return " ";
        }

        return TextRankSentenceEx.getTopSentenceList(document, count, 1).get(0);
    }
}
