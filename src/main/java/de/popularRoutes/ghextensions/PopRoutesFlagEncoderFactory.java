package de.popularRoutes.ghextensions;

import com.graphhopper.routing.util.DefaultFlagEncoderFactory;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.FlagEncoderFactory;
import com.graphhopper.util.PMap;

public class PopRoutesFlagEncoderFactory extends DefaultFlagEncoderFactory implements FlagEncoderFactory {
	public final String POP_MOTORCYCLE = "popular_motorcycle";

	public FlagEncoder createFlagEncoder(String name, PMap configuration) {
		if (name.equals(POP_MOTORCYCLE))
			return new PopRoutesMotorcycleFlagEncoder(configuration);

		return super.createFlagEncoder(name, configuration);
	}

}
