package de.popularRoutes.gpx;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.postgis.LineString;
import org.postgis.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.util.CmdArgs;
import com.graphhopper.util.StopWatch;

import de.popularRoutes.PopRouter;
import de.popularRoutes.RouteType;
import de.popularRoutes.db.RouterDBInterface;
import gnu.trove.set.hash.TLongHashSet;


public class ImportStoredTracks {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	// Der Aufruf sollte mit den config="..../import.properties" erfolgen.
	// Ausserdem muss der Parameter gpxPath gegeben sein.

	public static void main(String[] args) throws SQLException {
		new ImportStoredTracks().parseAndImport(args);
	}

	public void parseAndImport(String[] args) throws SQLException {
		CmdArgs cmdArgs = CmdArgs.read(args);
		RouterDBInterface dbInterface = new RouterDBInterface();
		ResultSet rs;
		LineString linestring;
		TLongHashSet edges;
		TLongHashSet edges2;
		TLongHashSet edges3;
		TLongHashSet edges4;
		long time;
		long time2;
		long time3;
		long time4;
		
		long id = 0;

		RouteType routeType = new RouteType(RouteType.MOTORCYCLE);
		PopRouter router = new PopRouter(cmdArgs, routeType, true);
		
		rs = dbInterface.readUnloadedTracks(routeType);
		
		while (rs.next()){ 
			id = rs.getLong(1);
			linestring = new LineString(rs.getString(2));
			StopWatch watch = new StopWatch().start();
			edges = this.parseTrack(router, routeType, linestring);
			time = watch.stop().getNanos();

			linestring = new LineString(rs.getString(3));
			watch.start();
			edges2 = this.parseTrack(router, routeType, linestring);
			time2 = watch.stop().getNanos();
			
			linestring = new LineString(rs.getString(4));
			watch.start();
			edges3 = this.parseTrack(router, routeType, linestring);
			time3 = watch.stop().getNanos();

			linestring = new LineString(rs.getString(5));
			watch.start();
			edges4 = this.parseTrack(router, routeType, linestring);
			time4 = watch.stop().getNanos();

			dbInterface.saveEdgesAndTimes(id, edges, time, edges2, time2, edges3, time3, edges4, time4);
		}
	}

	public TLongHashSet parseTrack(PopRouter router, RouteType routeType, LineString linestring){
		TLongHashSet edges = new TLongHashSet();
		int i = 1;
		int numberPoints;
		Point from;
		Point to;
		
		numberPoints = linestring.numPoints();
		if (numberPoints < 2)
		{
			return null;
		}
		
		from = linestring.getFirstPoint();
		
		for (i = 1; i < numberPoints; ++i){
			to = linestring.getPoint(i);
			router.collectEdgesOfRoute2(edges, from, to);

			from = to;
		}
			
		return edges;
	}

}
