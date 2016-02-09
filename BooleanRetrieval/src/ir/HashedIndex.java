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
import java.util.Map;
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
//    		Map<String, PostingsList> resultsMap = new HashMap<String, PostingsList>();
//        	PostingsList resultsList = index.get(queryTerms.get(0)).clone();
//        	resultsMap.put(queryTerms.get(0), resultsList);
//        	for (int i = 1; i < queryTerms.size(); i++) {
//        		String queryTerm = queryTerms.get(i);
//        		if (!resultsMap.containsKey(queryTerm)) {
//        			PostingsList res = index.get(queryTerm).clone();
//            		resultsMap.put(queryTerm, res);
//        			resultsList.intersect(res);
//        		}
//        	}
//        	Set<Integer> docsToRemove = new HashSet<Integer>();
//        	for (Integer docID: resultsList.getDocIDs()) {
//        		boolean toAdd = false;
//        		String startToken = queryTerms.get(0);
//        		PostingsEntry pe = resultsMap.get(startToken).get(docID); // all occurrences of a query word in a document
//        		for (int i = 0; i < pe.offsets.size() && !toAdd; i++) {
//        			Integer startOffset = pe.offsets.get(i);
//					boolean isValid = true;
//        			for (int j = 1; j < queryTerms.size() && isValid; j++) {
//        				String targetToken = queryTerms.get(j);
//                		PostingsEntry nextTermEntries = resultsMap.get(targetToken).get(docID); // all occurrences of a query word in a document
//                		isValid = nextTermEntries.offsets.contains(startOffset + j);
//        			}
//        			if (isValid) {
//        				toAdd = true;
//        			}
//        		}
//        		if (!toAdd) {
//        			docsToRemove.add(docID);
//        		}
//        	}
//        	for (Integer docID: docsToRemove) {
//    			resultsList.removeDocId(docID);
//        	}
//        	return resultsList;
    		return null;
    	} else {
    		PostingsList retList = index.get(queryTerms.get(0));
        	// TreeMap's keys will be returned in sorted order        	
        	for (int queryTermIndex = 1; queryTermIndex < queryTerms.size(); queryTermIndex++) {
        		PostingsList tempList = new PostingsList();
        		Iterator<Integer> firIter = retList.docIDtoPostings.keySet().iterator();
        		
        		int hold = firIter.next().intValue();
        		do {
        			Integer temp = firIter.next().intValue();
        			if (temp <= hold) {
        				System.exit(1);
        			}
        			hold = temp;
        		} while (firIter.hasNext());
        		
        		firIter = retList.docIDtoPostings.keySet().iterator();
        		
            	PostingsList secList = index.get(queryTerms.get(queryTermIndex));
            	Iterator<Integer> secIter = secList.docIDtoPostings.keySet().iterator();
            	
            	hold = secIter.next().intValue();
        		do {
        			Integer temp = secIter.next().intValue();
        			if (temp <= hold) {
        				System.exit(1);
        			}
        			hold = temp;
        		} while (secIter.hasNext());
        		
        		
        		secIter = secList.docIDtoPostings.keySet().iterator();
            	
            	if (firIter.hasNext() && secIter.hasNext()) {
            		int firDocId = firIter.next().intValue();
            		int secDocId = secIter.next().intValue();
                	while (firIter.hasNext() && secIter.hasNext()) {
                		while (firDocId > secDocId) {
                			// look for the firDocId in secList, stopping when we are on or past it
                			secDocId = secIter.next().intValue();
                		}
                		
                		if (secDocId == firDocId) {
                			// found in second list
                			if (queryTermIndex == 1) {
                				// due to fence post solution, during the first comparison,
                				// we want to add the positions from the first list we use to compare
                				// before we have the first result from our intersection
                    			tempList.putList(firDocId, retList.get(firDocId));
                			}
                			tempList.putList(secDocId, secList.get(secDocId));
                		}
                		
                		while (secDocId >=  firDocId) {
                			// we move on as far as we can to catch up if we need to
                			firDocId = firIter.next().intValue();
                		}                		
                	}
                	retList = tempList;
            	} else {
            		return new PostingsList();
            	}

        	}
        	return retList;
    	}    	
    }


    /**
     *  No need for cleanup in a HashedIndex.
     */
    public void cleanup() {
    }
}
