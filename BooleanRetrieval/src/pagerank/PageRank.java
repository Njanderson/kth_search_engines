package pagerank;

/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2012
 */

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Modified version that was used to interface with the HashedIndex, 
 * but then became deprecated when I merely read from a file instead.
 * @author Nick Anderson
 *
 */
public class PageRank {

	/**
	 * Maximal number of documents. We're assuming here that we don't have more
	 * docs than we can keep in main memory.
	 */
	final static int MAX_NUMBER_OF_DOCS = 2000000;

	/**
	 * Mapping from document names to document numbers.
	 */
	Hashtable<String, Integer> docNumber = new Hashtable<String, Integer>();

	/**
	 * Mapping from document numbers to document names
	 */
	String[] docName = new String[MAX_NUMBER_OF_DOCS];

	/**
	 * A memory-efficient representation of the transition matrix. The outlinks
	 * are represented as a Hashtable, whose keys are the numbers of the
	 * documents linked from.
	 * <p>
	 *
	 * The value corresponding to key i is a Set whose values are all the
	 * numbers of documents j that i links to.
	 * <p>
	 *
	 * If there are no outlinks from i, then the value corresponding key i is
	 * null.
	 */
	Hashtable<Integer, List<Integer>> link = new Hashtable<Integer, List<Integer>>();

	Hashtable<String, String> fileNamesToDocId = new Hashtable<String, String>();

	/**
	 * The number of outlinks from each node.
	 */
	int[] out = new int[MAX_NUMBER_OF_DOCS];

	/**
	 * The number of documents with no outlinks.
	 */
	int numberOfSinks = 0;


	/**
	 * The probability that the surfer will be bored, stop following links, and
	 * take a random jump somewhere.
	 */
	final static double BORED = 0.15;

	/**
	 * Convergence criterion: Transition probabilities do not change more that
	 * EPSILON from one iteration to another.
	 */
	final static double EPSILON = 0.0001;

	/**
	 * Never do more than this number of iterations regardless of whether the
	 * transition probabilities converge or not.
	 */
	final static int MAX_NUMBER_OF_ITERATIONS = 1000;

	/* --------------------------------------------- */	
	
	private Map<Integer, PageRankTuple> trueValueMap = new HashMap<Integer, PageRankTuple>();
	
	public PageRank(String filename) {
		int noOfDocs = readDocs(filename);
		PageRankTupleComparatorDec decComparator = new PageRankTupleComparatorDec();
		PageRankTupleComparatorInc incComparator = new PageRankTupleComparatorInc();
		
		System.out.println("Computing Page Rank...");
		PriorityQueue<PageRankTuple> trueValueDecQueue = new PriorityQueue<PageRankTuple>(decComparator);
		PriorityQueue<PageRankTuple> trueValueIncQueue = new PriorityQueue<PageRankTuple>(incComparator);
		Set<PageRankTuple> trueValue = computePagerankPowerIteration(noOfDocs);
		trueValueDecQueue.clear();
		trueValueDecQueue.addAll(trueValue);
		trueValueIncQueue.clear();
		trueValueIncQueue.addAll(trueValue);
		trueValueMap.clear();
		for (PageRankTuple val: trueValue) {
			trueValueMap.put(val.docId, val);
		}
		System.out.println("Page Rank Calculation Complete!");
	}
	
	/**
	 * 
	 * @param filename
	 * @param hashIndexDocIdToFilename Map from DocId to a filename
	 */
	public PageRank(String filename, String pageRankDocIdsToFileNamesFile, HashMap<String, String> hashIndexDocIdToFilename) {
		for (String docId: hashIndexDocIdToFilename.keySet()) {
			String filenameWithoutPath = hashIndexDocIdToFilename.get(docId);
			fileNamesToDocId.put(hashIndexDocIdToFilename.get(docId), docId);
		}
		Map<Integer, String> mappings = readMappings(pageRankDocIdsToFileNamesFile);
		System.out.println("mappings.size() = " + mappings.size());
		int noOfDocs = readDocs(filename);
		PageRankTupleComparatorDec decComparator = new PageRankTupleComparatorDec();
		PageRankTupleComparatorInc incComparator = new PageRankTupleComparatorInc();
		
		System.out.println("Computing Page Rank...");
		PriorityQueue<PageRankTuple> trueValueDecQueue = new PriorityQueue<PageRankTuple>(decComparator);
		PriorityQueue<PageRankTuple> trueValueIncQueue = new PriorityQueue<PageRankTuple>(incComparator);
		Set<PageRankTuple> trueValue = computePagerankPowerIteration(noOfDocs);
		trueValueDecQueue.clear();
		trueValueDecQueue.addAll(trueValue);
		trueValueIncQueue.clear();
		trueValueIncQueue.addAll(trueValue);
		trueValueMap.clear();
		for (PageRankTuple val: trueValue) {
			// place in external docIds
			Integer localIntegerDocName = Integer.parseInt(docName[val.docId]);
			String docFilename = mappings.get(localIntegerDocName);
			String externalDocId = fileNamesToDocId.get(docFilename);
			// don't include files that don't actually exist
			if (externalDocId != null) {
				trueValueMap.put(Integer.parseInt(externalDocId), val);
			}
		}
		System.out.println("Page Rank Calculation Complete!");
	}

