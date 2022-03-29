//package ir;
//
//import java.io.*;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Map;
//
//public class PersistentScalableHashedIndex extends PersistentHashedIndex{
//    public final int BLOCK_SIZE = 50000;
////public final int BLOCK_SIZE = 500;
//    public final ArrayList<String> indexNumbers = new ArrayList<String>();
//    public int indexNumber = -1;
//    public Thread mergeThread;
//    public boolean indexingFinished = false;
////    public ArrayList<Thread> threads = new ArrayList<Thread>();
//
//    public PersistentScalableHashedIndex() {
//        super();
//
//        mergeThread = new Thread() {
//
//            ArrayList<Thread> threads = new ArrayList<Thread>();
//
//            public void run() {
//                while (!indexingFinished) {
//                    Thread thread = null;
//                    synchronized (indexNumbers) {
//                        if (indexNumbers.size() >= 2) {
//                            thread = new Thread(() -> {
//                                String file1 = "";
//                                String file2 = "";
//                                synchronized (indexNumbers) {
//                                    if (indexNumbers.size() >= 2) {
//                                        file1 = indexNumbers.get(0);
//                                        file2 = indexNumbers.get(1);
//                                        indexNumbers.remove(0);
//                                        indexNumbers.remove(0);
//                                    }
//                                    else {
//                                        System.out.println("Error");
//                                        return;
//                                    }
//                                }
//                                System.out.println("Merge " + file1 + " and " + file2);
//                                mergeIndex(file1, file2);
//                                System.out.println("Merge finished");
//                            });
//                        }
//                    }
//                    if (thread != null) {
//                        threads.add(thread);
//                        thread.start();
//                    }
//                    try {
//                        Thread.sleep(500);
//                    } catch(InterruptedException e) {
//                    }
//                }
//                boolean threadRunning = true;
//                while(threadRunning) {
//                    threadRunning = false;
//                    while(indexNumbers.size() >= 2) {
//                        Thread thread = new Thread(() -> {
//                            String s1 = "";
//                            String s2 = "";
//                            synchronized(indexNumbers) {
//                                s1 = indexNumbers.get(0);
//                                s2 = indexNumbers.get(1);
//                                indexNumbers.remove(0);
//                                indexNumbers.remove(0);
//                            }
//                            System.out.println("Merge block " + s1 + " and " + s2 + "...");
//                            mergeIndex(s1, s2);
//                            System.out.println("Merge block " + s1 + " and " + s2 + " finished");
//                        });
//                        threads.add(thread);
//                        thread.start();
//                        try {
//                            Thread.sleep(500);
//                        } catch(InterruptedException e) {
//
//                        }
//                    }
//                    while(threads.size() > 0) {
//                        threadRunning = true;
//                        try {
//                            threads.get(0).join();
//                        } catch(InterruptedException e) {
//
//                        }
//                        threads.remove(0);
//                    }
//                    System.out.println(indexNumbers.size());
//                }
//                finish();
//            }
//        };
//        mergeThread.start();
//    }
//
//    public void mergeIndex(String file1, String file2) {
//        try {
//            System.out.println("mergeIndexFunction");
//            RandomAccessFile dictionary1 = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME + file1, "rw" );
//            RandomAccessFile dictionary2 = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME + file2, "rw" );
//
//            RandomAccessFile data1 = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME + file1, "rw" );
//            RandomAccessFile data2 = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME + file2, "rw" );
//
//            RandomAccessFile dictionaryTmp = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME + file1 + "_" + file2, "rw" );
//            RandomAccessFile dataTmp = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME + file1 + "_" + file2, "rw" );
//
//
//            HashMap<String,Entry> dict1 = readDictionary(dictionary1, data1);
//            HashMap<String,Entry> dict2 = readDictionary(dictionary2, data2);
//            long ptr = 0;
//            System.out.println("begin to merge!");
//            System.out.println("size: " + dict1.size() + " " + dict2.size());
//            for (Map.Entry<String, Entry> entry : dict1.entrySet()) {
//                System.out.println("merge????????");
//                Entry entry1 = entry.getValue();
//                String token = entry.getKey();
//                System.out.println("In mergeIndex function, the token == " + token);
//                PostingsList postingsList1 = toPostingsList(readData(data1, entry1.ptr, entry1.size));
//                if (dict2.containsKey(token)) {
//                    Entry entry2 = dict2.get(token);
//                    PostingsList postingsList2 = toPostingsList(readData(data2, entry2.ptr, entry2.size));
//
//                    if (postingsList1.size() >= postingsList2.size()) {
//                        postingsList1.merge(postingsList2);
//                        ptr += this.writeEntry(dictionaryTmp, dataTmp, token, postingsList1, ptr);
//                    }
//                    else {
//                        postingsList2.merge(postingsList1);
//                        ptr += this.writeEntry(dictionaryTmp, dataTmp, token, postingsList2, ptr);
//                    }
//                    dict2.remove(token);
//                }
//                else {
//                    ptr += this.writeEntry(dictionaryTmp, dataTmp, token, postingsList1, ptr);
//                }
//                ptr += 1;
//            }
//            for (Map.Entry<String, Entry> entry : dict2.entrySet()) {
//                PostingsList postingsList1 = toPostingsList(readData(data2, entry.getValue().ptr, entry.getValue().size));
//                ptr += this.writeEntry(dictionaryTmp, dataTmp, entry.getKey(), postingsList1, ptr);
//                ptr += 1;
//            }
//            System.out.println("merge Finished!");
//            synchronized (indexNumbers) {
//                indexNumbers.add(file1 + "_" + file2);
//            }
//            this.removeFile(INDEXDIR + "/" + DICTIONARY_FNAME + file1);
//            this.removeFile(INDEXDIR + "/" + DICTIONARY_FNAME + file2);
//            this.removeFile(INDEXDIR + "/" + DATA_FNAME + file1);
//            this.removeFile(INDEXDIR + "/" + DATA_FNAME + file2);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//
//    }
//
//    public void insert( String token, int docID, int offset ) {
//        if(indexNumber == -1) {
//            indexNumber++;
//            prepareNewIndex();
//        }
//        if(token != null && docID >= 0 && offset >= 0) {
//            if (!index.containsKey(token)) index.put(token, new PostingsList());
//
//            PostingsList postingList = index.get(token);
//            postingList.insert(docID, 0.0, offset);
//        }
//        if(this.index.size() >= BLOCK_SIZE) {
//            //System.out.println("BLOCKSIZE reached.");
//            writeIndex();
//        }
//    }
//
//    public void prepareNewIndex() {
//        // System.out.println("Prepare new index " + Integer.toString(this.indexNumber));
//        try {
//            dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME + Integer.toString(this.indexNumber), "rw" );
//            dictionaryFile.seek(TABLESIZE*ENTRYSIZE+1000);
//            dictionaryFile.writeInt(1);
//            dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME + Integer.toString(this.indexNumber), "rw" );
//        }
//        catch ( IOException e ) {
//            e.printStackTrace();
//        }
//        this.free = 0;
//        this.index.clear();
//    }
//
//    private HashMap<String,Entry> readDictionary(RandomAccessFile file, RandomAccessFile data) {
//        long ptr = 0;
//        HashMap<String, Entry> dic = new HashMap<>();
//
//
//        try {
//            while (true) {
//                file.seek(ptr);
//                long hashSec = file.readLong();
//
//                if (hashSec == 0) continue;
//
//                file.seek(ptr+8);
//                long pointer = file.readLong();
//                file.seek(ptr+16);
//                int size = file.readInt();
//
//                Entry entry = new Entry(pointer, size, hashSec);
//
//                String dataString = readData(data, pointer, size);
//                String token = dataString.split("\n")[0].split(" ")[1].split("#")[1];
//
//                dic.put(token, entry);
//
//                ptr += ENTRYSIZE;
//            }
//        }
//        catch (EOFException e) {
//            System.out.println("EOF");
//            return dic;
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//        }
//        return dic;
//    }
//
//    public void writeIndex() {
//        //System.out.println("Write index " + Integer.toString(this.indexNumber));
//        super.writeIndex();
//        synchronized(indexNumbers) {
//            indexNumbers.add(Integer.toString(this.indexNumber));
//        }
//        this.indexNumber++;
//        prepareNewIndex();
//    }
//
//    public void cleanup() {
//        if(index.size() > 0) {
//            super.writeIndex();
//            synchronized(indexNumbers) {
//                indexNumbers.add(Integer.toString(this.indexNumber));
//            }
//        }
//        indexingFinished = true;
//    }
//
//    public void finish() {
//        if(indexNumbers.size() > 0) {
//            removeFile( INDEXDIR + "/" + DICTIONARY_FNAME);
//            removeFile( INDEXDIR + "/" + DATA_FNAME);
//            renameFile( INDEXDIR + "/" + DICTIONARY_FNAME + indexNumbers.get(0), INDEXDIR + "/" + DICTIONARY_FNAME);
//            renameFile( INDEXDIR + "/" + DATA_FNAME + indexNumbers.get(0), INDEXDIR + "/" + DATA_FNAME);
//            try {
//                writeDocInfo();
//                dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME, "rw" );
//                dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME, "rw" );
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        System.out.println( "done!" );
//    }
//
//    /**
//     *  Writes data to the data file at a specified place.
//     *
//     *  @return The number of bytes written.
//     */
//    public int writeData(RandomAccessFile file, String dataString, long ptr) {
//        try {
//            file.seek( ptr );
//            byte[] data = dataString.getBytes();
//            file.write( data );
//            return data.length;
//        } catch ( IOException e ) {
//            e.printStackTrace();
//            return -1;
//        }
//    }
//
//
//    /**
//     *  Reads data from the data file
//     */
//    public String readData(RandomAccessFile file, long ptr, int size) {
//        try {
//            file.seek( ptr );
//            byte[] data = new byte[size];
//            file.readFully( data );
//            return new String(data);
//        } catch ( IOException e ) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//    public void writeEntry(RandomAccessFile file, Entry entry, long ptr) {
//        try {
//            file.seek(ptr);
//            file.writeLong(entry.hashSec);
//            file.writeLong(entry.ptr);
//            file.writeInt(entry.size);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }
//
//    public int writeEntry(RandomAccessFile dictionary, RandomAccessFile data, String token, PostingsList list, long ptr) {
//        long hashSec = toHashVersionTwo(token);
//        int size = this.writeData(data, list.toStr(token), ptr);
//        Entry entry = new Entry(ptr, size, hashSec);
//
//        long hash = toHash(token);
//        Entry tmp = readEntry(dictionary, hash);
//
//        while (tmp != null && tmp.ptr != 0) {
//            hash = (hash + ENTRYSIZE * 2) % TABLESIZE;
//            tmp = readEntry(dictionary, hash);
//        }
//
//        this.writeEntry(dictionary, entry, hash);
//
//        return size;
//    }
//
//    public Entry readEntry(RandomAccessFile file, long ptr) {
//        try {
//            file.seek(ptr);
//            long hashSec = file.readLong();
//            file.seek(ptr+8);
//            long pointer = file.readLong();
//            file.seek(ptr+16);
//            int size = file.readInt();
//
//            if (hashSec != 0) {
//                return new Entry(pointer, size, hashSec);
//            }
//            else return null;
//
//        }
//        catch(EOFException e) {
//            return null;
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//            return null;
//        }
//
//    }
//
//    public void removeFile(String path) {
//        File file = new File(path);
//        file.delete();
//    }
//
//    public void renameFile(String path, String newPath) {
//        File file    = new File(path);
//        File newFile = new File(newPath);
//        file.renameTo(newFile);
//    }
//}
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
