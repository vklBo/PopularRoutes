package de.popularRoutes.gpx;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;

import org.alternativevision.gpx.GPXParser;
import org.alternativevision.gpx.beans.GPX;
import org.alternativevision.gpx.beans.Route;
import org.alternativevision.gpx.beans.Track;
import org.alternativevision.gpx.beans.Waypoint;
import org.xml.sax.SAXException;

public class GpxTrackParser {
	protected String gpxfile;
	protected GPX gpx;

	public GpxTrackParser(String gpxfile) throws ParserConfigurationException, SAXException, IOException {
		super();
		this.gpxfile = gpxfile;
		FileInputStream in;
		GPXParser p = new GPXParser();

		in = new FileInputStream(gpxfile);
		gpx = p.parseGPX(in);
	}

	public HashSet<Track> getTracks() {
		return gpx.getTracks();
	}

	public HashSet<Route> getRoutes() {
		return gpx.getRoutes();
	}

	public HashSet<Waypoint> getWaypoints() {
		return gpx.getWaypoints();
	}

	public double getDiameter() {
		return gpx.getDiameter();
	}

	public static void main(String[] args) {
		String gpxfile = "/home/vk/graphhopper/graphhopper-master/route.gpx";

		GPXParser p = new GPXParser();
		FileInputStream in;
		try {
			in = new FileInputStream(gpxfile);
			GPX gpx = p.parseGPX(in);
			System.out.println(String.format("%d Tracks in der Datei gefunden", gpx.getTracks().size()));
			Track track = gpx.getTracks().iterator().next();
			ArrayList<Waypoint> wps = track.getTrackPoints();
			Iterator<Waypoint> wpsIterator = wps.iterator();
			Waypoint from = wpsIterator.next();
			Waypoint to;
			while (wpsIterator.hasNext()) {
				to = wpsIterator.next();
				System.out.println(from + " -> " + to);
				// berechneteRoute zwischen from und to und ändere
				// Kantengewichte
				from = to;

			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
