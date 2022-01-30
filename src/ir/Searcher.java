/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.ArrayList;

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
            for (int i = 0; i < query.size(); i++) {
                String token = query.queryterm.get(i).term;
                PostingsList tokenQueryPostingsList = index.getPostings(token);
                phraseResult = positionalIntersect(tokenQueryPostingsList, phraseResult);
            }
            result = phraseResult;
        }


        return result;
    }

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

    public PostingsList positionalIntersect(PostingsList tokenPostingsList, PostingsList intermediatePostingsList) {
        if (tokenPostingsList == null || intermediatePostingsList == null) return null;

        PostingsList       mergeResult  = new PostingsList();
        ArrayList<Integer> tmpArray     = new ArrayList<Integer>();

        if (intermediatePostingsList.size() == 0) return tokenPostingsList;

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