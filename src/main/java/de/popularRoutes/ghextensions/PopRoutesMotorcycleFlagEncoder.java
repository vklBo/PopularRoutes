package de.popularRoutes.ghextensions;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.util.EncodedValue;
import com.graphhopper.routing.util.MotorcycleFlagEncoder;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PMap;

import de.popularRoutes.db.PopWaysGenerator;
import gnu.trove.map.hash.TLongLongHashMap;

/**
 * Defines bit layout for popular routes of motorbikes
 * <p>
 *
 * @author Volker Klingspor
 */
public class PopRoutesMotorcycleFlagEncoder extends MotorcycleFlagEncoder {
	public static final int POPULAR_KEY = 113;

	public static final int TYPE_NONE = 0;
	public static final int TYPE_MOTORWAY = 1;
	public static final int TYPE_PRIMARY = 2;
	public static final int TYPE_SECONDARY = 3;
	public static final int TYPE_TERTIARY = 4;
	public static final int TYPE_RESIDENTIAL = 5;
	
	private EncodedValue popularEncoder;
	private TLongLongHashMap popularWaysMap = null;
	
	protected long highwayMaskBit;
	protected long motorwayBit;
	protected long primaryBit;
    protected long secondaryBit;
    protected long tertiaryBit;
    protected long residentialBit;
    protected long tunnelBit;
    protected long tollBit;
//    protected long ferryBit;
    protected long fordBit;
    protected long unpavedBit;

	public PopRoutesMotorcycleFlagEncoder() {
		this(5, 5, 0);
	}

	public PopRoutesMotorcycleFlagEncoder(PMap properties) {
		this((int) properties.getLong("speed_bits", 5), properties.getDouble("speed_factor", 5),
				properties.getBool("turn_costs", false) ? 1 : 0);
		this.properties = properties;

		this.setBlockFords(properties.getBool("block_fords", false));
	}

	public PopRoutesMotorcycleFlagEncoder(String propertiesStr) {
		this(new PMap(propertiesStr));
	}

	public PopRoutesMotorcycleFlagEncoder(int speedBits, double speedFactor, int maxTurnCosts) {
		super(speedBits, speedFactor, maxTurnCosts);
	}

