/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 */  

package ir;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 *   A list of postings for a given word.
 */
public class PostingsList implements Serializable {
    
    /** The postings list as a linked list. */
    private HashMap<Integer, PostingsEntry> docIDtoPostings;

    public PostingsList() {
    	docIDtoPostings = new HashMap<Integer, PostingsEntry>();
    }
    
    private PostingsList(HashMap<Integer, PostingsEntry> backingList) {
    	docIDtoPostings = backingList;
    }
    
    /**  Number of postings in this list  */
    public int size() {
    	return docIDtoPostings.size();
    }

    /**  Returns an iterator over the PostingsEntrys */
    public Iterator<PostingsEntry> getIterator() {
    	return docIDtoPostings.values().iterator();
    }
    
    /**  Returns the docIDs of postings */
    public Set<Integer> getDocIDs() {
    	return docIDtoPostings.keySet();
    }
    
    /**  Adds a PostingsEntry for a token */
    public void put(int docID, int offset) {
    	if (!docIDtoPostings.containsKey(docID)) {
        	docIDtoPostings.put(docID, new PostingsEntry(docID));
    	}
    	docIDtoPostings.get(docID).addOffset(offset);
    }
    
    /** Gets the PostingsEntry for a docID */
    public PostingsEntry get(int docID) {
    	return docIDtoPostings.get(docID);
    }
    
    /** Removes a docID */
    public void removeDocId(int docID) {
    	docIDtoPostings.remove(docID);
    }
    
    /** Intersects two PostingsLists, use a clone unless you want keys removed permanently */
    public void intersect(PostingsList other) {
    	if (other != null)
    		docIDtoPostings.keySet().retainAll(other.docIDtoPostings.keySet());
    	else 
    		docIDtoPostings.clear();
    }
    
    /** Clones a PostingsList as not to disrupt the current dictionary */ 
    public PostingsList clone() {
    	return new PostingsList((HashMap<Integer, PostingsEntry>)docIDtoPostings.clone());
    }
    
    //
    //  YOUR CODE HERE
    //
}
	

			   
