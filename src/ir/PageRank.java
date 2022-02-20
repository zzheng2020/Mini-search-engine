package ir;

import java.lang.reflect.Array;
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


    /**  
     *   Maximal number of documents. We're assuming here that we
     *   don't have more docs than we can keep in main memory.
     */
    final static int MAX_NUMBER_OF_DOCS = 2000000;

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
		iterate( noOfDocs, 1000 );
    }


    /* --------------------------------------------- */


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
			for (int i = 0; i<x_.length; i++) {
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


    public static void main( String[] args ) {
		if ( args.length != 1 ) {
			System.err.println( "Please give the name of the link file" );
		}
		else {
			new PageRank( args[0] );
		}
    }
}