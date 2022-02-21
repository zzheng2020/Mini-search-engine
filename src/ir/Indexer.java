/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.*;


/**
 *   Processes a directory structure and indexes all PDF and text files.
 */
public class Indexer {

    /** The index to be built up by this Indexer. */
    Index index;

    /** K-gram index to be built up by this Indexer */
    KGramIndex kgIndex;

    /** The next docID to be generated. */
    private int lastDocID = 0;

    /** The patterns matching non-standard words (e-mail addresses, etc.) */
    String patterns_file;

    public void initLastDocID() {
        this.lastDocID = 0;
    }


    /* ----------------------------------------------- */


    /** Constructor */
    public Indexer( Index index, KGramIndex kgIndex, String patterns_file ) {
        this.index = index;
        this.kgIndex = kgIndex;
        this.patterns_file = patterns_file;
    }


    /** Generates a new document identifier as an integer. */
    private int generateDocID() {
        return lastDocID++;
    }



    /**
     *  Tokenizes and indexes the file @code{f}. If <code>f</code> is a directory,
     *  all its files and subdirectories are recursively processed.
     */
    public void processFiles( File f, boolean is_indexing ) {
        // do not try to index fs that cannot be read
        if (is_indexing) {
            if ( f.canRead() ) {
                if ( f.isDirectory() ) {
                    String[] fs = f.list();
                    // an IO error could occur
                    if ( fs != null ) {
                        for ( int i=0; i<fs.length; i++ ) {
                            processFiles( new File( f, fs[i] ), is_indexing );
                        }
                    }
                } else {
                    // First register the document and get a docID
                    int docID = generateDocID();
                    if ( docID%1000 == 0 ) System.err.println( "Indexed " + docID + " files" );
                    try {
                        Reader reader = new InputStreamReader( new FileInputStream(f), StandardCharsets.UTF_8 );
                        Tokenizer tok = new Tokenizer( reader, true, false, true, patterns_file );
                        int offset = 0;

                        // 这篇文章中有没有出现过这个词
                        HashMap<String, Boolean> isAppear = new HashMap<>();

                        while ( tok.hasMoreTokens() ) {
                            String token = tok.nextToken();
                            insertIntoIndex( docID, token, offset++ );

                            if (isAppear.get(token) == null) {
                                if (index.wordInDocs.get(token) == null) {
                                    index.wordInDocs.put(token, 1);
                                }
                                else {
                                    int appearTimes = index.wordInDocs.get(token);
                                    index.wordInDocs.put(token, appearTimes + 1);
                                }
                            }
                            isAppear.put(token, true);

                        }
                        index.docNames.put( docID, f.getPath() );
                        index.docLengths.put( docID, offset );
                        reader.close();
                    } catch ( IOException e ) {
                        System.err.println( "Warning: IOException during indexing." );
                    }
                }
            }
        }
    }

    public void processFileAgain( File f ) {
        if ( f.isDirectory() ) {
            String[] fs = f.list();
            // an IO error could occur
            if ( fs != null ) {
                for ( int i = 0; i < fs.length; i++ ) {
                    processFileAgain( new File( f, fs[i] ) );
                }
            }
        }
        else {
            int docID = generateDocID();
            if (docID % 100 == 0) System.out.println("docId == " + docID);
            if( docID % 1000 == 0) System.out.println("Indexed " + docID + " files again");

            try {
                Reader reader = new InputStreamReader( new FileInputStream(f), StandardCharsets.UTF_8 );
                Tokenizer tok = new Tokenizer( reader, true, false, true, patterns_file );
                int offset = 0;

                HashMap<String, Integer> wordVector = new HashMap<>();


                while ( tok.hasMoreTokens() ) {
                    String token = tok.nextToken();
//                    insertIntoIndex( docID, token, offset++ );
                    if (wordVector.get(token) == null) {
                        wordVector.put(token, 1);
                    }
                    else {
                        int appearTimes = wordVector.get(token);
                        wordVector.put(token, appearTimes + 1);
                    }
                }

                double euclideanLengths = 0.0;
                double squareSum = 0.0;
                for (Map.Entry<String, Integer> entry : wordVector.entrySet()) {
//                    double appear = (double) entry.getValue();
//                    squareSum += appear * appear;
                    String               word = entry.getKey();
                    int                    tf = entry.getValue();
//                    PostingsList postingsList = index.getPostings(word);
                    int                     N = index.docNames.size();
//                    double                 df = postingsList.getList().size();
                    double df = N;
                    if (index.wordInDocs.get(word) == null) {
                        df = N;
                    }
                    else df = index.wordInDocs.get(word);

                    double               idf = Math.log( (double) N / df );

                    squareSum += Math.pow((double) tf * idf, 2);

                }

                euclideanLengths = Math.sqrt(squareSum);
                index.euclideanDocLengths.put(docID, euclideanLengths);

                reader.close();
            } catch ( IOException e ) {
                System.err.println( "Warning: IOException during indexing." );
            }
        }
    }


    /* ----------------------------------------------- */


    /**
     *  Indexes one token.
     */
    public void insertIntoIndex( int docID, String token, int offset ) {
        index.insert( token, docID, offset );
        if (kgIndex != null)
            kgIndex.insert(token);
    }
}

