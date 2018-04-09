package de.popularRoutes.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gnu.trove.map.hash.TLongLongHashMap;

public class PopWaysGenerator {
	public RouterDBInterface dbInterface;
	private final Logger logger = LoggerFactory.getLogger(getClass()); 

	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
		new PopWaysGenerator().getPopularWays("/home/vk/karten/pop_ways.ser");
	}

	public PopWaysGenerator() {
		super();
		dbInterface = new RouterDBInterface();
	}

	
	public TLongLongHashMap getPopularWays(String filename) throws ClassNotFoundException, SQLException, IOException {
		File popWaysFile = new File(filename);
		TLongLongHashMap popularWaysMap;
		
		if (! popWaysFile.exists())	{
			popularWaysMap = getPopularWaysFromDB();
			this.writePopularWaysToFile(filename, popularWaysMap);
			return popularWaysMap;
		}
		else		{
			return readPopularWaysFromFile( filename);
		}
	}
	
	
	protected TLongLongHashMap getPopularWaysFromDB() throws ClassNotFoundException, SQLException  {
		TLongLongHashMap popularWaysMap = new TLongLongHashMap();
		Statement stmt;
		ResultSet result;
		String sql;

		stmt = dbInterface.getCon().createStatement();
		sql = "Select id, q_score, category from " + RouterDBInterface.way_table;
		result = stmt.executeQuery(sql);

		while (result.next()) {
			long edgeID = result.getLong(1);
			int score = result.getInt(2);
			long category = result.getLong(3);

			category = score;
			
			popularWaysMap.put(edgeID, category);
		}

		stmt.close();
		logger.info("Popular Ways read from database");
		return popularWaysMap;
	}

	protected TLongLongHashMap getPopularWaysFromDBAlt() throws ClassNotFoundException, SQLException  {
		TLongLongHashMap popularWaysMap = new TLongLongHashMap();
		Statement stmt;
		ResultSet result;
		String sql;

		stmt = dbInterface.getCon().createStatement();
		sql = "Select id, new_score, category from " + RouterDBInterface.way_table;
		result = stmt.executeQuery(sql);

		while (result.next()) {
			long edgeID = result.getLong(1);
			double score = result.getDouble(2);
			long category = result.getLong(3);

			category = Math.round(score * 7);
			
			popularWaysMap.put(edgeID, category);
		}

		stmt.close();
		logger.info("Popular Ways read from database");
		return popularWaysMap;
	}

	protected TLongLongHashMap readPopularWaysFromFile(String filename)
			throws ClassNotFoundException, SQLException, IOException {
		InputStream fis = null;
		ObjectInputStream i = null;

		TLongLongHashMap popularWaysMap;

		fis = new FileInputStream(filename);
		i = new ObjectInputStream(fis);

		popularWaysMap = (TLongLongHashMap) i.readObject();
		fis.close();
		
		logger.info("Popular Ways read from file: " + filename);
		return popularWaysMap;
	}

	protected void writePopularWaysToFile(String filename, TLongLongHashMap popularWaysMap) throws IOException  {
		OutputStream fos = null;
		ObjectOutputStream o = null;

		fos = new FileOutputStream(filename);
		o = new ObjectOutputStream(fos);

		o.writeObject(popularWaysMap);
		fos.close();
		logger.info("Popular Ways write to file: " + filename);
	}

}
