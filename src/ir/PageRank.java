package ir;

import java.util.*;
import java.io.*;

public class PageRank {

	static class Number implements Comparable<Number>{
		Double data;
		int index;

		Number(Double d, int i){
			this.data = d;
			this.index = i;
		}

		@Override
		public int compareTo(Number o) {
			return this.data.compareTo(o.data);
		}
	}

	public PageRank() {

	}

    /**  
     *   Maximal number of documents. We're assuming here that we
     *   don't have more docs than we can keep in main memory.
     */
    final static int MAX_NUMBER_OF_DOCS = 2000000;

	int MAX_ITERATION = 1000000;

    /**
     *   Mapping from document names to document numbers.
     */
    HashMap<String,Integer> docNumber = new HashMap<String,Integer>();

    /**
     *   Mapping from document numbers to document names
     */
    String[] docName = new String[MAX_NUMBER_OF_DOCS];

    /**  
     *   A memory-efficient representation of the transition matrix.
     *   The outlinks are represented as a HashMap, whose keys are 
     *   the numbers of the documents linked from.<p>
     *
     *   The value corresponding to key i is a HashMap whose keys are 
     *   all the numbers of documents j that i links to.<p>
     *
     *   If there are no outlinks from i, then the value corresponding 
     *   key i is null.
     */
    HashMap<Integer,HashMap<Integer,Boolean>> link = new HashMap<Integer,HashMap<Integer,Boolean>>();

	HashMap<Integer,HashMap<Integer,Boolean>> outlinks = new HashMap<Integer,HashMap<Integer,Boolean>>();

	ArrayList<Integer> nullLinks = new ArrayList<Integer>();

	// mapping from docID to pagerank score
	HashMap<Integer, Double> estimatedResult = new HashMap<>();

	HashMap<Integer, String> idToTitle = new HashMap<>();

    /**
     *   The number of outlinks from each node.
     */
    int[] out = new int[MAX_NUMBER_OF_DOCS];

    /**
     *   The probability that the surfer will be bored, stop
     *   following links, and take a random jump somewhere.
     */
    final static double BORED = 0.15;

    /**
     *   Convergence criterion: Transition probabilities do not 
     *   change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.0001;

       
    /* --------------------------------------------- */


