/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.ArrayList;
import java.util.Collections;

/**
 *  Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;

    /** The k-gram index to be searched by this Searcher */
    KGramIndex kgIndex;
    
    /** Constructor */
    public Searcher( Index index, KGramIndex kgIndex ) {
        this.index = index;
        this.kgIndex = kgIndex;
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

            // 储存需要查询的词的PostingsList
            ArrayList<PostingsList> queryWordPostingsList = new ArrayList<>();
            // 计算查询的单词在每篇文章中的tf_idf值
            for (int i = 0; i < query.size(); i++) {
                String queryWord = query.queryterm.get(i).term;
                PostingsList post = index.getPostings(queryWord);
                calculateTF_IDF(post);
                queryWordPostingsList.add(post);
            }

            // 查询只有一个词的时候
            if (query.size() == 1) {
                // 如果只查一个词并且它不存在，那么返回空
                if (queryWordPostingsList.get(0) == null) return result;
                Collections.sort(queryWordPostingsList.get(0).getList());
                return queryWordPostingsList.get(0);
            }

            PostingsList mergeResult = new PostingsList();
            mergeResult = mergeRankedPostingsList(queryWordPostingsList.get(0), queryWordPostingsList.get(1));

            for (int i = 2; i < query.size(); i++) {
                mergeResult = mergeRankedPostingsList(mergeResult, queryWordPostingsList.get(i));
            }
            Collections.sort(mergeResult.getList());
            result = mergeResult;
            if (result.size() == 0) return null;
            return result;
        }


        return result;
    }

    public PostingsList mergeRankedPostingsList(PostingsList postingsList1, PostingsList postingsList2) {
        PostingsList answer = new PostingsList();

        if (postingsList1 == null) postingsList1 = new PostingsList();
        if (postingsList2 == null) postingsList2 = new PostingsList();

        int i = 0, j = 0;

        while (i < postingsList1.size() && j < postingsList2.size()) {
            PostingsEntry postingsEntry1 = postingsList1.get(i);
            PostingsEntry postingsEntry2 = postingsList2.get(j);

            if (postingsEntry1.docID == postingsEntry2.docID) {
                double score = 0;

                score = postingsEntry1.score + postingsEntry2.score;
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

    public void calculateTF_IDF(PostingsList postingsList) {
        if (postingsList == null) return;
        int N  = this.index.docNames.size();
        int df = postingsList.size();
//        System.out.println("N == " + N + ", df == " + df + " == " + this.index.docLengths.size());

        for (PostingsEntry entry : postingsList.getList()) {
            int    tf        = entry.position.size();
            double idf       = Math.log((double)N / (double)df);
            double docLength = index.docLengths.get(entry.docID);

            entry.score = (double)tf * idf / docLength;
        }
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

            int tokenDocID        = tokenPostingsEntry.docID;
            int intermediateDocID = intermediatePostingsEntry.docID;

            if (tokenDocID == intermediateDocID) {
                int    docID    = tokenPostingsEntry.docID;
                double score    = tokenPostingsEntry.score;
                int    offset   = tokenPostingsEntry.offset;
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