package de.popularRoutes;

import com.graphhopper.reader.DataReader;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.reader.osm.OSMReader;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.HintsMap;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.DataAccess;
import com.graphhopper.storage.Directory;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.util.BitUtil;

import de.popularRoutes.ghextensions.PopularWeighting;

/**
 *
 * @author Peter Karich
 */
public class PopRoutesHopper extends GraphHopperOSM {
	private BitUtil bitUtil = null;
	private DataAccess edgeMapping;

	/**
	 * Based on the hintsMap and the specified encoder a Weighting instance can
	 * be created. Note that all URL parameters are available in the hintsMap as
	 * String if you use the web module.
	 *
	 * @param hintsMap
	 *            all parameters influencing the weighting. E.g. parameters
	 *            coming via GHRequest.getHints or directly via "&amp;api.xy="
	 *            from the URL of the web UI
	 * @param encoder
	 *            the required vehicle
	 * @param graph
	 *            The Graph enables the Weighting for NodeAccess and more
	 * @return the weighting to be used for route calculation
	 * @see HintsMap
	 */
	public Weighting createWeighting(HintsMap hintsMap, FlagEncoder encoder, Graph graph) {
		String weighting = hintsMap.getWeighting().toLowerCase();

		if ("popular".equalsIgnoreCase(weighting)) {
			return new PopularWeighting(encoder, hintsMap);
		} else {
			return super.createWeighting(hintsMap, encoder, graph);
		}
	}

	@Override
	public boolean load(String graphHopperFolder) {
		boolean loaded = super.load(graphHopperFolder);

		Directory dir = getGraphHopperStorage().getDirectory();
		bitUtil = BitUtil.get(dir.getByteOrder());
		edgeMapping = dir.find("edge_mapping");

		if (loaded) {
			edgeMapping.loadExisting();
		}

		return loaded;
	}

	@Override
	protected DataReader createReader(GraphHopperStorage ghStorage) {
		OSMReader reader = new OSMReader(ghStorage) {

			{
				edgeMapping.create(1000);
			}

			// this method is only in >0.6 protected, before it was private
			@Override
			protected void storeOsmWayID(int edgeId, long osmWayId) {
				super.storeOsmWayID(edgeId, osmWayId);

				long pointer = 8L * edgeId;
				edgeMapping.ensureCapacity(pointer + 8L);

				edgeMapping.setInt(pointer, bitUtil.getIntLow(osmWayId));
				edgeMapping.setInt(pointer + 4, bitUtil.getIntHigh(osmWayId));
			}

			@Override
			protected void finishedReading() {
				super.finishedReading();

				edgeMapping.flush();
			}
		};

		return initDataReader(reader);
	}

	public long getOSMWay(int internalEdgeId) {
		long pointer = 8L * internalEdgeId;
		return getBitUtil().combineIntsToLong(getEdgeMapping().getInt(pointer), getEdgeMapping().getInt(pointer + 4L));
	}

	public BitUtil getBitUtil() {
		if (bitUtil == null) {
			Directory dir = getGraphHopperStorage().getDirectory();
			bitUtil = BitUtil.get(dir.getByteOrder());
		}
		return bitUtil;
	}

	public DataAccess getEdgeMapping() {
		if (edgeMapping == null) {
			Directory dir = getGraphHopperStorage().getDirectory();
			edgeMapping = dir.find("edge_mapping");
		}
		return edgeMapping;
	}

}
