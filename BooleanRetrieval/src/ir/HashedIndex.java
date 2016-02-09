/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 *   Additions: Hedvig Kjellström, 2012-14
 */  


package ir;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import ir.PostingsList;


/**
 *   Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex implements Index {

    /** The index as a hashtable. */
    private HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();


    /**
     *  Inserts this token in the index.
     */
    public void insert( String token, int docID, int offset ) {
	//
	//  YOUR CODE HERE
	//
    	if (!index.containsKey(token)) {
    		index.put(token, new PostingsList());
    	}
    	index.get(token).put(docID, offset);
    }


//    /**
//     *  Returns all the words in the index.
//     */
//    public Iterator<String> getDictionary() {
//	// 
//	//  REPLACE THE STATEMENT BELOW WITH YOUR CODE
//	//
//    	return index.keySet().iterator();
//    }


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
	// 
	//  REPLACE THE STATEMENT BELOW WITH YOUR CODE
	//
    	return index.get(token);
    }


    /**
     *  Searches the index for postings matching the query.
     */
    public PostingsList search( Query query, int queryType, int rankingType, int structureType ) {
	// 
	//  REPLACE THE STATEMENT BELOW WITH YOUR CODE
	//
    	LinkedList<String> queryTerms = query.getQueryTerms();
    	if (queryType == Index.PHRASE_QUERY) {
    		
    		// get basic intersection to limit possible documents
    		PostingsList retList = index.get(queryTerms.get(0));
    		if (queryTerms.size() > 1) {
    			retList = intersectPostingsLists(false, retList, index.get(queryTerms.get(1)));
    			for (int queryTermIndex = 2; queryTermIndex < queryTerms.size() && !retList.isEmpty(); queryTermIndex++) {
    				retList = intersectPostingsLists(false, retList, index.get(queryTerms.get(queryTermIndex)));
    			}
    		}
    		
    		Iterator<Integer> docIter = retList.docIDtoPostings.keySet().iterator();
    		while (docIter.hasNext()) {
    			Integer docId = docIter.next();
    			ListIterator<Integer>[] positionIterators = new ListIterator[queryTerms.size()];
    			for (int queryTermIndex = 0; queryTermIndex < queryTerms.size(); queryTermIndex++) {
    				positionIterators[queryTermIndex] = index.get(queryTerms.get(queryTermIndex)).get(docId).offsets.listIterator();
    			}
    			int queryWordIndex = 0;
				int currPosition = positionIterators[queryWordIndex].next();
				try {
					while (queryWordIndex < positionIterators.length - 1) {
						queryWordIndex++;
	    				currPosition++;
	    				int nextPosition = positionIterators[queryWordIndex].next();
	    				while (currPosition > nextPosition) {
	    					nextPosition = positionIterators[queryWordIndex].next();
	    				}
	    				if (currPosition != nextPosition) {
	    					positionIterators[queryWordIndex].previous();
	    					queryWordIndex = 0;
	    					currPosition = positionIterators[queryWordIndex].next();
	    				}
	    			}
				} catch (NoSuchElementException notFound) {
					docIter.remove();
				}
    		}
    		return retList;
    	} else {
    		PostingsList retList = index.get(queryTerms.get(0));
    		if (queryTerms.size() > 1) {
    			retList = intersectPostingsLists(true, retList, index.get(queryTerms.get(1)));
    			for (int queryTermIndex = 2; queryTermIndex < queryTerms.size() && !retList.isEmpty(); queryTermIndex++) {
    				retList = intersectPostingsLists(false, retList, index.get(queryTerms.get(queryTermIndex)));
    			}
    		}
    		return retList;
    	}    	
    }

	public PostingsList intersectPostingsLists(boolean includeFirst, PostingsList firList, PostingsList secList) {
    	PostingsList resList = new PostingsList();
    	Iterator<Integer> firIter = firList.docIDtoPostings.keySet().iterator();	
    	Iterator<Integer> secIter = secList.docIDtoPostings.keySet().iterator();      		
		secIter = secList.docIDtoPostings.keySet().iterator();
		if (firIter.hasNext() && secIter.hasNext()) {
			Integer firDocId = firIter.next();
    		Integer secDocId = secIter.next();
			while (firIter.hasNext() && secIter.hasNext()) {
	    		while (firDocId.compareTo(secDocId) > 0 && secIter.hasNext()) {
	    			// look for the firDocId in secList, stopping when we are on or past it
	    			secDocId = secIter.next();
	    		}
	    		if (secDocId.equals(firDocId)) {
	    			if (includeFirst) {
	        			resList.putList(firDocId, firList.get(firDocId));
	    			}
	    			resList.putList(secDocId, secList.get(secDocId));
	    			if (firIter.hasNext())
	    				firDocId = firIter.next();
	    		}
	    		while (secDocId.compareTo(firDocId) > 0  && firIter.hasNext()) {
	    			// we move on as far as we can to catch up if we need to
	    			firDocId = firIter.next();
	    		}                		
	    	}
			
			//check for end matches we may have missed
        	while (firDocId.compareTo(secDocId) > 0) {
    			// look for the firDocId in secList, stopping when we are on or past it
    			secDocId = secIter.next();
    		}
        	
        	// final check if they are matching on the last 
        	if (secDocId.equals(firDocId)) {
    			// found in second list
    			if (includeFirst) {
    				// due to fence post solution, during the first comparison,
    				// we want to add the positions from the first list we use to compare
    				// before we have the first result from our intersection
    				resList.putList(firDocId, firList.get(firDocId));
    			}
    			resList.putList(secDocId, secList.get(secDocId));
    		}
			
	    	return resList;
		} else {
			return new PostingsList();
		}
    }
    


    /**
     *  No need for cleanup in a HashedIndex.
     */
    public void cleanup() {
    }
}