    public PageRank( String filename ) {
		int noOfDocs = readDocs( filename );
//		iterate( noOfDocs, 1000 );

		int         m = 1;
		int         N = noOfDocs * m;
		double  alpha = 0.85;
		MAX_ITERATION = noOfDocs;

//		Number[] resultOfMonteCarlo = MonteCarlo1(noOfDocs, N, alpha);
		try {
			mapPageRankIdToFileName("/Users/zhangziheng/OneDrive/KTH/SEandIR_ZihengZhang/src/assignment2/pagerank/svwikiTitles.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (int i = 1; i <= 5; i++) {
			m = i;
			N = noOfDocs * m;
//			Number[] resultOfMonteCarlo = MonteCarlo2(noOfDocs, m, alpha);
//			Number[] resultOfMonteCarlo = MonteCarlo4(noOfDocs, m, alpha);
			Number[] resultOfMonteCarlo = MonteCarlo5(noOfDocs, N, alpha);
//			double diff = squaredSumDiff(resultOfMonteCarlo, noOfDocs);
//			System.out.println("m == " + m + ", N == " + N + ", diff == " + diff);
//			System.out.println("---------------------------");
		}

//		Number[] resultOfMonteCarlo = MonteCarlo2(noOfDocs, m, alpha);

//		Number[] resultOfMonteCarlo = MonteCarlo4(noOfDocs, m, alpha);

//		Number[] resultOfMonteCarlo = MonteCarlo5(noOfDocs, N, alpha);

//		double diff = squaredSumDiff(resultOfMonteCarlo, noOfDocs);
//		System.out.println("N == " + N + ", diff == " + diff);

    }


    /* --------------------------------------------- */

	public void mapPageRankIdToFileName(String fileName) throws IOException {
		BufferedReader in = new BufferedReader( new FileReader( fileName ));

		String line;
		while ((line = in.readLine()) != null) {
			String[] splitLine = line.split(";");
			int         id = Integer.parseInt(splitLine[0]);
			String docName = splitLine[1];

			idToTitle.put(id, docName);
		}
	}

	public Number[] readTrueDavisTop30(String fileName) {
		Number[] baseLine = new Number[100];
		try {
			BufferedReader in = new BufferedReader( new FileReader( fileName ));

			for (int i = 0; i < 30; i++) {
				baseLine[i] = new Number(0.0, 0);
			}
			int cnt = 0;
			String line;
			while ((line = in.readLine()) != null) {
				String[] splitLine = line.split(": ");
				int          docID = Integer.parseInt(splitLine[0]);
				double       score = Double.parseDouble(splitLine[1]);

				baseLine[cnt].index = docID;
				baseLine[cnt++].data = score;
			}
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
		return baseLine;
	}

	public double squaredSumDiff(Number[] pi, int noOfDocs) {
		estimatedResult.clear();
		Number[] baseLine = new Number[100];

		baseLine = readTrueDavisTop30("/Users/zhangziheng/OneDrive/KTH/SEandIR_ZihengZhang/src/assignment2/pagerank/davis_top_30.txt");


//		for (int i = 0; i < 30; i++) {
//			System.out.println("rank " + i + ", docNum: " + docName[rankedResult[i].index] + ": " + rankedResult[i].data);
//		}

//		for (int i = 0; i < 30; i++) {
//			System.out.println("docId == " + baseLine[i].index + ", score == " + baseLine[i].data);
//		}
//		System.out.println("-----------");

//		for (int i = 0; i < 30; i++) {
//			System.out.println("rank " + i + ", docNum: " + docName[rankedResult[i].index] + ": " + rankedResult[i].data);
//		}
		for (int i = 0; i < noOfDocs; i++) {
			int         trueDocID = Integer.parseInt(docName[pi[i].index]);
			double estimatedScore = pi[i].data;
			estimatedResult.put(trueDocID, estimatedScore);
		}


		double diff = 0;
		for (int i = 0; i < 30; i++) {
			int             docID = baseLine[i].index;
			double      trueScore = baseLine[i].data;
			double estimatedScore = estimatedResult.get(docID);

			diff += Math.pow(trueScore - estimatedScore, 2);
		}

		return diff;
	}


    /**
     *   Reads the documents and fills the data structures. 
     *
     *   @return the number of documents read.
     */
    int readDocs( String filename ) {
		int fileIndex = 0;
		try {
			System.err.print( "Reading file... " );
			BufferedReader in = new BufferedReader( new FileReader( filename ));
			String line;
			while ((line = in.readLine()) != null && fileIndex<MAX_NUMBER_OF_DOCS ) {
				int index = line.indexOf( ";" );
				String title = line.substring( 0, index );
				Integer fromdoc = docNumber.get( title );
				//  Have we seen this document before?
				if ( fromdoc == null ) {
					// This is a previously unseen doc, so add it to the table.
					fromdoc = fileIndex++;
					docNumber.put( title, fromdoc );
					docName[fromdoc] = title;
				}
				// Check all outlinks.
				StringTokenizer tok = new StringTokenizer( line.substring(index+1), "," );
				while ( tok.hasMoreTokens() && fileIndex<MAX_NUMBER_OF_DOCS ) {
					String otherTitle = tok.nextToken();
					Integer otherDoc = docNumber.get( otherTitle );
					if ( otherDoc == null ) {
						// This is a previousy unseen doc, so add it to the table.
						otherDoc = fileIndex++;
						docNumber.put( otherTitle, otherDoc );
						docName[otherDoc] = otherTitle;
					}
					// Set the probability to 0 for now, to indicate that there is
					// a link from fromdoc to otherDoc.
					if ( link.get(fromdoc) == null ) {
						link.put(fromdoc, new HashMap<Integer,Boolean>());
					}
					if ( link.get(fromdoc).get(otherDoc) == null ) {
						link.get(fromdoc).put( otherDoc, true );
						out[fromdoc]++;
					}
				}
			}
			if ( fileIndex >= MAX_NUMBER_OF_DOCS ) {
				System.err.print( "stopped reading since documents table is full. " );
			}
			else {
				System.err.print( "done. " );
			}
		}
		catch ( FileNotFoundException e ) {
			System.err.println( "File " + filename + " not found!" );
		}
		catch ( IOException e ) {
			System.err.println( "Error reading file " + filename );
		}
		System.err.println( "Read " + fileIndex + " number of documents" );
		return fileIndex;
    }


    /* --------------------------------------------- */


    /*
     *   Chooses a probability vector a, and repeatedly computes
     *   aP, aP^2, aP^3... until aP^i = aP^(i+1).
     */
    void iterate( int numberOfDocs, int maxIterations ) {

		// YOUR CODE HERE
		double[] x = new double[numberOfDocs];
		double[] x_ = new double[numberOfDocs];
		System.out.println("linkSize == " + link.size());

		// outLink{i, k}: exist an edge pointing from k to i
		for (int i = 0; i < numberOfDocs; i++) {

			if (link.get(i) != null) {

				for (int key : link.get(i).keySet()) {

					if (outlinks.get(key) != null) {

						outlinks.get(key).put(i, true);
					} else {
						HashMap<Integer, Boolean> ipointk = new HashMap<Integer, Boolean>();
						ipointk.put(i, true);
						outlinks.put(key, ipointk);
					}
				}
			}
			else {
				nullLinks.add(i);
			}
		}
		System.out.println("outLink == " + outlinks.size());
		System.out.println("nullLink == " + nullLinks.size());

		System.out.println("finish outLinks");




		x[0] = 1.0;
		int iteration = 0;

		do {
			if (iteration != 0) {
				x = x_.clone();
			}
			double nullValue = 0.0;
			for (Integer nullLink : nullLinks) {
				nullValue += (double) x[nullLink] * (1 - BORED) / (double) numberOfDocs;
			}

			for (int i = 0; i < numberOfDocs; i++) {
				x_[i] = BORED / (double)numberOfDocs;
				if (outlinks.get(i) != null) {
					for (int outLink : outlinks.get(i).keySet()) {
						x_[i] += x[outLink] * (1 - BORED) / link.get(outLink).size();
					}
				}
				x_[i] += nullValue;
			}
			double sum = 0.0;
			for (double v : x_) {
				sum += v;
			}
			for (int i = 0; i < x_.length; i++) {
				x_[i] = x_[i] / sum;
			}
			iteration++;
			System.out.println(diff(x, x_));
		} while (iteration < maxIterations && diff(x, x_) > EPSILON);

		System.out.println("=======");
//		double max = -1;
//		int item = 0;
//		for (int i = 0; i < numberOfDocs; i++) {
//			if (x_[i] > max) {
//				max = x_[i];
//				item = i;
//			}
//		}
//		System.out.println("max  == " + max);
//		System.out.println("item == " + item);
//		System.out.println("doc  == " + docName[item]);
		Number[] rankedResult = new Number[numberOfDocs];
		for (int i = 0; i < numberOfDocs; i++) {
			rankedResult[i] = new Number(x_[i], i);
		}

		Arrays.sort(rankedResult, Collections.reverseOrder());

		for (int i = 0; i < 30; i++) {
			System.out.println(docName[rankedResult[i].index] + ": " + rankedResult[i].data);
		}

		writeToDoc("/Users/zhangziheng/OneDrive/KTH/SEandIR_ZihengZhang/src/ir/myRankedResult.txt", rankedResult, numberOfDocs);

//		System.out.println("sort:");
//		Arrays.sort(x_);
//
//		for (int i = numberOfDocs - 1; i >= numberOfDocs - 30; i--) System.out.println(x_[i]);

	}

	public void writeToDoc(String fileName, Number[] result, int numberOfDocs) {
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileName));

			for (int i = 0; i < numberOfDocs; i++) {
				String str = docName[result[i].index] + ":" + result[i].data;
				bufferedWriter.write(str);
				bufferedWriter.newLine();
			}

			bufferedWriter.close();
			System.out.println("finished write rankedPage data to file");
		} catch ( IOException e ) {
			e.printStackTrace();
		}
	}

	double diff(double[] x1, double[] x2) {
		double difference = 0.0;
		for (int i = 0; i < x1.length; i++) {
			difference += Math.abs(x1[i] - x2[i]);
		}
		return difference;
	}


    /* --------------------------------------------- */



	public Number[] MonteCarlo1(int noOfDocs, int N, double alpha) {
		double[] pi = new double[noOfDocs];
		for (int i = 0; i < noOfDocs; i++) pi[i] = 0;

		for (int i = 0; i < N; i++) {
			int startID = (int) (Math.random() * noOfDocs);
			int   endID = randomWalk(startID, noOfDocs, alpha);
			pi[endID]++;
		}

		for (int i = 0; i < noOfDocs; i++) {
			pi[i] = pi[i] / N;
		}

		Number[] rankedResult = new Number[noOfDocs];
		for (int i = 0; i < noOfDocs; i++) {
			rankedResult[i] = new Number(pi[i], i);
		}

		Arrays.sort(rankedResult, Collections.reverseOrder());

		for (int i = 0; i < 30; i++) {
			System.out.println("rank " + i + ", docNum: " + docName[rankedResult[i].index] + ": " + rankedResult[i].data);
		}
		return rankedResult;
	}

	public Number[] MonteCarlo2(int noOfDocs, int m, double alpha) {
		double[] pi = new double[noOfDocs];
		for (int i = 0; i < noOfDocs; i++) pi[i] = 0;

		System.out.println("begin");
		for (int cnt = 0; cnt < m; cnt++) {
			for (int i = 0; i < noOfDocs; i++) {
				int startID = i;
				int   endID = randomWalk(startID, noOfDocs, alpha);
				pi[endID]++;
			}
		}
		System.out.println("end");

		for (int i = 0; i < noOfDocs; i++) {
			pi[i] = pi[i] / ((double) noOfDocs * m ) ;
		}

		Number[] rankedResult = new Number[noOfDocs];
		for (int i = 0; i < noOfDocs; i++) {
			rankedResult[i] = new Number(pi[i], i);
		}

		Arrays.sort(rankedResult, Collections.reverseOrder());

		for (int i = 0; i < 30; i++) {
			System.out.println("rank " + i + ", docNum: " + docName[rankedResult[i].index] + ": " + rankedResult[i].data);
		}
		return rankedResult;
	}

	public Number[] MonteCarlo4(int noOfDocs, int m, double alpha) {
		double[] pi = new double[noOfDocs];
		for (int i = 0; i < noOfDocs; i++) pi[i] = 0;

		int totalWalks = 0;
		for (int cnt = 0; cnt < m; cnt++) {
			for (int i = 0; i < noOfDocs; i++) {
				int startID = i;
				totalWalks = randomWalkForMC45(startID, alpha, pi, totalWalks);
			}
		}

		for (int i = 0; i < noOfDocs; i++) {
			pi[i] = pi[i] / (double) totalWalks;
		}

		Number[] rankedResult = new Number[noOfDocs];
		for (int i = 0; i < noOfDocs; i++) {
			rankedResult[i] = new Number(pi[i], i);
		}

		Arrays.sort(rankedResult, Collections.reverseOrder());

		for (int i = 0; i < 30; i++) {
			System.out.println("rank " + i + ", docNum: " + docName[rankedResult[i].index] + ": " + rankedResult[i].data);
		}

		return rankedResult;
	}

	public Number[] MonteCarlo5(int noOfDocs, int N, double alpha) {
		double[] pi = new double[noOfDocs];
		for (int i = 0; i < noOfDocs; i++) pi[i] = 0;

		int totalWalks = 0;
		for (int i = 0; i < N; i++) {
			int startID = (int) (Math.random() * noOfDocs);
			totalWalks = randomWalkForMC45(startID, alpha, pi, totalWalks);
		}
		for (int i = 0; i < noOfDocs; i++) {
			pi[i] = pi[i] / (double) totalWalks;
		}

		Number[] rankedResult = new Number[noOfDocs];
		for (int i = 0; i < noOfDocs; i++) {
			rankedResult[i] = new Number(pi[i], i);
		}

		Arrays.sort(rankedResult, Collections.reverseOrder());

		for (int i = 0; i < 30; i++) {
			System.out.println("rank " + i + ", docNum: " + docName[rankedResult[i].index] + ": " + rankedResult[i].data + "; " + idToTitle.get( Integer.parseInt(docName[rankedResult[i].index])));
		}

		return rankedResult;
	}

	public int randomWalk(int nowNode, int noOfDocs, double alpha) {
		int cnt = 0;
		int currentNode = nowNode;
		while (cnt < MAX_ITERATION) {
			cnt++;
			// 所有和 nowNode 相连接的点
			HashMap<Integer, Boolean> toNode = link.get(currentNode);

			// nowNode 存在至少一条出边
			if (toNode != null) {
				// 将 toNode 转化为数组 outNode
				ArrayList<Integer> outNode = new ArrayList<>();
				for (Map.Entry<Integer, Boolean> entry : toNode.entrySet()) {
					outNode.add(entry.getKey());
				}
				currentNode = outNode.get((new Random().nextInt(outNode.size())));
			}
			// nowNode 不存在出边，说明它是 sink Node
			else {
				currentNode = (int) (Math.random() * noOfDocs);
			}
			double currentAlpha = Math.random();
			if (currentAlpha > alpha) return currentNode;
		}
		return currentNode;
	}


	public int randomWalkForMC45(int nowNode, double alpha, double[] pi, int totalWalks) {
		int cnt = 0;

		int curNode = nowNode;
		while (cnt < MAX_ITERATION) {
			cnt++;
			totalWalks++;
			pi[curNode]++;
			// 所有和 nowNode 相连接的点
			HashMap<Integer, Boolean> toNode = link.get(curNode);

			// nowNode 存在至少一条出边
			if (toNode != null) {
				// 将 toNode 转化为数组 outNode
				ArrayList<Integer> outNode = new ArrayList<>();
				for (Map.Entry<Integer, Boolean> entry : toNode.entrySet()) {
					outNode.add(entry.getKey());
				}
				curNode = outNode.get((new Random().nextInt(outNode.size())));
			}
			// nowNode 不存在出边，说明它是 sink Node
			else {
				return totalWalks;
			}
			double currentAlpha = Math.random();
			if (currentAlpha > alpha) return totalWalks;
		}
		return totalWalks;
	}

    public static void main( String[] args ) {
		if ( args.length != 1 ) {
			System.err.println( "Please give the name of the link file" );
		}
		else {
			new PageRank( args[0] );
		}
    }
}