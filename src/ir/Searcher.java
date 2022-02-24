/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

//import javafx.geometry.Pos;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 *  Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;

    /** The k-gram index to be searched by this Searcher */
    KGramIndex kgIndex;

    private final HashMap<Integer, Double> pageRankScore = new HashMap<>();

    private final HashMap<String, Integer> fileNameToPRID = new HashMap<>();

    HITSRanker hitsRanker;
    
    /** Constructor */
    public Searcher( Index index, KGramIndex kgIndex, HITSRanker hitsRanker) {
        this.index = index;
        this.kgIndex = kgIndex;
        this.hitsRanker = hitsRanker;
        try {
            mapPageRankIdToFileName("/Users/zhangziheng/OneDrive/KTH/SEandIR_ZihengZhang/src/assignment2/pagerank/davisTitles.txt");
            readPagedRank("/Users/zhangziheng/OneDrive/KTH/SEandIR_ZihengZhang/src/ir/myRankedResult.txt");
            readEuclideanLengths("/Users/zhangziheng/OneDrive/KTH/SEandIR_ZihengZhang/src/ir/euclidean.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readEuclideanLengths(String fileName) throws IOException {
        BufferedReader in = new BufferedReader( new FileReader( fileName ));

        String line;
        while ((line = in.readLine()) != null) {
            String[] splitLine = line.split(":");
            int docID = Integer.parseInt(splitLine[0]);
            double euclideanLengths = Double.parseDouble(splitLine[1]);

            index.euclideanDocLengths.put(docID, euclideanLengths);
        }
    }

    public void mapPageRankIdToFileName(String fileName) throws IOException {
        BufferedReader in = new BufferedReader( new FileReader( fileName ));

        String line;
        while ((line = in.readLine()) != null) {
            String[] splitLine = line.split(";");
            int pageRankID = Integer.parseInt(splitLine[0]);
            String docName = splitLine[1];

            fileNameToPRID.put(docName, pageRankID);
        }
    }

    public void readPagedRank(String fileName) throws IOException {
        BufferedReader in = new BufferedReader( new FileReader( fileName ));

        String line;
        while ((line = in.readLine()) != null) {
            String[] splitLine = line.split(":");
            int docName = Integer.parseInt(splitLine[0]);
            double score = Double.parseDouble(splitLine[1]);

            pageRankScore.put(docName, score);
        }
    }

    /**
     *  Searches the index for postings matching the query.
     *  @return A postings list representing the result of the query.
     */
    public PostingsList search( Query query, QueryType queryType, RankingType rankingType, NormalizationType normType ) { 
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        PostingsList result = new PostingsList();
//        result = null;
//         query for one word
//        for (int i = 0; i < query.size(); i++) {
//            String token = query.queryterm.get(i).term;
//            result = index.getPostings(token);
//            result.removeDuplication();
//            System.out.println(result.size());
//        }

        // query for multi words
        int cnt = 0;
        if (queryType == QueryType.INTERSECTION_QUERY) {
            PostingsList intersectionResult = new PostingsList();
            for (int i = 0; i < query.size(); i++) {
                String token = query.queryterm.get(i).term;
                PostingsList tokenPostingsList = index.getPostings(token);
                intersectionResult = mergeTwoPostingsList(tokenPostingsList, intersectionResult);
//            result.removeDuplication();
//            System.out.println(result.size());
            }
            result = intersectionResult;
        }
        // query for phrase
        else if (queryType == QueryType.PHRASE_QUERY) {
            System.out.println("phrase query");
            PostingsList phraseResult = new PostingsList();
            PostingsList tmpResult = new PostingsList();
            System.out.println("query size: " + query.size());
            for (int i = 0; i < query.size(); i++) {
                cnt++;
                String token = query.queryterm.get(i).term;
                System.out.println("token in phrase query: " + token);
                PostingsList tokenQueryPostingsList = index.getPostings(token);
                phraseResult = positionalIntersect(tokenQueryPostingsList, phraseResult);
//                if (query.size() > 1 && phraseResult.size() == 0) phraseResult = tmpResult;

                // fixed the bug.
                // If we already find the phraseResult == 0, and still have words that need to be searched,
                // we will get the wrong answer.
                // This "if" block is to tackle this problem.
                // When we have more than or equal two words, and find the size of phraseResult is 0,
                // then the result will be definitely 0,
                // therefore, we return the result early.
                if (cnt >= 2 && phraseResult.size() == 0) return result;
            }
            result = phraseResult;
        }
        // query for ranked query
        else if (queryType == QueryType.RANKED_QUERY) {
            System.out.println("Ranked_Query");

            if (query.size() == 0) {
                return result;
            }

            result = RankedQuery(query, rankingType, normType);

            if (rankingType == RankingType.COMBINATION) { doNormalization(result); }


//            if (rankingType == RankingType.TF_IDF) {
//                result = RankedQuery(query, rankingType);
//            }
//
//            if (rankingType == RankingType.PAGERANK) {
//                result = RankedQuery(query, rankingType);
//            }
//
//            if (rankingType == RankingType.COMBINATION) {
//                result = RankedQuery(query, rankingType);
//            }

            if (result == null || result.size() == 0) return null;
            return result;
        }


        return result;
    }

    public void doNormalization(PostingsList result) {
        double sumTF_IDF = 0.0;
        double sumPG_RNK = 0.0;

        for (PostingsEntry entry : result.getList()) {
            sumTF_IDF += entry.score;

            int          docID = entry.docID;
            String    fileName = index.docNames.get(docID);
            String[] splitLine = fileName.split("davisWiki/");

                      fileName = splitLine[1];
            int     pageRankID = -1;

            if (fileNameToPRID.get(fileName) != null) pageRankID = fileNameToPRID.get(fileName);

            if (pageRankScore.get(pageRankID) != null) {
                sumPG_RNK += pageRankScore.get(pageRankID);
            }
        }

        double w1 = 0.5;
        double w2 = 0.5;
        for (PostingsEntry entry : result.getList()) {
            int          docID = entry.docID;
            String    fileName = index.docNames.get(docID);
            String[] splitLine = fileName.split("davisWiki/");

                      fileName = splitLine[1];
            int     pageRankID = -1;

            entry.score = w1 * entry.score / sumTF_IDF;

            if (fileNameToPRID.get(fileName) != null) pageRankID = fileNameToPRID.get(fileName);

            if (pageRankScore.get(pageRankID) != null) {
                entry.score += w2 * pageRankScore.get(pageRankID) / sumPG_RNK;
            }
        }
        System.out.println("sumTF_IDF == " + sumTF_IDF + ", sumPG_RNK == " + sumPG_RNK);

        Collections.sort(result.getList());
    }


    public PostingsList RankedQuery(Query query, RankingType rankingType, NormalizationType normalizationType) {
        System.out.println(rankingType);
        ArrayList<PostingsList> queryWordPostingsList = new ArrayList<>();
        // 计算查询的单词在每篇文章中的tf_idf值
        for (int i = 0; i < query.size(); i++) {
            String queryWord = query.queryterm.get(i).term;
            PostingsList post = index.getPostings(queryWord);
            if (rankingType == RankingType.TF_IDF) calculateTF_IDF(post, normalizationType);
            if (rankingType == RankingType.PAGERANK) calculatePageRankScore(post);
            if (rankingType == RankingType.COMBINATION) calculateCombinedScore(post, normalizationType);
            queryWordPostingsList.add(post);
        }

        // 查询只有一个词的时候
        if (query.size() == 1) {
            // 如果只查一个词并且它不存在，那么返回空
            if (queryWordPostingsList.get(0) == null) return null;
            Collections.sort(queryWordPostingsList.get(0).getList());
            if (rankingType == RankingType.HITS) {
                PostingsList hitResult = new PostingsList();
                hitResult = hitsRanker.rank(queryWordPostingsList.get(0));
                return hitResult;
            }
            return queryWordPostingsList.get(0);
        }

        PostingsList mergeResult = new PostingsList();
        mergeResult = mergeRankedPostingsList(queryWordPostingsList.get(0), queryWordPostingsList.get(1), rankingType);

        for (int i = 2; i < query.size(); i++) {
            mergeResult = mergeRankedPostingsList(mergeResult, queryWordPostingsList.get(i), rankingType);
        }
        Collections.sort(mergeResult.getList());

        if (rankingType == RankingType.HITS) {
            mergeResult = hitsRanker.rank(mergeResult);
            System.out.println("searcher index == " + index.fileNameToDocId.size());
        }

        return mergeResult;
    }

    public void calculateTF_IDF(PostingsList postingsList, NormalizationType normalizationType) {
        if (postingsList == null) return;
        int N  = this.index.docNames.size();
        int df = postingsList.size();
//        System.out.println("N == " + N + ", df == " + df + " == " + this.index.docLengths.size());

        for (PostingsEntry entry : postingsList.getList()) {
            int           tf = entry.position.size();
            double       idf = Math.log((double)N / (double)df);
            double docLength = index.docLengths.get(entry.docID);

            if (normalizationType == NormalizationType.EUCLIDEAN) docLength = index.euclideanDocLengths.get(entry.docID);

            entry.score = (double)tf * idf / docLength;
        }
    }

    public void calculatePageRankScore(PostingsList postingsList) {
        if (postingsList == null) return;

        for (PostingsEntry entry : postingsList.getList()) {
            int          docID = entry.docID;
            String    fileName = index.docNames.get(docID);
            String[] splitLine = fileName.split("davisWiki/");

                      fileName = splitLine[1];
            int     pageRankID = -1;

            if (fileNameToPRID.get(fileName) != null) pageRankID = fileNameToPRID.get(fileName);


            if (pageRankScore.get(pageRankID) != null) {
                entry.score = pageRankScore.get(pageRankID);
            }
            else entry.score = 0.0;
        }
    }

    public void calculateCombinedScore(PostingsList postingsList, NormalizationType normalizationType) {
        calculateTF_IDF(postingsList, normalizationType);
//        if (postingsList == null) return;
//
//        double w1 = 1;
//        double w2 = 100;
//
//
//        for (PostingsEntry entry : postingsList.getList()) {
//            int docID          = entry.docID;
//            String fileName    = index.docNames.get(docID);
//            String[] splitLine = fileName.split("davisWiki/");
//
//            fileName           = splitLine[1];
//            int pageRankID     = -1;
//
//            if (fileNameToPRID.get(fileName) != null) pageRankID = fileNameToPRID.get(fileName);
//
//            if (pageRankScore.get(pageRankID) != null) {
//                entry.score = w1 * entry.score / sumTF_IDF + w2 * pageRankScore.get(pageRankID) / sumPG_RNK;
//            }
//            else {
//                entry.score = w1 * entry.score / index.docLengths.get(docID);
//            }
//
//        }
    }



    public PostingsList mergeRankedPostingsList(PostingsList postingsList1, PostingsList postingsList2, RankingType rankingType) {
        PostingsList answer = new PostingsList();

        if (postingsList1 == null) postingsList1 = new PostingsList();
        if (postingsList2 == null) postingsList2 = new PostingsList();

        int i = 0, j = 0;

        while (i < postingsList1.size() && j < postingsList2.size()) {
            PostingsEntry postingsEntry1 = postingsList1.get(i);
            PostingsEntry postingsEntry2 = postingsList2.get(j);

            if (postingsEntry1.docID == postingsEntry2.docID) {
                double score = 0;

                if (rankingType != RankingType.PAGERANK) score = postingsEntry1.score + postingsEntry2.score;
                else score = postingsEntry1.score;
                answer.insert(postingsEntry1.docID, score, postingsEntry1.offset);

                i++;
                j++;
            }
            else if (postingsEntry1.docID < postingsEntry2.docID) {
                answer.insert(postingsEntry1.docID, postingsEntry1.score, postingsEntry1.offset);

                i++;
            }
            else {
                answer.insert(postingsEntry2.docID, postingsEntry2.score, postingsEntry2.offset);

                j++;
            }
        }

        while (i < postingsList1.size()) {
            answer.insert(postingsList1.get(i).docID, postingsList1.get(i).score, postingsList1.get(i).offset);

            i++;
        }
        while (j < postingsList2.size()) {
            answer.insert(postingsList2.get(j).docID, postingsList2.get(j).score, postingsList2.get(j).offset);

            j++;
        }

        return answer;
    }


    // intersection query
    public PostingsList mergeTwoPostingsList(PostingsList tokenPostingsList, PostingsList intermediatePostingsList) {
        if (tokenPostingsList == null || intermediatePostingsList == null) return null;

        PostingsList mergeResult = new PostingsList();
        if (intermediatePostingsList.size() == 0) return tokenPostingsList;
        int pointerToken = 0, pointerIntermediate = 0;
        while (pointerToken < tokenPostingsList.size() && pointerIntermediate < intermediatePostingsList.size()) {
            PostingsEntry tokenPostingsEntry        = tokenPostingsList.get(pointerToken);
            PostingsEntry intermediatePostingsEntry = intermediatePostingsList.get(pointerIntermediate);

            if (tokenPostingsEntry == null || intermediatePostingsEntry == null) return null;

            int        tokenDocID = tokenPostingsEntry.docID;
            int intermediateDocID = intermediatePostingsEntry.docID;

            if (tokenDocID == intermediateDocID) {
                int     docID = tokenPostingsEntry.docID;
                double  score = tokenPostingsEntry.score;
                int    offset = tokenPostingsEntry.offset;
                mergeResult.insert(docID, score, offset);
                pointerToken++;
                pointerIntermediate++;
            }
            else if (tokenDocID < intermediateDocID) pointerToken++;
            else pointerIntermediate++;
        }
//        mergeResult.removeDuplication();
        return mergeResult;
    }

    // phrase query
    public PostingsList positionalIntersect(PostingsList tokenPostingsList, PostingsList intermediatePostingsList) {
        if (tokenPostingsList == null || intermediatePostingsList == null) return null;
        System.out.println("size " + tokenPostingsList.size() + " " + intermediatePostingsList.size());


        PostingsList       mergeResult  = new PostingsList();
        ArrayList<Integer> tmpArray     = new ArrayList<Integer>();

        if (intermediatePostingsList.size() == 0) return tokenPostingsList;
//        if (intermediatePostingsList.size() == 0) return null;

        int pointerToken = 0, pointerIntermediate = 0;

        while (pointerToken < tokenPostingsList.size() && pointerIntermediate < intermediatePostingsList.size()) {
            PostingsEntry      tokenEntry        = tokenPostingsList.get(pointerToken);
            PostingsEntry      intermediateEntry = intermediatePostingsList.get(pointerIntermediate);
            ArrayList<Integer> tokenArray        = tokenEntry.position;
            ArrayList<Integer> intermediateArray = intermediateEntry.position;
            int                tokenDocID        = tokenEntry.docID;
            int                intermediateDocID = intermediateEntry.docID;
            double             tokenScore        = tokenEntry.score;

//            System.out.println("token array");
//            for (Integer ele : tokenArray) System.out.println(ele);

            if (tokenDocID == intermediateDocID) {
                int pointerTokenArray = 0, pointerIntermediateArray = 0;
                while (pointerTokenArray < tokenArray.size() && pointerIntermediateArray < intermediateArray.size()) {
                    int indexTokenArray        = tokenArray.get(pointerTokenArray);
                    int indexIntermediateArray = intermediateArray.get(pointerIntermediateArray);
                    if (indexTokenArray - indexIntermediateArray == 1) {
                        tmpArray.add(indexTokenArray);
                        pointerTokenArray++;
                        pointerIntermediateArray++;
                    }
                    else if (indexIntermediateArray < indexTokenArray) pointerIntermediateArray++;
                    else pointerTokenArray++;
                }
                for (Integer element : tmpArray) {
                    mergeResult.insert(tokenDocID, tokenScore, element);
                }
                tmpArray.clear();
//                System.out.println("=====");
//                for (int i = 0; i < mergeResult.size(); i++) {
//                    PostingsEntry test = mergeResult.get(i);
//                    for (Integer ele : test.position) {
//                        System.out.println(ele);
//                    }
//                }
//                System.out.println("=====");


                pointerToken++;
                pointerIntermediate++;
            }
            else if (tokenDocID < intermediateDocID) pointerToken++;
            else pointerIntermediate++;
        }
//        mergeResult.removeDuplication();
        return mergeResult;
    }
}