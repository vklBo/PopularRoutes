package de.popularRoutes.gpx;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.alternativevision.gpx.beans.Route;
import org.alternativevision.gpx.beans.Track;
import org.alternativevision.gpx.beans.Waypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.graphhopper.util.CmdArgs;

import de.popularRoutes.PopRouter;
import de.popularRoutes.RouteType;
import de.popularRoutes.db.RouterDBInterface;

public class ParseAndImportGPXTrack {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	// Der Aufruf sollte mit den config="..../import.properties" erfolgen.
	// Ausserdem muss der Parameter gpxPath gegeben sein.

	private static final long OUT_OF_BOUNDS = 1;
	private static final long TO_SHORT = 2;
	private static final double MIN_DIAMETER = 10;

	public static void main(String[] args) {
		new ParseAndImportGPXTrack().parseAndImport(args);
	}

	public void parseAndImport(String[] args) {
		CmdArgs cmdArgs = CmdArgs.read(args);
		String gpxPath;
		String gpxfile;

		gpxPath = cmdArgs.get("gpxpath", "/pathToGpxFiles");
		if (gpxPath.charAt(gpxPath.length() - 1) != '/')
			gpxPath = gpxPath + "/";

		logger.info("Starte Import: " + gpxPath);
		File dir = new File(gpxPath);
		if (! (dir.exists() && dir.isDirectory())){
			throw new RuntimeException("Directory " + gpxPath + " does not exist.");
		}
		File dir_loaded = new File(gpxPath + "loaded/");
		File dir_outOfBounds = new File(gpxPath + "outOfBound/");
		File dir_error = new File(gpxPath + "error/");
		File[] fileArray = dir.listFiles();
		dir_loaded.mkdir();
		dir_outOfBounds.mkdir();
		dir_error.mkdir();

		RouteType routeType = new RouteType(RouteType.MOTORCYCLE);
		PopRouter router = new PopRouter(cmdArgs, routeType, true);

		for (int i = 0; i < fileArray.length; ++i) {
			File f = fileArray[i];
			if (f.isFile() && (f.getName().endsWith(".gpx") || f.getName().endsWith(".GPX"))) {
				gpxfile = f.getAbsolutePath();
				try {
					long result = parseFile(router, routeType, gpxfile);
					if (result == 0) {
						logger.info("Geladene Datei " + gpxfile);
						f.renameTo(new File(dir_loaded.getAbsolutePath() + "/" + f.getName()));
					} else if (result == OUT_OF_BOUNDS) {
						logger.info("Datei out of Bounds " + gpxfile);
						f.renameTo(new File(dir_outOfBounds.getAbsolutePath() + "/" + f.getName()));
					} else if (result == TO_SHORT) {
						logger.error("Track zu kurz " + gpxfile);
						f.renameTo(new File(dir_error.getAbsolutePath() + "/short_" + f.getName()));
					}

				} catch (Exception e) {
					logger.error("Fehler bei " + gpxfile, e);
					f.renameTo(new File(dir_error.getAbsolutePath() + "/" + f.getName()));
				}
			}
		}

		logger.info("Fertig");
	}

	public long parseFile(PopRouter router, RouteType routeType, String gpxfile)
			throws ParserConfigurationException, SAXException, IOException, ClassNotFoundException, SQLException {
		Set<Long> edges;
		Iterator<Waypoint> wayPointIterator;
		RouterDBInterface dbInterface = new RouterDBInterface();
		HashSet<Track> trackSet;
		HashSet<Route> routeSet;
		HashSet<Waypoint> waySet;
		long returnvalue = 0;

		GpxTrackParser trackParser = new GpxTrackParser(gpxfile);

		trackSet = trackParser.getTracks() != null ? trackParser.getTracks() : new HashSet<Track>();
		routeSet = trackParser.getRoutes() != null ? trackParser.getRoutes() : new HashSet<Route>();
		waySet = trackParser.getWaypoints() != null ? trackParser.getWaypoints() : new HashSet<Waypoint>();

		logger.info(String.format("%s: %f km Diameter, %d Tracks, %d Routen, %d Wegpunkte in der Datei gefunden",
				gpxfile, trackParser.getDiameter(), trackSet.size(), routeSet.size(), waySet.size()));

		if (trackParser.getDiameter() < MIN_DIAMETER)
			return TO_SHORT;

		Iterator<Track> trackIterator = trackSet.iterator();
		Iterator<Route> routeIterator = routeSet.iterator();

		while (trackIterator.hasNext()) {
			ArrayList<Waypoint> tp = trackIterator.next().getTrackPoints();
			wayPointIterator = tp.iterator();
			edges = new HashSet<Long>();
			Waypoint from = wayPointIterator.next();
			Waypoint to;
			while (wayPointIterator.hasNext()) {
				to = wayPointIterator.next();
				router.collectEdgesOfRoute(edges, from, to);

				from = to;
			}
			logger.info(String.format("%s: %d Trackpoints, Gefundene Kanten: %d", gpxfile, tp.size(), edges.size()));
			if (edges.size() == 0)
				return OUT_OF_BOUNDS;
			logger.info(String.format("Datei: %s Distanz: %f", gpxfile, trackParser.getDiameter()));
			if (trackParser.getDiameter() < 10)
				return TO_SHORT;
			dbInterface.updateEdges(edges, routeType);
		}

		// Wenn die Anzahl von Tracks identisch der Anzahl Routen sind, dann
		// werden vermutlich dieselben Strecken beschrieben
		if (trackSet.size() != routeSet.size()) {
			while (routeIterator.hasNext()) {
				ArrayList<Waypoint> tp = routeIterator.next().getRoutePoints();
				wayPointIterator = tp.iterator();
				edges = new HashSet<Long>();
				Waypoint from = wayPointIterator.next();
				Waypoint to;
				while (wayPointIterator.hasNext()) {
					to = wayPointIterator.next();
					router.collectEdgesOfRoute(edges, from, to);

					from = to;
				}
				logger.info(
						String.format("%s: %d Routepoints, Gefundene Kanten: %d", gpxfile, tp.size(), edges.size()));
				if (edges.size() == 0)
					return OUT_OF_BOUNDS;
				dbInterface.updateEdges(edges, routeType);
			}
		}

		wayPointIterator = waySet.iterator();
		edges = new HashSet<Long>();
		// Waypoints werden nur interpretiert, wenn weder Routen noch Tracks in
		// der Datei sind
		if (trackSet.size() + routeSet.size() == 0 && wayPointIterator.hasNext()) {
			Waypoint from = wayPointIterator.next();
			Waypoint to;
			while (wayPointIterator.hasNext()) {
				to = wayPointIterator.next();
				router.collectEdgesOfRoute(edges, from, to);

				from = to;
			}
			logger.info(String.format("%s: %d Waypoints, Gefundene Kanten: %d", gpxfile, waySet.size(), edges.size()));
			if (edges.size() == 0)
				return OUT_OF_BOUNDS;
			dbInterface.updateEdges(edges, routeType);
		}

		dbInterface.close();
		return returnvalue;
	}

}
