package de.popularRoutes;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.alternativevision.gpx.beans.Waypoint;
import org.postgis.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.VirtualEdgeIteratorState;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.CmdArgs;
import com.graphhopper.util.EdgeIteratorState;

import de.popularRoutes.ghextensions.PopRoutesFlagEncoderFactory;
import gnu.trove.set.hash.TLongHashSet;

public class PopRouter {
	protected PopRoutesHopper hopper;
	protected RouteType routeType;
	Logger logger = LoggerFactory.getLogger(this.getClass());

	public PopRouter(CmdArgs args, RouteType routeType, boolean forDesktop) {
		super();
		this.routeType = routeType;
		hopper = new PopRoutesHopper();
		hopper.init(args);
		if (forDesktop) {
			hopper.forDesktop();
		} else {
			hopper.forServer();
		}
		hopper.setFlagEncoderFactory(new PopRoutesFlagEncoderFactory());
		hopper.setEncodingManager(new EncodingManager(routeType.getVehicle()));
		hopper.importOrLoad();
	}

	public void collectEdgesOfRoute(Set<Long> edges, Waypoint from, Waypoint to) {
		GHRequest req = new GHRequest(from.getLatitude(), from.getLongitude(), to.getLatitude(), to.getLongitude())
				.setWeighting(routeType.getWeighting()).setVehicle(routeType.getVehicle()).setLocale(Locale.GERMAN);

		GHResponse rsp = new GHResponse();

		List<Path> paths = hopper.calcPaths(req, rsp);

		if (paths.size() > 0) {
			Path path0 = paths.get(0);
			for (EdgeIteratorState edge : path0.calcEdges()) {
				int edgeId = edge.getEdge();
				if (edge instanceof VirtualEdgeIteratorState) {
					// first, via and last edges can be virtual
					VirtualEdgeIteratorState vEdge = (VirtualEdgeIteratorState) edge;
					edgeId = vEdge.getOriginalTraversalKey() / 2;
				}
				try {
					edges.add(new Long(hopper.getOSMWay(edgeId)));
				} catch (ArrayIndexOutOfBoundsException e) {
					logger.info(String.format("EdgeID %d nicht identifizieren können.", edgeId));
				}
			}
		}
	}

	public void collectEdgesOfRoute2(TLongHashSet edges, Point from, Point to) {
		GHRequest req = new GHRequest(from.getY(), from.getX(), to.getY(), to.getX())
				.setWeighting(routeType.getWeighting()).setVehicle(routeType.getVehicle()).setLocale(Locale.GERMAN);

		GHResponse rsp = new GHResponse();

		List<Path> paths = hopper.calcPaths(req, rsp);

		if (paths.size() > 0) {
			Path path0 = paths.get(0);
			for (EdgeIteratorState edge : path0.calcEdges()) {
				int edgeId = edge.getEdge();
				if (edge instanceof VirtualEdgeIteratorState) {
					// first, via and last edges can be virtual
					VirtualEdgeIteratorState vEdge = (VirtualEdgeIteratorState) edge;
					edgeId = vEdge.getOriginalTraversalKey() / 2;
				}
				try {
					edges.add(hopper.getOSMWay(edgeId));
				} catch (ArrayIndexOutOfBoundsException e) {
					logger.info(String.format("EdgeID %d nicht identifizieren können.", edgeId));
				}
			}
		}
	}

}
