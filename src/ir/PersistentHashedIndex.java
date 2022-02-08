/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, KTH, 2018
 */  

package ir;

import java.io.*;
import java.util.*;


/*
 *   Implements an inverted index as a hashtable on disk.
 *   
 *   Both the words (the dictionary) and the data (the postings list) are
 *   stored in RandomAccessFiles that permit fast (almost constant-time)
 *   disk seeks. 
 *
 *   When words are read and indexed, they are first put in an ordinary,
 *   main-memory HashMap. When all words are read, the index is committed
 *   to disk.
 */
public class PersistentHashedIndex implements Index {

    /** The directory where the persistent index files are stored. */
    public static final String INDEXDIR = "/Users/zhangziheng/OneDrive/KTH/SEandIRGradeA/src/index";

    /** The dictionary file name */
    public static final String DICTIONARY_FNAME = "dictionary";

    /** The data file name */
    public static final String DATA_FNAME = "data";

    /** The terms file name */
    public static final String TERMS_FNAME = "terms";

    /** The doc info file name */
    public static final String DOCINFO_FNAME = "docInfo";

    /** The dictionary hash table on disk can fit this many entries. */
    public static final long TABLESIZE = 611953L;
//    public static final long TABLESIZE = 3500000;

    public static final int ENTRYSIZE = 20;

    /** The dictionary hash table is stored in this file. */
    RandomAccessFile dictionaryFile;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile;

    /** Pointer to the first free memory cell in the data file. */
    long free = 0L;

    /** The cache as a main-memory hash map. */
    HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();


    // ===================================================================

    /**
     *   A helper class representing one entry in the dictionary hashtable.
     */ 
    public class Entry {
        //
        //  YOUR CODE HERE
        //
        public int size;
        public long ptr;
        public long hashSec;
        //public String token;

        public Entry(long ptr, int size, long hashSec) {
            this.size = size;
            this.ptr = ptr;
            this.hashSec = hashSec;
            // this.token = token;
        }
    }


