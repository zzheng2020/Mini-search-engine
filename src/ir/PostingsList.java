/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.ArrayList;
import java.util.HashMap;

public class PostingsList {
    
    /** The postings list */
    private ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();
    public double weight = 1;

    // {docID, Index in the PostingsList}
    public HashMap<Integer, Integer> hasDocID = new HashMap<Integer, Integer>();
    public int countDoc = 0;


    /** Number of postings in this list. */
    public int size() {
    return list.size();
    }

    /** Returns the ith posting. */
    public PostingsEntry get( int i ) {
        return list.get( i );
    }

    // 
    //  YOUR CODE HERE
    //
    public ArrayList<PostingsEntry> getList() {
        return list;
    }

    public void insert(int docID, double score, int offset) {
        if (!hasDocID.containsKey(docID)) {
            hasDocID.put(docID, countDoc++);
            PostingsEntry postingsEntry = new PostingsEntry(docID, score, offset);
            postingsEntry.addOffset(offset);
            list.add(postingsEntry);
        }
        else {
            int i = hasDocID.get(docID);
            PostingsEntry existEntry = list.get(i);
            existEntry.addOffset(offset);
        }

//        PostingsEntry postingsEntry = new PostingsEntry(docID, score, offset);
////        postingsEntry.addOffset(offset);
//        list.add(postingsEntry);
    }

    public String toStr() {
        StringBuilder a = new StringBuilder("");
        for (PostingsEntry postingsEntry : list) {
            // +" "+Double.toString(list.get(i).score)
            a.append(Integer.toString(postingsEntry.docID));
            a.append(" ");
            for (int item : postingsEntry.position) {
                a.append(Integer.toString(item));
                a.append(",");
            }
//                a.append(Integer.toString(postingsEntry.offset));
            a.append("\n");
        }
        return a.toString();
    }

//     If a word, for example 'zombie', appears several times in a
//     document, then it will be counted for more than one time,
//     therefore, we need to remove these duplications.
//    public void removeDuplication() {
//        for (int i = 0; i < list.size() - 1; i++) {
//            int firstDocID  = list.get(i).docID;
//            int secondDocID = list.get(i + 1).docID;
//            if (firstDocID == secondDocID) {
//                list.remove(i);
//                i--;
//            }
//        }
//    }

}

