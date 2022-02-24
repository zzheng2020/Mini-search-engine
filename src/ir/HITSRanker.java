/**
 *   Computes the Hubs and Authorities for an every document in a query-specific
 *   link graph, induced by the base set of pages.
 *
 *   @author Dmytro Kalpakchi
 */

package ir;

import java.util.*;
import java.io.*;


public class HITSRanker {

    /**
     *   Max number of iterations for HITS
     */
    final static int MAX_NUMBER_OF_STEPS = 1000;

    /**
     *   Convergence criterion: hub and authority scores do not 
     *   change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.001;

    /**
     *   The inverted index
     */
    Index index;

    /**
     *   Mapping from the titles to internal document ids used in the links file
     */
    HashMap<String, Integer> titleToId = new HashMap<String,Integer>();
    HashMap<Integer, String> idToTitle = new HashMap<>();

    /**
     *   Sparse vector containing hub scores
     */
    HashMap<Integer,Double> hubs;

    /**
     *   Sparse vector containing authority scores
     */
    HashMap<Integer,Double> authorities;

    PageRank pageRank;

    HashMap<Integer, HashMap<Integer, Boolean>> reversedGraph = new HashMap<>();

    public class HITS {
        HashSet<Integer> rootIdSet = new HashSet<>();
        HashSet<Integer> baseIDSet = new HashSet<>();

        private HITS(String[] titles) {
            for (String title : titles) {
//                System.out.println("now title == " + title);
                if (titleToId.get(title) != null) {
                    int          rootId = titleToId.get(title);
                    String rootIdString = String.valueOf(rootId);
                    rootIdSet.add(pageRank.docNumber.get(rootIdString));
                    baseIDSet.add(pageRank.docNumber.get(rootIdString));

//                    System.out.print("rootId == " + rootId + ": ");

                    int zippedRootId = pageRank.docNumber.get(rootIdString);

//                    pageRank.link.get(rootId) != null
                    // root 中的点连接到了哪些点
                    if (pageRank.link.get(zippedRootId) != null) {
                        HashMap<Integer, Boolean> vLinks = pageRank.link.get(zippedRootId);
                        for (Map.Entry<Integer, Boolean> v : vLinks.entrySet()) {
                            baseIDSet.add(v.getKey());
//                            System.out.print(pageRank.docName[v.getKey()] + ",");
                        }
                    }
//                    System.out.println("\n");
                    // 哪些点连接到了 root
                    if (reversedGraph.get(zippedRootId) != null) {
                        HashMap<Integer, Boolean> uLinks = reversedGraph.get(zippedRootId);
                        for (Map.Entry<Integer, Boolean> u : uLinks.entrySet()) {
                            baseIDSet.add(u.getKey());
//                            System.out.print(pageRank.docName[u.getKey()] + ",");
                        }
                    }
//                    System.out.println("\n---------");
                }
            }
            System.out.println("rootSet size == " + rootIdSet.size());
            System.out.println("baseSet size == " + baseIDSet.size());
            System.out.println("reverse size == " + reversedGraph.size());
            System.out.println("titleToId size == " + titleToId.size());
            System.out.println("idToTitle size == " + idToTitle.size());
        }
    }

    
    /* --------------------------------------------- */

    /**
     * Constructs the HITSRanker object
     * 
     * A set of linked documents can be presented as a graph.
     * Each page is a node in graph with a distinct nodeID associated with it.
     * There is an edge between two nodes if there is a link between two pages.
     * 
     * Each line in the links file has the following format:
     *  nodeID;outNodeID1,outNodeID2,...,outNodeIDK
     * This means that there are edges between nodeID and outNodeIDi, where i is between 1 and K.
     * 
     * Each line in the titles file has the following format:
     *  nodeID;pageTitle
     *  
     * NOTE: nodeIDs are consistent between these two files, but they are NOT the same
     *       as docIDs used by search engine's Indexer
     *
     * @param      linksFilename   File containing the links of the graph
     * @param      titlesFilename  File containing the mapping between nodeIDs and pages titles
     * @param      index           The inverted index
     */
    public HITSRanker( String linksFilename, String titlesFilename, Index index ) {
           this.index = index;
        this.pageRank = new PageRank();
        readDocs( linksFilename, titlesFilename );
//        rank();
    }