	protected TLongLongHashMap getPopularWays() {
		if (popularWaysMap == null) {

			try {
				PopWaysGenerator swg = new PopWaysGenerator();
				// TODO: Das muss natürlich irgendwo parametrisiert werden
				popularWaysMap = swg.getPopularWays("/home/vk/karten/pop_ways.ser");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return popularWaysMap;
	}

	@Override
	public int getVersion() {
		return 2;
	}

	/**
	 * Define the place of the speedBits in the edge flags for car.
	 */
	@Override
	public int defineWayBits(int index, int shift) {
		// first two bits are reserved for route handling in superclass
		shift = super.defineWayBits(index, shift);

		popularEncoder = new EncodedValue("Popular", shift, 3, 1, 0, 7);
		shift += popularEncoder.getBits();

		// 3 Bit für die unterschiedlichen Straßentypen
		motorwayBit = 1L << shift;
		primaryBit = 2L << shift;
	    secondaryBit = 3L << shift;
	    tertiaryBit = 4L << shift;
		residentialBit = 5L << shift;
		highwayMaskBit = 7L << shift; // alle relevanten Bits sind gesetzt - dient dem zurücksetzen des Highwaytyps
		shift += 3;
	    tunnelBit = 1L << shift++;
	    tollBit = 1L << shift++;
	    ferryBit = 1L << shift++;
	    fordBit = 1L << shift++;
	    unpavedBit = 1L << shift++;
	    
		return shift;
	}

	@Override
	public long handleWayTags(ReaderWay way, long allowed, long priorityFromRelation) {
		long flags = 0;

		flags = super.handleWayTags(way, allowed, priorityFromRelation);

		String highwayValue = way.getTag("highway");
		
		if (highwayValue == null) {
			flags = this.setHighwayType(flags, TYPE_NONE);
		}
		else if (highwayValue.equals("motorway") || highwayValue.equals("motorway_link") || highwayValue.equals("motorroad") || highwayValue.equals("trunk_link") || highwayValue.equals("trunk_link")){
			flags = this.setHighwayType(flags, TYPE_MOTORWAY);
		}
		else if (highwayValue.equals("primary") || highwayValue.equals("primary_link")){
			flags = this.setHighwayType(flags, TYPE_PRIMARY);
		}
		else if (highwayValue.equals("secondary") || highwayValue.equals("secondary_link")){
			flags = this.setHighwayType(flags, TYPE_SECONDARY);
		}
		else if (highwayValue.equals("tertiary") || highwayValue.equals("tertiary_link")|| highwayValue.equals("unclassified")){
			flags = this.setHighwayType(flags, TYPE_TERTIARY);
		}
		else if (highwayValue.equals("residential") || highwayValue.equals("living_street")){
			flags = this.setHighwayType(flags, TYPE_RESIDENTIAL);
		}
		else{ 
			flags = this.setHighwayType(flags, TYPE_NONE);
		}
		
		
		flags = this.setToll(flags, "yes".equals(way.getTag("toll")) || "yes".equals(way.getTag("toll:motorbike")));
		flags = this.setUnpaved(flags, "unpaved".equals(way.getTag("surface")));
		//TODO: Furt und Fähre
		
		// Set popularity to the Minimum
		flags = popularEncoder.setValue(flags, 0);

		return flags;
	}

	@Override
	public double getDouble(long flags, int key) {
		switch (key) {
		case PopRoutesMotorcycleFlagEncoder.POPULAR_KEY:
			return (double) popularEncoder.getValue(flags) / 7;
		default:
			return super.getDouble(flags, key);
		}
	}

	@Override
	public void applyWayTags(ReaderWay way, EdgeIteratorState edge) {
		super.applyWayTags(way, edge);

		long number = getPopularWays().get(way.getId());

		long scenicness = number;
		if (scenicness < 0) {
			scenicness = 0;
		}
		if (scenicness > 7) {
			scenicness = 7;
		}

		edge.setFlags(this.popularEncoder.setValue(edge.getFlags(), scenicness));
	}
	
    public long setHighwayType(long flags, int type) {
    	if (type == TYPE_MOTORWAY)
    		return (flags & ~highwayMaskBit) | motorwayBit;
    	else if (type == TYPE_PRIMARY)
    		return (flags & ~highwayMaskBit) | primaryBit;
    	else if (type == TYPE_SECONDARY)
    		return (flags & ~highwayMaskBit) | secondaryBit;
    	else if (type == TYPE_TERTIARY)
    		return (flags & ~highwayMaskBit) | tertiaryBit;
    	else if (type == TYPE_RESIDENTIAL)
    		return (flags & ~highwayMaskBit) | residentialBit;
    	else // Nur löschen
    		return flags & ~highwayMaskBit;
    }

    public boolean isResidential(long flags) {
        return (flags & highwayMaskBit) == residentialBit;
    }
    
	public boolean isMotorway(long flags) {
		return (flags & highwayMaskBit) == motorwayBit;
	}

    public boolean isPrimary(long flags) {
        return (flags & highwayMaskBit) == primaryBit;
    }

    public boolean isSecondary(long flags) {
        return (flags & highwayMaskBit) == secondaryBit;
    }
        
    public boolean isTertiary(long flags) {
        return (flags & highwayMaskBit) == tertiaryBit;
    }
    
    public long setTunell(long flags, boolean value) {
    	return (value ? flags | tunnelBit : flags & ~tunnelBit);
    }

    public boolean isTunnel(long flags) {
        return (flags & tunnelBit) != 0;
    }
    public long setToll(long flags, boolean value) {
    	return (value ? flags | tollBit : flags & ~tollBit);
    }

    public boolean isToll(long flags) {
        return (flags & tollBit) != 0;
    }
    public long setFerry(long flags, boolean value) {
    	return (value ? flags | ferryBit : flags & ~ferryBit);
    }

    public boolean isFerry(long flags) {
        return (flags & ferryBit) != 0;
    }
    public long setFord(long flags, boolean value) {
    	return (value ? flags | fordBit : flags & ~fordBit);
    }

    public boolean isFord(long flags) {
        return (flags & fordBit) != 0;
    }
    public long setUnpaved(long flags, boolean value) {
    	return (value ? flags | unpavedBit : flags & ~unpavedBit);
    }

    public boolean isUnpaved(long flags) {
        return (flags & unpavedBit) != 0;
    }

	@Override
	public String toString() {
		return "popular_motorcycle";
	}

}
