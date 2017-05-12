package com.hankcs.hanlp.summary;

import com.hankcs.hanlp.dictionary.stopword.CoreStopWordDictionary;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.tokenizer.StandardTokenizer;

import java.util.*;

public class TextRankSentenceEx {
    /**
     * 阻尼系数（ＤａｍｐｉｎｇＦａｃｔｏｒ），一般取值为0.85
     */
    final static double d = 0.85;
    /**
     * 最大迭代次数
     */
    final static int max_iter = 200;
    final static double min_diff = 0.001;
    /**
     * 文档句子的个数
     */
    int D;
    /**
     * 拆分为[句子[单词]]形式的文档
     */
    List<List<String>> docs;
    /**
     * 排序后的最终结果 score <-> index
     */
    TreeMap<Double, Integer> top;

    /**
     * 句子和其他句子的相关程度
     */
    double[][] weight;
    /**
     * 该句子和其他句子相关程度之和
     */
    double[] weight_sum;
    /**
     * 迭代之后收敛的权重
     */
    double[] vertex;

    /**
     * BM25相似度
     */
    BM25 bm25;

    public TextRankSentenceEx(List<List<String>> docs) {
        this(docs, docs.size());
    }

    public TextRankSentenceEx(List<List<String>> docs, int count) {
        this.docs = docs;
        bm25 = new BM25(docs);
        D = count;//docs.size();
        weight = new double[D][docs.size()]; // D -> docs.size()
        weight_sum = new double[D];
        vertex = new double[D];
        top = new TreeMap<Double, Integer>(Collections.reverseOrder());
        solve();
    }

    private void solve() {
        int cnt = 0;
        for (List<String> sentence : docs) {
            if (cnt >= D) {
                break;
            }

            double[] scores = bm25.simAll(sentence);
//            System.out.println(sentence);
//            System.out.println(Arrays.toString(scores));
            weight[cnt] = scores;
            weight_sum[cnt] = sum(scores) - scores[cnt]; // 减掉自己，自己跟自己肯定最相似
            vertex[cnt] = 1.0;
            ++cnt;
        }
        for (int _ = 0; _ < max_iter; ++_) {
            double[] m = new double[D];
            double max_diff = 0;
            for (int i = 0; i < D; ++i) {
                m[i] = 1 - d;
                for (int j = 0; j < D; ++j) {
                    if (j == i || weight_sum[j] == 0) continue;
                    m[i] += (d * weight[j][i] / weight_sum[j] * vertex[j]);
                }
                double diff = Math.abs(m[i] - vertex[i]);
                if (diff > max_diff) {
                    max_diff = diff;
                }
            }
            vertex = m;
            if (max_diff <= min_diff) break;
        }
        // 我们来排个序吧
        for (int i = 0; i < D; ++i) {
            top.put(vertex[i], i);
        }
    }

    /**
     * 简单的求和
     *
     * @param array
     * @return
     */
    private static double sum(double[] array)
    {
        double total = 0;
        for (double v : array)
        {
            total += v;
        }
        return total;
    }

    /**
     * 将文章分割为句子
     *
     * @param document
     * @return
     */
    static List<String> spiltSentence(String document)
    {
        List<String> sentences = new ArrayList<String>();
        for (String line : document.split("[\r\n]"))
        {
            line = line.trim();
            if (line.length() == 0) continue;
            // [，,。:：？?！!；;　\s]
            for (String sent : line.split("[，,。:：？?！!；;——~　\\s]")) // 倒数第二个字符为中文全角空格
            {
                sent = sent.trim();
                if (sent.length() == 0) continue;
                sentences.add(sent);
            }
        }

        return sentences;
    }

    /**
     * 将句子列表转化为文档
     *
     * @param sentenceList
     * @return
     */
    private static List<List<String>> convertSentenceListToDocument(List<String> sentenceList)
    {
        List<List<String>> docs = new ArrayList<List<String>>(sentenceList.size());
        for (String sentence : sentenceList)
        {
            List<Term> termList = StandardTokenizer.segment(sentence.toCharArray());
            List<String> wordList = new LinkedList<String>();
            for (Term term : termList)
            {
                if (CoreStopWordDictionary.shouldInclude(term))
                {
                    wordList.add(term.word);
                }
            }
            docs.add(wordList);
        }
        return docs;
    }

    /**
     * 获取前几个关键句子
     *
     * @param size 要几个
     * @return 关键句子的下标
     */
    public int[] getTopSentence(int size)
    {
        Collection<Integer> values = top.values();
        size = Math.min(size, values.size());
        int[] indexArray = new int[size];
        Iterator<Integer> it = values.iterator();
        for (int i = 0; i < size; ++i)
        {
            indexArray[i] = it.next();
        }
        return indexArray;
    }

    /**
     * 一句话调用接口
     *
     * @param document 目标文档
     * @param count    句子数
     * @param size     需要的关键句的个数
     * @return 关键句列表
     */
    public static List<String> getTopSentenceList(String document, int count, int size)
    {
        List<String> sentenceList = spiltSentence(document);
//        System.out.println(sentenceList);
        List<List<String>> docs = convertSentenceListToDocument(sentenceList);
        TextRankSentenceEx textRankEx = new TextRankSentenceEx(docs, count);
        int[] topSentence = textRankEx.getTopSentence(size);
        List<String> resultList = new LinkedList<String>();
        for (int i : topSentence)
        {
            resultList.add(sentenceList.get(i));
        }
//        System.out.println(resultList);
        return resultList;
    }

    /**
     * 一句话调用接口
     *
     * @param document 目标文档
     * @param size     需要的关键句的个数
     * @return 关键句列表
     */
    public static List<String> getTopSentenceList(String document, int size)
    {
        List<String> sentenceList = spiltSentence(document);
//        System.out.println(sentenceList);
        List<List<String>> docs = convertSentenceListToDocument(sentenceList);
        TextRankSentenceEx textRankEx = new TextRankSentenceEx(docs);
        int[] topSentence = textRankEx.getTopSentence(size);
        List<String> resultList = new LinkedList<String>();
        for (int i : topSentence)
        {
            resultList.add(sentenceList.get(i));
        }
        return resultList;
    }
}
