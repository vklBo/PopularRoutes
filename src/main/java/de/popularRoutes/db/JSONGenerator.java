package de.popularRoutes.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JSONGenerator {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private RouterDBInterface dbInterface;

	public static void main(String[] args) {
		JSONGenerator jg = new JSONGenerator();

		final long timeStart = System.currentTimeMillis();
		// jg.updateJsonLinestrings();
		// String json = jg.createJson(7.717895507812499, 8.265838623046875,
		// 51.16556659836182, 52.17612705252836, 3, 9);

		String json = jg.getJSONWeights();

		final long timeEnd = System.currentTimeMillis();

		System.out.println(json);
		System.out.println("Verlaufszeit der Schleife: " + (timeEnd - timeStart) + " Millisek.");
	}

	public JSONGenerator() {
		dbInterface = new RouterDBInterface();
	}

	// Füllt die JS
	public void updateJsonLinestrings() throws ClassNotFoundException, SQLException {
		Statement stmt;
		ResultSet result;
		String sql;

		stmt = dbInterface.getCon().createStatement();
		sql = "Select id from " + RouterDBInterface.way_table + " where linestring is NULL";
		result = stmt.executeQuery(sql);

		while (result.next()) {
			String edgeID = result.getString(1);

			addJSONLinestring(edgeID);
		}

		stmt.close();
		dbInterface.getCon().close();
	}

	private void addJSONLinestring(String way_id) {
		PreparedStatement stmt;
		ResultSet result;
		String sql;
		String linestring = null;

		try {
			// Mögliche Verbesserung: die richtigen POSTGIS-Datenstrukturen
			// verwenden, dann kann ohne Umwandung kopiert werden
			sql = "SELECT ST_AsText(linestring) from " + RouterDBInterface.osm_way_table + " where id = ?";

			stmt = dbInterface.getCon().prepareStatement(sql);
			stmt.setLong(1, Long.parseLong(way_id));

			result = stmt.executeQuery();

			while (result.next()) { // Es sollte nur einen geben :-)
				linestring = result.getString(1);
			}

			sql = "UPDATE " + RouterDBInterface.way_table + " SET linestring = ST_GeometryFromText(?) where id = ?";
			stmt = dbInterface.getCon().prepareStatement(sql);
			stmt.setString(1, linestring); // jsonMultilineString.toString());
			stmt.setLong(2, Long.parseLong(way_id));
			stmt.execute();

		} catch (Exception e) {
			logger.error("Fehler beim Update der Kante " + way_id, e);
		}
	}

	public String createJson(double minLon, double maxLon, double minLat, double maxLat, int category, int zoomlevel) {
		StringBuffer jsonString = new StringBuffer("[{ \"type\": \"MultiLineString\", \"coordinates\": [ \n");
		PreparedStatement stmt = null;
		ResultSet result;
		String sql;
		boolean first = true;

		try {
			// Die genaue Berechnung der precision sollte ich mich nir nochmal
			// anschauen.
			double precision = Math.pow(10, -zoomlevel / 3.5);
			sql = "select ST_AsGeoJSON(ST_SIMPLIFY(linestring, ?, true)) FROM " + RouterDBInterface.way_table
					+ " where ST_SetSRID(ST_MakeBox2D(ST_MakePoint(?, ?), "
			//		+ " ST_MakePoint(?, ?)), 4236) && bbox and category = ?";
			//		+ " ST_MakePoint(?, ?)), 4236) && bbox and round(new_score * 7) = ?";
					+ " ST_MakePoint(?, ?)), 4236) && bbox and q_score = ?";
			

			stmt = dbInterface.getCon().prepareStatement(sql);
			stmt.setDouble(1, precision);
			stmt.setDouble(2, minLon);
			stmt.setDouble(3, minLat);
			stmt.setDouble(4, maxLon);
			stmt.setDouble(5, maxLat);
			stmt.setInt(6, category);

			result = stmt.executeQuery();

			while (result.next()) {
				String jsonLinestring = result.getString(1);
				// logger.info(jsonLinestring);
				if (!first)
					jsonString.append(',');
				jsonString.append(jsonLinestring.substring(35, jsonLinestring.length() - 1)).append('\n');
				first = false;
			}
		} catch (ClassNotFoundException | SQLException e) {
			logger.error("Fehler beim JSON-Generieren", e);
		} finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					logger.error("Fehler beim JSON-Generieren: Schließen des Stmt", e);
				}
			}

			try {
				dbInterface.getCon().close();
			} catch (ClassNotFoundException | SQLException e) {
				// TODO Auto-generated catch block
				logger.error("Fehler beim JSON-Generieren: Schließen der Connection", e);

			}
		}

		jsonString.append("]}]");
		return jsonString.toString();
	}

	public String getJSONWeights() {
		String sql = "select ST_X(pos), ST_Y(pos), num_ways, max_score, sum_score, stddev, var, quantile from pr_weights where num_ways>0";
		Statement stmt = null;
		ResultSet result;
		StringBuffer json = new StringBuffer("[");
		boolean first = true;

		try {
			stmt = dbInterface.getCon().createStatement();
			result = stmt.executeQuery(sql);

			while (result.next()) {
				if (!first) {
					json.append(", ");
				}
				first = false;

				json.append("{");
				json.append("\"lon\": " + result.getDouble(1) + ", ");
				json.append("\"lat\": " + result.getDouble(2) + ", ");
				json.append("\"num_ways\": " + result.getInt(3) + ", ");
				json.append("\"max_score\": " + result.getInt(4) + ", ");
				json.append("\"sum_score\": " + result.getInt(5) + ", ");
				json.append("\"stddev\": " + result.getDouble(6) + ", ");
				json.append("\"varianz\": " + result.getDouble(7) + ", ");
				json.append("\"quantil\": \"" + result.getArray(8) + "\"");
				json.append('}');
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				dbInterface.getCon().close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		json.append(']');
		return json.toString();
	}
}
