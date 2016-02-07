/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 */  

package ir;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {
    
    public int docID;
    public List<Integer> offsets;
    public double score;
    
    public PostingsEntry(int docID) {
    	this.docID = docID;
    	this.offsets = new LinkedList<Integer>();
    	this.score = 0;
    }

    /**
     *  PostingsEntries are compared by their score (only relevant 
     *  in ranked retrieval).
     *
     *  The comparison is defined so that entries will be put in 
     *  descending order.
     */
    public int compareTo( PostingsEntry other ) {
    	return Double.compare( other.score, score );
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + docID;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PostingsEntry other = (PostingsEntry) obj;
		if (docID != other.docID)
			return false;
		return true;
	}

	public void addOffset(int offset) {
		offsets.add(offset);
	}
    

    //
    //  YOUR CODE HERE
    //

}

    
