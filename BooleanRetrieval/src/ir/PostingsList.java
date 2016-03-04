/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 */  

package ir;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;


/**
 *   A list of postings for a given word.
 */
public class PostingsList implements Serializable {
    
    /** The postings list as a linked list. */
    TreeMap<Integer, PostingsEntry> docIDtoPostings;

    public PostingsList() {
    	docIDtoPostings = new TreeMap<Integer, PostingsEntry>();
    }
    
    private PostingsList(TreeMap<Integer, PostingsEntry> backingList) {
    	docIDtoPostings = backingList;
    }
    
    /**  Number of docIds (keys) in this tree map */
    public int docOccurrenceSize() {
    	return docIDtoPostings.size();
    }
    
    /**  Number of occurrences of this word */
    public int size() {
    	return docIDtoPostings.values().size();
    }

    /**  Returns an iterator over the PostingsEntrys */
    public Iterator<PostingsEntry> getIterator() {
    	return docIDtoPostings.values().iterator();
    }
    
    /**  Returns an iterator over the PostingsEntrys */
    public Iterator<PostingsEntry> getScoreSortedIterator() {
    	PostingsEntry[] entries = new PostingsEntry[size()];
    	docIDtoPostings.values().toArray(entries);
        Arrays.sort(entries, new PostingsEntryScoreComparator());
        List<PostingsEntry> entriesList = Arrays.asList(entries);
        return entriesList.iterator();
    }
    
    /**  Returns the docIDs of postings */
    public Set<Integer> getDocIDs() {
    	return docIDtoPostings.keySet();
    }
    
    /**  Adds a PostingsEntry for a token without adding an offset */
    public void put(int docID) {
    	if (!docIDtoPostings.containsKey(docID)) {
        	docIDtoPostings.put(docID, new PostingsEntry(docID));
    	}
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
    	return new PostingsList((TreeMap<Integer, PostingsEntry>)docIDtoPostings.clone());
    }

    /**  Adds a PostingsEntry for a token */
    public void putList(int docID, PostingsEntry newList) {
    	if (!docIDtoPostings.containsKey(docID)) {
    		docIDtoPostings.put(docID, newList.clone());
    	} else {
    		docIDtoPostings.get(docID).addList(newList);
    	}    	
    }

	public boolean isEmpty() {
		return docIDtoPostings.isEmpty();
	}
    
    public boolean containsDocId(int docID) {
    	return docIDtoPostings.containsKey(docID);
    }
    
    public class PostingsEntryScoreComparator implements Comparator<PostingsEntry> {
        @Override
        public int compare(PostingsEntry fir, PostingsEntry sec) {
            return (new Double(sec.score)).compareTo(new Double(fir.score));
        }
    }
    
}
	

			   
