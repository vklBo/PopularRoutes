package de.popularRoutes;

public class RouteType {
	protected int type;
	protected String weighting;
	protected String vehicle;

	public static int MOTORCYCLE = 1;
	public static int TOURING_BIKE = 2;
	public static int SPEED_BIKE = 3;
	public static int MOUNTAIN_BIKE = 4;

	public RouteType(int type) {
		this.type = type;

		if (type == MOTORCYCLE) {
			this.weighting = "fastest";
			this.vehicle = "motorcycle";
		} else if (type == TOURING_BIKE) {
			this.weighting = "fastest";
			this.vehicle = "motorcycle";
		} else if (type == SPEED_BIKE) {
			this.weighting = "fastest";
			this.vehicle = "motorcycle";
		} else if (type == MOUNTAIN_BIKE) {
			this.weighting = "fastest";
			this.vehicle = "motorcycle";
		}
	}

	public int getType() {
		return type;
	}

	public String getWeighting() {
		return weighting;
	}

	public String getVehicle() {
		return vehicle;
	}

}