	private Map<Integer, String> readMappings(String filename) {
		Map<Integer, String> mappings = new HashMap<Integer, String>();
		BufferedReader in;
		try {
			System.err.print("Reading mappings file... ");
			in = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = in.readLine()) != null) {
				int index = line.indexOf(";");
				int docId = Integer.parseInt(line.substring(0, index));
				String docName = "src\\resources\\davisWiki\\" + line.substring(index + 1, line.length()) + ".f";
				mappings.put(docId, docName);
			}
			in.close();
		} catch (FileNotFoundException e) {
			System.err.println("File " + filename + " not found!");
		} catch (IOException e) {
			System.err.println("Error reading file " + filename);
		}
		return mappings;
	}

	/*
	 * Modifies the state of the PageRank, so do not change the PageRankTuples!
	 */
	public Map<Integer, PageRankTuple> getPageRankMap() {
		return trueValueMap;
	}
		
	/* --------------------------------------------- */

	/**
	 * Reads the documents and creates the docs table. When this method finishes
	 * executing then the @code{out} vector of outlinks is initialised for each
	 * doc, and the @code{p} matrix is filled with zeroes (that indicate direct
	 * links) and NO_LINK (if there is no direct link.
	 * <p>
	 *
	 * @return the number of documents read.
	 */
	int readDocs(String filename) {
		int fileIndex = 0;
		BufferedReader in;
		try {
			System.err.print("Reading file... ");
			in = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = in.readLine()) != null && fileIndex < MAX_NUMBER_OF_DOCS) {
				int index = line.indexOf(";");
				String title = line.substring(0, index);
				Integer fromdoc = docNumber.get(title);
				// Have we seen this document before?
				if (fromdoc == null) {
					// This is a previously unseen doc, so add it to the table.
					fromdoc = fileIndex++;
					docNumber.put(title, fromdoc);
					docName[fromdoc] = title;
				}
				// Check all outlinks.
				StringTokenizer tok = new StringTokenizer(line.substring(index + 1), ",");
				while (tok.hasMoreTokens() && fileIndex < MAX_NUMBER_OF_DOCS) {
					String otherTitle = tok.nextToken();
					Integer otherDoc = docNumber.get(otherTitle);
					if (otherDoc == null) {
						// This is a previously unseen doc, so add it to the
						// table.
						otherDoc = fileIndex++;
						docNumber.put(otherTitle, otherDoc);
						docName[otherDoc] = otherTitle;
					}
					// Set the probability to 0 for now, to indicate that there
					// is a link from fromdoc to otherDoc.
					if (link.get(fromdoc) == null) {
						link.put(fromdoc, new LinkedList<Integer>());
					}
					if (!link.get(fromdoc).contains(otherDoc)) {
						link.get(fromdoc).add(otherDoc);
						out[fromdoc]++;
					}
				}
			}
			if (fileIndex >= MAX_NUMBER_OF_DOCS) {
				System.err.print("stopped reading since documents table is full. ");
			} else {
				System.err.print("done. ");
			}
			// Compute the number of sinks.
			for (int i = 0; i < fileIndex; i++) {
				if (out[i] == 0)
					numberOfSinks++;
			}
			in.close();
		} catch (FileNotFoundException e) {
			System.err.println("File " + filename + " not found!");
		} catch (IOException e) {
			System.err.println("Error reading file " + filename);
		}
		
		System.out.println("Read " + fileIndex + " number of documents");
		return fileIndex;
	}
	
	boolean hasCovergedCompareDocs(PriorityQueue<PageRankTuple> trueValueQueue, Map<Integer, PageRankTuple> mcMethodMap, int numToInclude) {
		PageRankTuple[] retainTrue = new PageRankTuple[numToInclude+1]; // + 1 to handle indexing more easily
		double epsln = 0.0;
		double maxError = 0.0001;
		for (int i = 1; i <= numToInclude; i++) {
			PageRankTuple trueValue = trueValueQueue.poll();
			PageRankTuple mcMethod = mcMethodMap.get(trueValue.docId);
			if (maxError <= Math.abs(trueValue.pageRank - mcMethod.pageRank))
				return false;
//			epsln += Math.abs(trueValue.pageRank - mcMethod.pageRank);
			retainTrue[i] = trueValue;
		}
		// re-introduce to pq
		for (int i = 1; i < numToInclude; i++) {
			trueValueQueue.add(retainTrue[i]);
		}
//		System.out.println(epsln);
		return true;
//		return epsln <= maxError;
	}
	
	boolean hasConverged(PriorityQueue<PageRankTuple> trueValueQueue, PriorityQueue<PageRankTuple> mcMethodQueue, int numToInclude) {
		PageRankTuple[] retainTrue = new PageRankTuple[numToInclude+1]; // + 1 to handle indexing more easily
		PageRankTuple[] retainMC = new PageRankTuple[numToInclude+1]; // + 1 to handle indexing more easily
		double epsln = 0.0;
		double maxError = 0.0001;
		for (int i = 1; i <= numToInclude; i++) {
			PageRankTuple trueValue = trueValueQueue.poll();
			PageRankTuple mcMethod = mcMethodQueue.poll();
			if (maxError <= Math.abs(trueValue.pageRank - mcMethod.pageRank))
				return false;
//			epsln += Math.abs(trueValue.pageRank - mcMethod.pageRank);
			retainTrue[i] = trueValue;
			retainMC[i] = mcMethod;
		}
		// re-introduce to pq
		for (int i = 1; i < numToInclude; i++) {
			trueValueQueue.add(retainTrue[i]);
			mcMethodQueue.add(retainMC[i]);
		}
		return true;
//		return epsln <= maxError;
	}
	
	double computeSumOfSqrDiff(PriorityQueue<PageRankTuple> trueValueQueue, PriorityQueue<PageRankTuple> mcMethodQueue, int numToInclude) {
		PageRankTuple[] retainTrue = new PageRankTuple[numToInclude+1]; // + 1 to handle indexing more easily
		PageRankTuple[] retainMC = new PageRankTuple[numToInclude+1]; // + 1 to handle indexing more easily
		double sumOfLeastSqrs = 0.0;
		for (int i = 1; i <= numToInclude; i++) {
			PageRankTuple trueValue = trueValueQueue.poll();
			PageRankTuple mcMethod = mcMethodQueue.poll();
			sumOfLeastSqrs += Math.pow(trueValue.pageRank - mcMethod.pageRank, 2);
			retainTrue[i] = trueValue;
			retainMC[i] = mcMethod;
		}
		// re-introduce to pq
		for (int i = 1; i <= numToInclude; i++) {
			trueValueQueue.add(retainTrue[i]);
			mcMethodQueue.add(retainMC[i]);
		}
		return sumOfLeastSqrs;
	}
	
	/*
	 * Leaves pq intact
	 */
	void printTopResults(PriorityQueue<PageRankTuple> queue, int numToPrint) {
//		PageRankTuple[] retain = new PageRankTuple[numToPrint+1]; // + 1 to handle indexing more easily
//		for (int i = 1; i <= numToPrint; i++) {
//			PageRankTuple top = queue.poll();
//			System.out.println(i + ". " + docName[top.docId] + ", " + top.pageRank);
//			retain[i] = top;
//		}
//		// re-introduce to pq
//		for (int i = 1; i <= numToPrint; i++) {
//			queue.add(retain[i]);
//		}
	}

	/* --------------------------------------------- */
	
	/*
	 * Computes the pagerank of each document using POWER ITERATION.
	 */
	Set<PageRankTuple> computePagerankPowerIteration(int numberOfDocs) {
		// Current iteration
		int iteration = 1;
		// Initialize probabilities of each state
		double delta = 1;
		double[] nextProb = new double[numberOfDocs];
		double[] prob = new double[numberOfDocs];
		prob[0] = 1;
		while (delta > EPSILON && iteration < MAX_NUMBER_OF_ITERATIONS) {
			iteration++;
			delta = 0;
			for (int state = 0; state < prob.length; state++) {
				List<Integer> outLinks = link.get(state);
				if (outLinks != null) {
					// Distribute to outlinks
					for (Integer outLink: outLinks) {
						double toAdd = (1.0 - BORED) * prob[state] / outLinks.size();
						nextProb[outLink] += toAdd;
					}
				} else {
					// Distribute randomly
					double toDist = (1.0 - BORED) * prob[state] / numberOfDocs;
					for (int randomDoc = 0; randomDoc < nextProb.length; randomDoc++) {
						nextProb[randomDoc] += toDist;
					}
				}
				for (int doc = 0; doc < nextProb.length; doc++) {
					double toDist = BORED * prob[state] / numberOfDocs;
					nextProb[doc] += toDist;
				}
			}
			for (int doc = 0; doc < nextProb.length; doc++) {
				delta += Math.abs(nextProb[doc] - prob[doc]);
			}
			prob = nextProb;
			nextProb = new double[numberOfDocs];
		}
        Set<PageRankTuple> toRet = new HashSet<PageRankTuple>();
		for (int state = 0; state < prob.length; state++) {
			toRet.add(new PageRankTuple(state, prob[state]));
		}
		return toRet;
	}
	
	public class PageRankTuple implements Comparable<PageRankTuple> {
		public int docId;
		public double pageRank;
		
		public PageRankTuple(int docId, double pageRank) {
			this.docId = docId;
			this.pageRank = pageRank;
		}

		@Override
		public int compareTo(PageRankTuple o) {
			int pageRankComparison = Double.compare(this.pageRank, o.pageRank);
			if (pageRankComparison == 0) {
				return docId - o.docId;
			} else {
				return pageRankComparison;
			}
		}
	}
	
	public class PageRankTupleComparatorDec implements Comparator<PageRankTuple> {
		@Override
		public int compare(PageRankTuple x, PageRankTuple y) {
			return y.compareTo(x);
		}
	}
	
	public class PageRankTupleComparatorInc implements Comparator<PageRankTuple> {
		@Override
		public int compare(PageRankTuple x, PageRankTuple y) {
			return x.compareTo(y);
		}
	}
	
	Set<PageRankTuple> computePagerankMCRandomStart(int numberOfDocs, int numWalks) {
		Random rand = new Random(System.currentTimeMillis());
		double[] hits = new double[numberOfDocs];
		for (int walk = 0; walk < numWalks; walk++) {
			int choice = rand.nextInt(numberOfDocs);
			List<Integer> outLinks = link.get(choice);
			while (rand.nextDouble() > BORED) {
				if (outLinks != null) {
					int randInt = rand.nextInt(outLinks.size());
					choice = outLinks.get(randInt);
					outLinks = link.get(choice);
				} else {
					choice = rand.nextInt(numberOfDocs);
					outLinks = link.get(choice);
				}
			}
			hits[choice]++;
		}
		
        Set<PageRankTuple> toRet = new HashSet<PageRankTuple>();
		double totalProb = 0.0;
		for (int state = 0; state < hits.length; state++) {
			hits[state] /= numWalks;
			toRet.add(new PageRankTuple(state, hits[state]));
			totalProb += hits[state];
		}
//		System.out.println("Probability is: " + totalProb);
		return toRet;
	}
	
	Set<PageRankTuple> computePagerankMCCyclicStart(int numberOfDocs, int numWalksPerDoc) {
		Random rand = new Random(System.currentTimeMillis());
		double[] hits = new double[numberOfDocs];
		for (int doc = 0; doc < numberOfDocs; doc++) {
			for (int walkNum = 0; walkNum < numWalksPerDoc; walkNum++) {
				int choice = doc;
				List<Integer> outLinks = link.get(choice);
				while (rand.nextDouble() > BORED) {
					if (outLinks != null) {
						int randInt = rand.nextInt(outLinks.size());
						choice = outLinks.get(randInt);
						outLinks = link.get(choice);
					} else {
						choice = rand.nextInt(numberOfDocs);
						outLinks = link.get(choice);
					}
				}
				hits[choice]++;
			}
		}
		
		Set<PageRankTuple> toRet = new HashSet<PageRankTuple>();
		double totalProb = 0.0;
		for (int doc = 0; doc < hits.length; doc++) {
			hits[doc] /= numberOfDocs * numWalksPerDoc;
			toRet.add(new PageRankTuple(doc, hits[doc]));
			totalProb += hits[doc];
		}
//		System.out.println("Probability is: " + totalProb);
		return toRet;
	}

	Set<PageRankTuple> computePagerankMCCompletePath(int numberOfDocs, int numWalksPerDoc) {
		Random rand = new Random(System.currentTimeMillis());
		double[] hits = new double[numberOfDocs];
		int totalLength = 0;
		for (int doc = 0; doc < numberOfDocs; doc++) {
			for (int walkNum = 0; walkNum < numWalksPerDoc; walkNum++) {
				int choice = doc;
				totalLength++;
				hits[choice]++;
				List<Integer> outLinks = link.get(choice);
				while (rand.nextDouble() > BORED) {
					if (outLinks != null) {
						int randInt = rand.nextInt(outLinks.size());
						choice = outLinks.get(randInt);
						outLinks = link.get(choice);
					} else {
						choice = rand.nextInt(numberOfDocs);
						outLinks = link.get(choice);
					}
					totalLength++;
					hits[choice]++;
				}
			}
		}
		
		Set<PageRankTuple> toRet = new HashSet<PageRankTuple>();
		double totalProb = 0.0;
		for (int doc = 0; doc < hits.length; doc++) {
			hits[doc] /= totalLength;
			toRet.add(new PageRankTuple(doc, hits[doc]));
			totalProb += hits[doc];
		}
//		System.out.println("Probability is: " + totalProb);
		return toRet;
	}
	
	Set<PageRankTuple> computePagerankMCCompletePathStopDangling(int numberOfDocs, int numWalksPerDoc) {
		Random rand = new Random(System.currentTimeMillis());
		double[] hits = new double[numberOfDocs];
		int totalLength = 0;
		for (int doc = 0; doc < numberOfDocs; doc++) {
			for (int walkNum = 0; walkNum < numWalksPerDoc; walkNum++) {
				int choice = doc;
				totalLength++;
				List<Integer> outLinks = link.get(choice);
				hits[choice]++;
				while (rand.nextDouble() > BORED && outLinks != null) {
					int randInt = rand.nextInt(outLinks.size());
					choice = outLinks.get(randInt);
					outLinks = link.get(choice);
					hits[choice]++;
					totalLength++;
				}
			}
		}
		
		Set<PageRankTuple> toRet = new HashSet<PageRankTuple>();
		double totalProb = 0.0;
		for (int doc = 0; doc < hits.length; doc++) {
			hits[doc] /= totalLength;
			toRet.add(new PageRankTuple(doc, hits[doc]));
			totalProb += hits[doc];
		}
//		System.out.println("Probability is: " + totalProb);
		return toRet;
	}
	
	Set<PageRankTuple> computePagerankMCCompletePathRandomStart(int numberOfDocs, int numWalks) {
		Random rand = new Random(System.currentTimeMillis());
		double[] hits = new double[numberOfDocs];
		int totalLength = 0;
		for (int walk = 0; walk < numWalks; walk++) {
			int choice = rand.nextInt(numberOfDocs);
			totalLength++;
			hits[choice]++;
			List<Integer> outLinks = link.get(choice);
			while (rand.nextDouble() > BORED && outLinks != null) {
				int randInt = rand.nextInt(outLinks.size());
				choice = outLinks.get(randInt);
				outLinks = link.get(choice);
				hits[choice]++;
				totalLength++;
			}
		}
		Set<PageRankTuple> toRet = new HashSet<PageRankTuple>();
		double totalProb = 0.0;
		for (int doc = 0; doc < hits.length; doc++) {
			hits[doc] /= totalLength;
			toRet.add(new PageRankTuple(doc, hits[doc]));
			totalProb += hits[doc];
		}
//		System.out.println("Probability is: " + totalProb);
		return toRet;
	}	

	/* --------------------------------------------- */
	
}
