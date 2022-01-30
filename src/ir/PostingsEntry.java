/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.*;
import java.io.Serializable;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {

    public int    docID;
    public double score  = 0;
    public int    offset = 0;
    public ArrayList<Integer> position = new ArrayList<Integer>();
//    Set<Integer> position = new HashSet<Integer>();

    public PostingsEntry(int docID, double score, int offset) {
        this.docID  = docID;
        this.score  = score;
        this.offset = offset;
//        position.add(offset);
    }

    public void addOffset(int offset) {
        this.position.add(offset);
    }

    /**
     *  PostingsEntries are compared by their score (only relevant
     *  in ranked retrieval).
     *
     *  The comparison is defined so that entries will be put in 
     *  descending order.
     */
    public int compareTo( PostingsEntry other ) {
       return Double.compare( other.score, score );
    }


    //
    // YOUR CODE HERE
    //
}

