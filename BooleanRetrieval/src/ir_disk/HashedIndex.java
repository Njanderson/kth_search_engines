/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 *   Additions: Hedvig Kjellström, 2012-14
 */  


package ir_disk;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;


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
    		Map<String, PostingsList> resultsMap = new HashMap<String, PostingsList>();
        	PostingsList resultsList = index.get(queryTerms.get(0)).clone();
        	resultsMap.put(queryTerms.get(0), resultsList);
        	for (int i = 1; i < queryTerms.size(); i++) {
        		String queryTerm = queryTerms.get(i);
        		if (!resultsMap.containsKey(queryTerm)) {
        			PostingsList res = index.get(queryTerm).clone();
            		resultsMap.put(queryTerm, res);
        			resultsList.intersect(res);
        		}
        	}
        	Set<Integer> docsToRemove = new HashSet<Integer>();
        	for (Integer docID: resultsList.getDocIDs()) {
        		boolean toAdd = false;
        		String startToken = queryTerms.get(0);
        		PostingsEntry pe = resultsMap.get(startToken).get(docID); // all occurrences of a query word in a document
        		for (int i = 0; i < pe.offsets.size() && !toAdd; i++) {
        			Integer startOffset = pe.offsets.get(i);
					boolean isValid = true;
        			for (int j = 1; j < queryTerms.size() && isValid; j++) {
        				String targetToken = queryTerms.get(j);
                		PostingsEntry nextTermEntries = resultsMap.get(targetToken).get(docID); // all occurrences of a query word in a document
                		isValid = nextTermEntries.offsets.contains(startOffset + j);
        			}
        			if (isValid) {
        				toAdd = true;
        			}
        		}
        		if (!toAdd) {
        			docsToRemove.add(docID);
        		}
        	}
        	for (Integer docID: docsToRemove) {
    			resultsList.removeDocId(docID);
        	}
        	return resultsList;
    	} else {
        	PostingsList resultsList = index.get(queryTerms.removeFirst()).clone();
        	for (String queryTerm: queryTerms) {
    			resultsList.intersect(index.get(queryTerm));
        	}
        	return resultsList;
    	}    	
    }


    /**
     *  No need for cleanup in a HashedIndex.
     */
    public void cleanup() {
    }


	@Override
	public String getDocName(int docID) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void insertDocIdMapping(int docID, String path) throws SQLException {
		// TODO Auto-generated method stub
		
	}
}
