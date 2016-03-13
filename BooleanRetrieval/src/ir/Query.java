/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Hedvig Kjellström, 2012
 */

package ir;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.StringTokenizer;

public class Query {

	public LinkedList<String> terms = new LinkedList<String>();
	public LinkedList<Double> weights = new LinkedList<Double>();

	/**
	 * Creates a new empty Query
	 */
	public Query() {
	}

	/**
	 * Creates a new Query from a string of words
	 */
	public Query(String queryString) {
		StringTokenizer tok = new StringTokenizer(queryString);
		while (tok.hasMoreTokens()) {
			terms.add(tok.nextToken());
			weights.add(new Double(1));
		}
	}

	/**
	 * Returns the number of terms
	 */
	public int size() {
		return terms.size();
	}

	/**
	 * Returns a shallow copy of the Query
	 */
	public Query copy() {
		Query queryCopy = new Query();
		queryCopy.terms = (LinkedList<String>) terms.clone();
		queryCopy.weights = (LinkedList<Double>) weights.clone();
		return queryCopy;
	}

	/**
	 * Expands the Query using Relevance Feedback
	 */
	public void relevanceFeedback(PostingsList results, boolean[] docIsRelevant, Indexer indexer) {
		// results contain the ranked list from the current search
		// docIsRelevant contains the users feedback on which of the 10 first
		// hits are relevant
		
		int numRelevant = 0;
		for (int rank = 0; rank < docIsRelevant.length; rank++) {
			if (docIsRelevant[rank])
				numRelevant++;
		}
		
		for (int word = 0; word < weights.size(); word++) {
			weights.set(word, weights.get(word)/weights.size());
		}
		
		// Change the numerator to change Beta, the relevant doc word weights
		double docWordsWeight = 1 / numRelevant;

		int[] docIds = new int[docIsRelevant.length];
		Iterator<PostingsEntry> topEntries = results.getScoreSortedIterator();
		Set<String> encounteredThisIteration = new HashSet<String>();
		for (int entry = 0; entry < docIds.length && topEntries.hasNext(); entry++) {
			int docId = topEntries.next().docID;
			docIds[entry] = docId;
			Set<String> wordSet = (((HashedIndex)indexer.index).getWordsInDoc(docId));
			double docWordsWeightInDocument = docWordsWeight / ((HashedIndex)indexer.index).docLengths.get(docId);
			for (String word: wordSet) {
				if (!terms.contains(word)) {
					terms.add(word);
					weights.add(docWordsWeightInDocument);
				} else {
					int indexOfWord = terms.indexOf(word);
					weights.set(indexOfWord, weights.get(indexOfWord) + docWordsWeightInDocument);
				}
			}
		}
		
		
	}

	public LinkedList<String> getQueryTerms() {
		return terms;
	}
}
