package de.popularRoutes.db;

import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import de.popularRoutes.RouteType;
import gnu.trove.set.hash.TLongHashSet;

public class RouterDBInterface {
	private String server = "pav02u";
	// private String database = "popularRouting";
	private String database = "pgsnapshot";
	private String user = "vk";
	private String password = "vk";
	public static String osm_way_table = "ways";
	public static String way_table = "pr_ways";

	private Connection con = null;

	public static void main(String[] args) {
		SimpleDateFormat formatter = new SimpleDateFormat(
                "dd.MM.yyyy - HH:mm:ss ");

        System.out.println("Start: Weighttable füllen: " + formatter.format(new Date()));
		new RouterDBInterface().fillWeightTable();
        System.out.println("Ende: Weighttable füllen: " + formatter.format(new Date()));
        System.out.println("Start: Kantenpopularität berechnen: " + formatter.format(new Date()));
		new RouterDBInterface().scoreWaysRelativesQuantil();
        System.out.println("Ende: Kantenpopularität berechnen: " + formatter.format(new Date()));
		//new RouterDBInterface().scoreWays();
	}

	public Connection getCon() throws ClassNotFoundException, SQLException {
		if (con == null) {
			Class.forName("org.postgresql.Driver");
			String connString = "jdbc:postgresql://" + server + ":5432/" + database;
			con = DriverManager.getConnection(connString, user, password);
		}
		return con;
	}

	public void updateEdges(Set<Long> edges, RouteType routeType) throws SQLException, ClassNotFoundException {
		Statement upsertStmt;
		String sql;

		Iterator<Long> iterator = edges.iterator();
		upsertStmt = getCon().createStatement();

		while (iterator.hasNext()) {
			Long edgeID = iterator.next();

			// UPSERT-Befehl: Versuch des INSERTS, wenn Datensatz schon
			// vorhanden ist, wird upgedated
			sql = String.format(
					"INSERT INTO %s Values (%d, %d, %d) on CONFLICT(id,type) DO  update set score=%s.score+1",
					way_table, edgeID, 1, routeType.getType(), way_table);
			upsertStmt.addBatch(sql);
		}

		upsertStmt.executeBatch();
		upsertStmt.close();
	}