    // ==================================================================

    
    /**
     *  Constructor. Opens the dictionary file and the data file.
     *  If these files don't exist, they will be created. 
     */
    public PersistentHashedIndex() {
        try {
            dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME, "rw" );
            dictionaryFile.seek(TABLESIZE*ENTRYSIZE+1000);
            dictionaryFile.writeInt(1);
            dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME, "rw" );
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        try {
            readDocInfo();
        } catch ( FileNotFoundException e ) {
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     *  Writes data to the data file at a specified place.
     *
     *  @return The number of bytes written.
     */ 
    int writeData( String dataString, long ptr ) {
        try {
            dataFile.seek( ptr ); 
            byte[] data = dataString.getBytes();
            dataFile.write( data );
            return data.length;
        } catch ( IOException e ) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     *  Reads data from the data file
     */ 
    String readData( long ptr, int size ) {
        try {
            dataFile.seek( ptr );
            byte[] data = new byte[size];
            dataFile.readFully( data );
            return new String(data);
        } catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }


    // ==================================================================
    //
    //  Reading and writing to the dictionary file.

    /*
     *  Writes an entry to the dictionary hash table file. 
     *
     *  @param entry The key of this entry is assumed to have a fixed length
     *  @param ptr   The place in the dictionary file to store the entry
     */
    void writeEntry( Entry entry, long ptr ) {
        //
        //  YOUR CODE HERE
        //
        try {
            dictionaryFile.seek(ptr);
            //dictionaryFile.writeBytes(entry.token);
            dictionaryFile.writeLong(entry.hashSec);
            dictionaryFile.writeLong(entry.ptr);
            dictionaryFile.writeInt(entry.size);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     *  Reads an entry from the dictionary file.
     *
     *  @param ptr The place in the dictionary file where to start reading.
     */
    Entry readEntry( long ptr ) {   
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE 
        //
        try {
            dictionaryFile.seek(ptr);

            long hashSec = dictionaryFile.readLong();
            dictionaryFile.seek(ptr+8);
            long pointer = dictionaryFile.readLong();
            dictionaryFile.seek(ptr+16);
            int size = dictionaryFile.readInt();
            return new Entry(pointer, size, hashSec);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    // ==================================================================

    /**
     *  Writes the document names and document lengths to file.
     *
     * @throws IOException  { exception_description }
     */
    private void writeDocInfo() throws IOException {
        FileOutputStream fout = new FileOutputStream( INDEXDIR + "/docInfo" );
        for ( Map.Entry<Integer,String> entry : docNames.entrySet() ) {
            Integer key = entry.getKey();
            String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
            fout.write( docInfoEntry.getBytes() );
        }
        fout.close();
    }


    /**
     *  Reads the document names and document lengths from file, and
     *  put them in the appropriate data structures.
     *
     * @throws     IOException  { exception_description }
     */
    private void readDocInfo() throws IOException {
        File file = new File( INDEXDIR + "/docInfo" );
        FileReader freader = new FileReader(file);
        try ( BufferedReader br = new BufferedReader(freader) ) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(";");
                docNames.put( new Integer(data[0]), data[1] );
                docLengths.put( new Integer(data[0]), new Integer(data[2]) );
            }
        }
        freader.close();
    }


    /**
     *  Write the index to files.
     */
    public void writeIndex() {
        int collisions = 0;
        int cnt = 0;
        try {
            // Write the 'docNames' and 'docLengths' hash maps to a file
            writeDocInfo();

            // Write the dictionary and the postings list

            // 
            //  YOUR CODE HERE
            //
            for (Map.Entry<String, PostingsList> entry : index.entrySet()) {

                String token      = entry.getKey();
                long hash         = toHash(token);
                long SecondHash   = toHashVersionTwo(token);
                long comparedHash = 0;
                int sq = 1;

                if (hash >= TABLESIZE || hash <0) hash = 0L;
                while (isCollision(hash, comparedHash)) {
                    if (hash < 0) hash = 0L;
                    hash = (hash + ENTRYSIZE * 2) % TABLESIZE;
                    collisions++;
                }

                cnt++;
                int postingsListSize = writeData(entry.getValue().toStr(), free);
                Entry newEntry = new Entry(free, postingsListSize, SecondHash);
                writeEntry(newEntry, hash * ENTRYSIZE);
                free = free + postingsListSize ;

                //test
                // if(entry.getKey().equals("they")){
                //     System.err.println(entry.getKey());
                //     System.err.println(entry.getValue().toStr());
                // }
                if (cnt % 1000 == 0) System.out.println(cnt + " index have been written.");
            }
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        System.err.println( collisions + " collisions." );
    }

    // ==================================================================

    public static long toHash(String term) {
        long hash = 7;
        for (int i = 0; i < term.length(); i++) {
            hash = (33 * hash + term.charAt(i)) % TABLESIZE;
        }
        return hash;

    }

    public static long toHashVersionTwo(String term){
        int hash, i;
        for (hash = term.length(), i=0; i < term.length(); i++)
            hash = (hash << 4) ^ (hash >> 28) ^ term.charAt(i);
        return (hash % TABLESIZE);

    }

    // if has hash collisions
    public boolean isCollision(long hash, long comparedValue){
        boolean result = true;
        try {
            if (hash >= TABLESIZE || hash<0) {
                result = true;
            }
            else{
                dictionaryFile.seek(hash * ENTRYSIZE);
                if(dictionaryFile.readLong() == comparedValue){
                    result = false;
                }
            }
        }
        catch (EOFException e) {
            return false;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        long hash  = toHash(token);
        long hash2 = toHashVersionTwo(token);

        if (hash >= TABLESIZE || hash < 0) {
            hash = 0L;
        }
//        while (isCollision(hash, hash2)) {
//            if (hash >= TABLESIZE || hash<0) {
//                hash = 0L;
//            }
//            hash = (hash + ENTRYSIZE * 2) % TABLESIZE;
//        }
        int cnt = 0;
        while (!getEntry(token, hash, hash2) && cnt <= 100) {
            if (hash < 0) {
                hash = 0L;
            }
            hash = (hash + ENTRYSIZE * 2) % TABLESIZE;
            cnt++;
        }
        if (cnt < 100) {
            cnt = 0;
            Entry entry = readEntry(hash * ENTRYSIZE);
            //System.out.println(entry.ptr+","+entry.size);
            String a = readData(entry.ptr, entry.size);
            //System.out.println(a);
            return toPostingsList(a);
        }
        else {
            cnt = 0;
            return null;
        }
    }

    public boolean getEntry(String token, long hash1, long hash2) {
        try {
            dictionaryFile.seek(hash1 * ENTRYSIZE);
            long hashSec = dictionaryFile.readLong();
            return hash2 == hashSec;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public PostingsList toPostingsList(String a){
        PostingsList result = new PostingsList();

        String[] postingsList = a.split("\n");

        for (String item : postingsList) {
            String[] docID = item.split(" ");
//            System.out.println(docID[0]); // docID;
            String[] offset = docID[1].split(",");
            for (String i : offset) {
//                System.out.println(i);
                result.insert(Integer.parseInt(docID[0]), 0.0, Integer.parseInt(i));
            }

        }
        return result;
    }


    /**
     *  Inserts this token in the main-memory hashtable.
     */
    public void insert( String token, int docID, int offset ) {
        //
        //  YOUR CODE HERE
        //
        if (!index.containsKey(token)) index.put(token, new PostingsList());

        PostingsList postingList = index.get(token);
        postingList.insert(docID, 0.0, offset);
    }


    /**
     *  Write index to file after indexing is done.
     */
    public void cleanup() {
        System.err.println( index.keySet().size() + " unique words" );
        System.err.print( "Writing index to disk..." );
        writeIndex();
        System.err.println( "done!" );
    }
}
