package ir_disk;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class DiskHashedIndex implements Index {
	
	Connection conn;
	final int BATCH_LIMIT = 50000;
	int currentBatchSize = 0;
		
	private PreparedStatement insertEntryBatchStatement;
	
	private static final String INSERT_BATCH_ENTRY_SQL =
			"INSERT INTO Entry VALUES(?, ?, ?)";
	
	private PreparedStatement selectEntryByTokenStatement;
	
	private static final String SELECT_BY_TOKEN_ENTRY_SQL =
			"SELECT * from Entry WHERE TOKEN = ?";
	
	private PreparedStatement insertDocIDNameStatement;
	
	private static final String INSERT_BATCH_DOCID_NAME_SQL =
			"INSERT INTO DocIDtoName VALUES(?, ?)";
	
	private PreparedStatement selectDocIDNameStatement;
	
	private static final String SELECT_DOCID_NAME_SQL =
			"SELECT * from DocIDtoName WHERE DocID = ?";

	private PreparedStatement deleteDocIDNameStatement;
	
	private static final String DELETE_DOCID_NAME_SQL =
			"DELETE from DocIDtoName";
	
	private PreparedStatement deleteEntryStatement;
	
	private static final String DELETE_ENTRY_SQL =
			"DELETE from Entry";
	
	/**
	 * Uses the tables Entry and DocIDtoName, which you will need to create.
	 * Schemas:
	 * Entry(Token VARCHAR(50), DOCID INT, POSITION INT)
	 * DocIDtoName(DocID INT PRIMARY KEY, Filename VARCHAR(100))
	 * 
	 * Index:
	 * create index token_idx on Entry(TOKEN);
	 */
	public DiskHashedIndex() {
	    Connection c = null;
	    try {
	      Class.forName("org.sqlite.JDBC");
	      conn = DriverManager.getConnection("jdbc:sqlite:index.db");
	      conn.setAutoCommit(false);
	      prepareStatements();
	    } catch ( Exception e ) {
	      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
	      System.exit(0);
	    }
	}
	
	
	private void prepareStatements() throws Exception {
		insertEntryBatchStatement = conn.prepareStatement(INSERT_BATCH_ENTRY_SQL);
		selectEntryByTokenStatement = conn.prepareStatement(SELECT_BY_TOKEN_ENTRY_SQL);
		insertDocIDNameStatement = conn.prepareStatement(INSERT_BATCH_DOCID_NAME_SQL);
		selectDocIDNameStatement = conn.prepareStatement(SELECT_DOCID_NAME_SQL);
		deleteDocIDNameStatement = conn.prepareStatement(DELETE_DOCID_NAME_SQL);
		deleteEntryStatement = conn.prepareStatement(DELETE_ENTRY_SQL);
	}
	
	@Override
	public void insertDocIdMapping(int docID, String filename) throws SQLException {
		insertDocIDNameStatement.setInt(1, docID);
		insertDocIDNameStatement.setString(2, filename);
		insertDocIDNameStatement.addBatch();
	}
	
	@Override
	public void insert(String token, int docID, int offset) throws SQLException {
		insertEntryBatchStatement.setString(1, token);
		insertEntryBatchStatement.setInt(2, docID);
		insertEntryBatchStatement.setInt(3, offset);
		insertEntryBatchStatement.addBatch();
		currentBatchSize++;
		if (currentBatchSize > BATCH_LIMIT) {
			insertEntryBatchStatement.executeBatch();
			currentBatchSize = 0;
		}
	}

	@Override
	public PostingsList getPostings(String token) throws SQLException {
		selectEntryByTokenStatement.clearParameters();
		selectEntryByTokenStatement.setString(1, token);
		ResultSet res = selectEntryByTokenStatement.executeQuery();
		PostingsList toReturn = new PostingsList();
		while (res.next()) {
			toReturn.put(res.getInt("DOCID"), res.getInt("POSITION"));
		}
		return toReturn;
	}

	@Override
	public PostingsList search(Query query, int queryType, int rankingType, int structureType) throws SQLException {
		LinkedList<String> queryTerms = query.getQueryTerms();
    	if (queryType == Index.PHRASE_QUERY) {
    		Map<String, PostingsList> resultsMap = new HashMap<String, PostingsList>();
        	PostingsList resultsList = getPostings(queryTerms.get(0));
        	resultsMap.put(queryTerms.get(0), resultsList);
        	for (int i = 1; i < queryTerms.size(); i++) {
        		String queryTerm = queryTerms.get(i);
        		if (!resultsMap.containsKey(queryTerm)) {
        			PostingsList res = getPostings(queryTerm);
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
        	PostingsList resultsList = getPostings(queryTerms.removeFirst());
        	for (String queryTerm: queryTerms) {
    			resultsList.intersect(getPostings(queryTerm));
        	}
        	return resultsList;
    	}    	
	}

	@Override
	public void cleanup() throws SQLException {
		conn.close();
	}

	public void clearOldData() throws SQLException {
		deleteEntryStatement.executeUpdate();
		deleteDocIDNameStatement.executeUpdate();
	}

	public void executeBatchInsert() {
		
		try {
			insertEntryBatchStatement.executeBatch();
			insertDocIDNameStatement.executeBatch();
			conn.commit();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.out.println(e.getMessage());
		}
	}


	@Override
	public String getDocName(int docID) {
		try {
			selectDocIDNameStatement.setInt(1, docID);
			ResultSet res = selectDocIDNameStatement.executeQuery();
			if (res.next()) {
				return res.getString("Filename");
			}
		} catch (SQLException e) {			
		}
		return null;
	}

}