	public void close() {
		try {
			if (con != null)
				con.close();
			con = null;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void fillWeightTable() {
		String selectSql = "SELECT count(score), max(score), sum(score), stddev_samp(score), var_samp(score), "
				+ "percentile_disc(array[0.1/0.7,0.2/0.7,0.3/0.7,0.4/0.7,0.5/0.7,0.6/0.7,1]) WITHIN GROUP (ORDER BY score) "
				+ "from pr_ways " + "where  ST_MakeBox2D(ST_Point(?, ?), ST_Point(?, ?)) && linestring";

		String insertStr = "INSERT INTO pr_weights (id, pos, num_ways, max_score, sum_score, stddev, var, quantile) values "
				+ "(?, ST_SetSRID(ST_MakePoint(?, ?), 4326), ?, ?, ?, ?, ?, ?)";

		PreparedStatement selectStmt = null;
		PreparedStatement insertStmt = null;
		ResultSet rs;

		long id = 1;
		double lat, lon;

		try {
			for (lat = 32; lat <= 72; lat += 0.5) { // Zypern bis Nordkapp
				for (lon = -25; lon <= 47; lon += 0.5) { // Island bis Ukraine
					++id;
					selectStmt = getCon().prepareStatement(selectSql);
					selectStmt.setDouble(1, lon - 0.25);
					selectStmt.setDouble(2, lat - 0.25);
					selectStmt.setDouble(3, lon + 0.25);
					selectStmt.setDouble(4, lat + 0.25);

					rs = selectStmt.executeQuery();

					if (rs.next()) {
						if (rs.getLong(1) > 0){
							insertStmt = getCon().prepareStatement(insertStr);
							insertStmt.setLong(1, id);
							insertStmt.setDouble(2, lon);
							insertStmt.setDouble(3, lat);
							insertStmt.setLong(4, rs.getLong(1));
							insertStmt.setLong(5, rs.getLong(2));
							insertStmt.setLong(6, rs.getLong(3));
							insertStmt.setDouble(7, rs.getDouble(4));
							insertStmt.setDouble(8, rs.getDouble(5));
							insertStmt.setArray(9, rs.getArray(6));
	
							insertStmt.execute();
						}
					}
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (selectStmt != null) {
					selectStmt.close();
				}
				if (insertStmt != null) {
					insertStmt.close();
				}

			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void insertTrack(String linestring, RouteType routeType, int file_id) {
		PreparedStatement stmt = null;
		String sql;
		long id = 0;

		try {
			sql = "insert into pr_tracks (type, linestring) values (?, ST_GeomFromText(?, 4326))";

			stmt = getCon().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			stmt.setInt(1, new Integer(routeType.getType()));
			stmt.setString(2, linestring);

			stmt.execute();

			ResultSet rs = stmt.getGeneratedKeys();
			if (rs.next()) {
				id = rs.getLong(1);
				stmt.close(); // TODO
				sql = "insert into pr_filestracks (trackid, fileid, type) values (?, ?, ?)";
				stmt = getCon().prepareStatement(sql);
				stmt.setLong(1, id);
				stmt.setInt(2, file_id);
				stmt.setInt(3, new Integer(routeType.getType()));
				stmt.execute();
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public int insertFile(String filename, RouteType routeType) {
		PreparedStatement stmt = null;
		String sql;
		int id = 0;

		try {
			sql = "insert into pr_files (type, filename) values (?, ?)";

			stmt = getCon().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			stmt.setInt(1, new Integer(routeType.getType()));
			stmt.setString(2, filename);

			stmt.execute();

			ResultSet rs = stmt.getGeneratedKeys();
			if (rs.next()) {
				id = rs.getInt(1);
				return id;
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return 0;
	}

	public ResultSet readUnloadedTracks(RouteType routeType) {
		PreparedStatement stmt = null;
		String sql;
		ResultSet rs = null;

		try {
			sql = "select id, ST_AsText(linestring),  ST_AsText(ST_SIMPLIFY(linestring, ?, true)), ST_AsText(ST_SIMPLIFY(linestring, ?, true)), ST_AsText(ST_SIMPLIFY(linestring, ?, true)) from pr_tracks where type = ? and not loaded";

			stmt = getCon().prepareStatement(sql);
			stmt.setDouble(1, 0.000001);
			stmt.setDouble(2, 0.0000001);
			stmt.setDouble(3, 0.00000001);
			stmt.setLong(4, routeType.getType());

			rs = stmt.executeQuery();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rs;
	}

	public void saveEdgesAndTimes(long trackId, TLongHashSet edges, long time, TLongHashSet edges2, long time2,
			TLongHashSet edges3, long time3, TLongHashSet edges4, long time4) {
		PreparedStatement stmt = null;
		String sql;

		try {
			sql = "insert into pr_testImport (trackid, edges, time, edges2, time2, edges3, time3, edges4, time4) "
					+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?)";

			stmt = getCon().prepareStatement(sql);
			stmt.setLong(1, trackId);
			stmt.setString(2, edges.toString());
			stmt.setLong(3, time);
			stmt.setString(4, edges2.toString());
			stmt.setLong(5, time2);
			stmt.setString(6, edges3.toString());
			stmt.setLong(7, time3);
			stmt.setString(8, edges4.toString());
			stmt.setLong(9, time4);

			stmt.execute();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void scoreWaysRelativesQuantil() {
		final int ANZ_QUANTILE = 7;
		
		String selectSql = "SELECT id, ST_X(ST_Centroid(linestring)) as lon, ST_Y(ST_Centroid(linestring)) as lat, score "
				+ "from pr_ways where q_score is null and bbox is not null";

		String selectNearestSQL = "SELECT ST_Distance_Sphere(pos, ST_SetSRID(ST_MakePoint(?, ?), 4326)), quantile "
				+ "from pr_weights where pos && ST_SetSRID(ST_MakeBox2D(ST_MakePoint(?, ?), ST_MakePoint(?, ?)), 4326)";

		String updateSql = "update pr_ways set q_score = ? where id = ?";

		PreparedStatement selectStmt = null;
		PreparedStatement selectNearestStmt = null;
		PreparedStatement updateStmt = null;
		ResultSet rs = null;
		ResultSet rsNearest = null;

		long id;
		double lat, lon;
		int score;
		int new_score;
		double dist[] = new double[4];
		double summeDist;
		Integer quantile[][] = new Integer[4][];
		int i, j, anz;
		double grenze;

		
		try {
			selectStmt = getCon().prepareStatement(selectSql);

			rs = selectStmt.executeQuery();

			while (rs.next()) {
				id = rs.getLong(1);
				lon = rs.getDouble(2);
				lat = rs.getDouble(3);
				score = rs.getInt(4);
				
				new_score = 0;

				selectNearestStmt = getCon().prepareStatement(selectNearestSQL);
				selectNearestStmt.setDouble(1, lon);
				selectNearestStmt.setDouble(2, lat);
				selectNearestStmt.setDouble(3, lon - 0.5);
				selectNearestStmt.setDouble(4, lat - 0.5);
				selectNearestStmt.setDouble(5, lon + 0.5);
				selectNearestStmt.setDouble(6, lat + 0.5);
				rsNearest = selectNearestStmt.executeQuery();

				i = 0;
				summeDist = 0;
				while (rsNearest.next() && i < 4) {
					Array aktQuantile = rsNearest.getArray(2);
					if (aktQuantile != null){
						dist[i] = 1/rsNearest.getDouble(1);
						summeDist += dist[i];
						quantile[i++] = (Integer []) aktQuantile.getArray();
					}
				}
				anz = i;

				// Es kann vorkommen, dass der Score nie kleiner als die Grenze wird, daher wird als default der Score auf den max-Score gesetzt. 
				new_score = ANZ_QUANTILE;

				// Durch alle Quantile laufen und prüfen, wann das erste nicht mehr überschritten ist
				for (j = 0; j < ANZ_QUANTILE; ++j){
					grenze = 0;
					for (i = 0; i < anz; ++i){
						grenze += quantile[i][j] * (dist[i] / summeDist);
					}
					// Für den Fall, dass eins oder mehr Umgebungsquadranten keine gewichteten Wege besitzt
					for (i = anz; i < 4; ++i){
						grenze += 1 * (dist[i] /summeDist);
					}
					if (score < grenze){
						// Wenn die Häufigkeit der Kante nicht mehr ausreicht, wird das aktuelle Quantil als Score ausgewählt.
						// Da der Score von 1 bis 7 gezählt wird, passt es, obwohl j eigentlich bereits ein Quantil weiter ist.
						new_score = j;
						break;
					}
				}
				
				updateStmt = getCon().prepareStatement(updateSql);
				updateStmt.setInt(1, new_score);
				updateStmt.setLong(2, id);
				updateStmt.execute();
			}

		} catch (ClassNotFoundException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				selectStmt.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				selectNearestStmt.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				updateStmt.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				getCon().close();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
}