    /* --------------------------------------------- */

    /**
     * A utility function that gets a file name given its path.
     * For example, given the path "davisWiki/hello.f",
     * the function will return "hello.f".
     *
     * @param      path  The file path
     *
     * @return     The file name.
     */
    public static String getFileName(String path) {
        String result = "";
        StringTokenizer tok = new StringTokenizer( path, "\\/" );
        while ( tok.hasMoreTokens() ) {
            result = tok.nextToken();
        }
        return result;
    }


    /**
     * Reads the files describing the graph of the given set of pages.
     *
     * @param      linksFilename   File containing the links of the graph
     * @param      titlesFilename  File containing the mapping between nodeIDs and pages titles
     */
    void readDocs( String linksFilename, String titlesFilename ) {
        //
        // YOUR CODE HERE
        //
        int numberOfDocs = pageRank.readDocs(linksFilename);
        buildReversedGraph(pageRank.link);
        System.out.println("link == " + pageRank.link.size());
        System.out.println("reverse link == " + reversedGraph.size());
        try {
            BufferedReader in = new BufferedReader( new FileReader( titlesFilename ));
            String line;
            while ((line = in.readLine()) != null) {
                String[] splitLine = line.split(";");
                titleToId.put(splitLine[1], Integer.parseInt(splitLine[0]));
                idToTitle.put(Integer.parseInt(splitLine[0]), splitLine[1]);
            }
        } catch (NumberFormatException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Original Graph: u -> v
     * This function is used to create reversed link, pointing from v to u
     */
    public void buildReversedGraph(HashMap<Integer, HashMap<Integer, Boolean>> link) {
        for (Map.Entry<Integer, HashMap<Integer, Boolean>> entry : link.entrySet()) {
            int u = entry.getKey();
            HashMap<Integer, Boolean> vNodes = entry.getValue();
            for (Map.Entry<Integer, Boolean> vNode : vNodes.entrySet()) {
                int v = vNode.getKey();
                if (reversedGraph.get(v) == null) {
                    HashMap<Integer, Boolean> uNode = new HashMap<>();
                    uNode.put(u, true);
                    reversedGraph.put(v, uNode);
                }
                else {
                    reversedGraph.get(v).putIfAbsent(u, true);
                }

            }
        }
    }

    /**
     * Perform HITS iterations until convergence
     *
     * @param      titles  The titles of the documents in the root set
     */
    private void iterate(String[] titles) {
        //
        // YOUR CODE HERE
        //
        HITS hits = new HITS(titles);
        beginIterate(hits);
    }

    public void beginIterate(HITS hits) {
        HashSet<Integer>       baseIDs = hits.baseIDSet;
        HashSet<Integer>       rootIDs = hits.rootIdSet;
        HashMap<Integer, Double>  auth = new HashMap<>();
        HashMap<Integer, Double>   hub = new HashMap<>();
        HashMap<Integer, Double> auth_ = new HashMap<>();
        HashMap<Integer, Double>  hub_ = new HashMap<>();

        for (int id : baseIDs) {
            auth.put(id, 1.0);
            hub.put(id, 1.0);
        }

        int cnt = 1000;
        while (cnt > 0) {
//            System.out.println("cnt == " + cnt);
            auth_ = MatrixMulVector(reversedGraph, hub);
             hub_ = MatrixMulVector(pageRank.link, auth);

//             int t = 0;
//             System.out.print("old: ");
//             for (Map.Entry<Integer, Double> entry : auth.entrySet()) {
//                 t++;
//                 if (t > 10) break;
//                 System.out.print(entry.getValue() + " ");
//             }
//            System.out.print("\n");
//
//             t = 0;
//             System.out.print("new: ");
//             for (Map.Entry<Integer, Double> entry : auth_.entrySet()) {
//                 t++;
//                 if (t > 10) break;
//                 System.out.print(entry.getValue() + " ");
//             }

             if (diff(auth, auth_) && diff(hub, hub_)) break;
//             else cnt = 10;
             auth = (HashMap<Integer, Double>) auth_.clone();
              hub = (HashMap<Integer, Double>) hub_.clone();
              cnt--;
        }

               hubs = new HashMap<>();
        authorities = new HashMap<>();

        for (int id : baseIDs) {
            double authScore = 0;
            double  hubScore = 0;

            if (auth_.get(id) != null) authScore = auth_.get(id);
            if (hub_.get(id) != null)   hubScore = hub_.get(id);

            if (pageRank.docName[id] != null) {
                authorities.put(Integer.parseInt(pageRank.docName[id]), authScore);
                hubs.put(Integer.parseInt(pageRank.docName[id]), hubScore);
            }
        }


    }

    public HashMap<Integer, Double> MatrixMulVector(HashMap<Integer, HashMap<Integer, Boolean>> links, HashMap<Integer, Double> base) {
        HashMap<Integer, Double> ans = new HashMap<>();
        for (Map.Entry<Integer, Double> entry : base.entrySet()) {
            double sum = 0;
            if (links.get(entry.getKey()) != null) {
                HashMap<Integer, Boolean> vSet = links.get(entry.getKey());
                for (Map.Entry<Integer, Boolean> vNode : vSet.entrySet()) {
                    // 只将 base 里的点加进来
                    if (base.get(vNode.getKey()) != null) {
                        sum += base.get(vNode.getKey());
                    }
                }
            }
            if (base.get(entry.getKey()) != null) {
                ans.put(entry.getKey(), sum);
            }
        }
        return normalize(ans);
    }

    public HashMap<Integer, Double> normalize(HashMap<Integer, Double> ans) {
        double     sum = 0;
        double sqrtSum = 0;

        for (Map.Entry<Integer, Double> entry : ans.entrySet()) {
            sum += Math.pow(entry.getValue(), 2);
        }
        sqrtSum = Math.sqrt(sum);

        for (Map.Entry<Integer, Double> entry : ans.entrySet()) {
            ans.put(entry.getKey(), entry.getValue() / sqrtSum);
        }

        return ans;
    }

    public boolean diff(HashMap<Integer, Double> now, HashMap<Integer, Double> previous) {
        for (Map.Entry<Integer, Double> entry : now.entrySet()) {
            double difference = Math.abs(now.get(entry.getKey()) - previous.get(entry.getKey()));
            if (difference > EPSILON) return true;
        }
        return false;
    }


    /**
     * Rank the documents in the subgraph induced by the documents present
     * in the postings list `post`.
     *
     * @param      post  The list of postings fulfilling a certain information need
     *
     * @return     A list of postings ranked according to the hub and authority scores.
     */
    PostingsList rank(PostingsList post) {
        //
        // YOUR CODE HERE
        //
        ArrayList<PostingsEntry> entryArrayList = post.getList();
//        System.out.println("entry size == " + entryArrayList.size());

        String[] titles = new String[entryArrayList.size()];

        HashMap<String, Integer> fileNameToRealDocId = new HashMap<>();

        // 拿到查询到的文件的 title
        int cnt = 0;
//        int beforeSum = 0;
        System.out.println();
        for (PostingsEntry postingsEntry : entryArrayList) {
            String fileName = getFileName(index.docNames.get(postingsEntry.docID));
            titles[cnt++] = fileName;
            fileNameToRealDocId.put(fileName, postingsEntry.docID);

//            beforeSum += postingsEntry.score;
        }
//        System.out.println("beforeSum == " + beforeSum);

        System.out.println("title == " + Arrays.toString(titles));
        HITS hits = new HITS(titles);
        beginIterate(hits);
        HashMap<Integer, Double> scores = new HashMap<>();

        double wAuth = 0.5;
        double  wHub = 0.5;
        for (Map.Entry<Integer, Double> entry : authorities.entrySet()) {
            int        docID = entry.getKey();
            double scoreAuth = entry.getValue();
            double  scoreHub = hubs.get(docID);
            double       sum = wAuth * scoreAuth + wHub * scoreHub;
            scores.put(docID, sum);
        }

        PostingsList postingsListResult = new PostingsList();

        for (Integer baseId : hits.baseIDSet) {
            Integer   docId = Integer.parseInt(pageRank.docName[baseId]);
            String docTitle = getFileName(idToTitle.get(docId));


//                System.out.println("not null");
            if (index.fileNameToDocId.get(docTitle) != null) {
                int     realDocId = index.fileNameToDocId.get(docTitle);
                double finalScore = scores.get(docId);
//                System.out.println("docTitle == " + docTitle + ", realDocId == " + realDocId);
                postingsListResult.insert(realDocId, finalScore, 0);
            }
        }

//        for (Integer rootId : hits.rootIdSet) {
//            Integer   docId = Integer.parseInt(pageRank.docName[rootId]);
//            String docTitle = getFileName(idToTitle.get(docId));
//
//            if (fileNameToRealDocId.get(docTitle) != null) {
////                System.out.println("not null");
//                int     realDocId = fileNameToRealDocId.get(docTitle);
//                double finalScore = scores.get(docId);
////                System.out.println("docTitle == " + docTitle + ", realDocId == " + realDocId);
//                postingsListResult.insert(realDocId, finalScore, 0);
//            }
//        }
        Collections.sort(postingsListResult.getList());
        return postingsListResult;

    }


    /**
     * Sort a hash map by values in the descending order
     *
     * @param      map    A hash map to sorted
     *
     * @return     A hash map sorted by values
     */
    private HashMap<Integer,Double> sortHashMapByValue(HashMap<Integer,Double> map) {
        if (map == null) {
            return null;
        } else {
            List<Map.Entry<Integer,Double> > list = new ArrayList<Map.Entry<Integer,Double> >(map.entrySet());
      
            Collections.sort(list, new Comparator<Map.Entry<Integer,Double>>() {
                public int compare(Map.Entry<Integer,Double> o1, Map.Entry<Integer,Double> o2) { 
                    return (o2.getValue()).compareTo(o1.getValue()); 
                } 
            }); 
              
            HashMap<Integer,Double> res = new LinkedHashMap<Integer,Double>(); 
            for (Map.Entry<Integer,Double> el : list) { 
                res.put(el.getKey(), el.getValue()); 
            }
            return res;
        }
    } 


    /**
     * Write the first `k` entries of a hash map `map` to the file `fname`.
     *
     * @param      map        A hash map
     * @param      fname      The filename
     * @param      k          A number of entries to write
     */
    void writeToFile(HashMap<Integer,Double> map, String fname, int k) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fname));
            
            if (map != null) {
                int i = 0;
                for (Map.Entry<Integer,Double> e : map.entrySet()) {
                    i++;
                    writer.write(e.getKey() + ": " + String.format("%.5g%n", e.getValue()));
                    if (i >= k) break;
                }
            }
            writer.close();
        } catch (IOException e) {}
    }


    /**
     * Rank all the documents in the links file. Produces two files:
     *  hubs_top_30.txt with documents containing top 30 hub scores
     *  authorities_top_30.txt with documents containing top 30 authority scores
     */
    void rank() {
        iterate(titleToId.keySet().toArray(new String[0]));
        HashMap<Integer,Double> sortedHubs = sortHashMapByValue(hubs);
        HashMap<Integer,Double> sortedAuthorities = sortHashMapByValue(authorities);
        writeToFile(sortedHubs, "hubs_top_30.txt", 30);
        writeToFile(sortedAuthorities, "authorities_top_30.txt", 30);
    }


    /* --------------------------------------------- */


    public static void main( String[] args ) {
        if ( args.length != 2 ) {
            System.err.println( "Please give the names of the link and title files" );
        }
        else {
            HITSRanker hr = new HITSRanker( args[0], args[1], null );
            hr.rank();
        }
    }
} 