package de.popularRoutes.ghextensions;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.MotorcycleFlagEncoder;
import com.graphhopper.routing.weighting.CurvatureWeighting;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PMap;

public class PopularWeighting extends CurvatureWeighting {
	private final double minFactor;
	private int avoidResidential;
	private int avoidMotorway;
	private int avoidPrimary;
	private int avoidSecondary;
	private int avoidTertiary;
	private int avoidTunnel;
	private int avoidToll;
	private int avoidFerry;
	private int avoidFord;
	private int preferUnpaved;
	private int preferCurved;
	private int preferPopular;
	
	
	public PopularWeighting(FlagEncoder flagEncoder, PMap pMap) {
		super(flagEncoder, pMap);
		
		this.setAvoidMotorway(pMap.getInt("avoid_motorway", 7));
		this.setAvoidPrimary(pMap.getInt("avoid_primary", 0));
		this.setAvoidSecondary(pMap.getInt("avoid_secondary", 0));
		this.setAvoidTertiary(pMap.getInt("avoid_tertiary", 0));
		this.setAvoidResidential(pMap.getInt("avoid_residential", 2));
		this.setAvoidTunnel(pMap.getInt("avoid_tunnel", 1));
		this.setAvoidToll(pMap.getInt("avoid_toll", 0));
		this.setAvoidFerry(pMap.getInt("avoid_ferry", 0));
		this.setAvoidFord(pMap.getInt("avoid_ford", 0));
		this.setPreferUnpaved(pMap.getInt("prefer_unpaved", 0));
		this.setPreferCurved(pMap.getInt("prefer_curved", 8));
		this.setPreferPopular(pMap.getInt("prefer_popular", 8));
	

		double minBendiness = 1; // see correctErrors
		double maxPriority = 1; // BEST / BEST
		// double maxScenicness = 1; // BEST (Wert liegt zwischen 0 und 1 -
		// alles größer 0 weisst auf benutzte Wege hin)
		double maxScenicness = 7; // WORST (Wert liegt zwischen 0 und 1 - alles
									// größer 0 weisst auf benutzte Wege hin)
		//minFactor = minBendiness / Math.log(flagEncoder.getMaxSpeed()) / (0.5 + maxPriority)
		//		* calcPopularity(minScenicness);
		
		
		//TEST
		minFactor =  calcPopularity(maxScenicness) * 0.1 / flagEncoder.getMaxSpeed();// / (0.5 + maxPriority); 
	}

	public double calcPopularity(double value) {
		// die Berechnung soll auch selten besuchte Kanten relativ hoch
		// gewichten; 0-Werte sollen aber auch verwendet werden
		// return -Math.log(value + 0.01) / 3 + 1;
		return (1 - value/2) * (1 - value/2);
	}

	@Override
	public double getMinWeight(double distance) {
		return minFactor * distance;
	}

	@Override
	public double calcWeight(EdgeIteratorState edge, boolean reverse, int prevOrNextEdgeId) {
		PopRoutesMotorcycleFlagEncoder popFlagEncoder = (PopRoutesMotorcycleFlagEncoder) flagEncoder;
		double priority = flagEncoder.getDouble(edge.getFlags(), KEY);
		double bendiness = flagEncoder.getDouble(edge.getFlags(), MotorcycleFlagEncoder.CURVATURE_KEY);
		double popularity = flagEncoder.getDouble(edge.getFlags(), PopRoutesMotorcycleFlagEncoder.POPULAR_KEY);
		double speed = getRoadSpeed(edge, reverse);
		double roadDistance = edge.getDistance();

		double highwaytypeWeight = 1;
		
		if (speed <= 50 && !popFlagEncoder.isUnpaved(edge.getFlags()) && this.getPreferUnpaved() == 0) {
			speed = this.getAvoidResidential() != 0 ? speed/this.getAvoidResidential() : speed;
		}
		
		// Define the weight for the way based on its type
		highwaytypeWeight *= getHighwaytypeWeight(this.getAvoidMotorway(), popFlagEncoder.isMotorway(edge.getFlags()));
		highwaytypeWeight *= getHighwaytypeWeight(this.getAvoidPrimary(), popFlagEncoder.isPrimary(edge.getFlags()));
		highwaytypeWeight *= getHighwaytypeWeight(this.getAvoidSecondary(), popFlagEncoder.isSecondary(edge.getFlags()));
		highwaytypeWeight *= getHighwaytypeWeight(this.getAvoidTertiary(), popFlagEncoder.isTertiary(edge.getFlags()));
		
		// In der Berechnung der Geschwindigkeit aufgegangen
		//highwaytypeWeight *= getHighwaytypeWeight(this.getAvoidResidential(), popFlagEncoder.isResidential(edge.getFlags()));
		
		// Factors, to calculate new weights for bendiness and popularity
		double bendinessFactor = this.getCorrectionFactor(bendiness, this.getPreferCurved() * 10);
		double popularityFactor =  this.getCorrectionFactor(this.calcPopularity(popularity), this.getPreferPopular() * 10);
		
		// We use the log of the speed to decrease the impact of the speed,
		// therefore we don't use the highway
		double regularWeight = roadDistance / speed;

		double bendinessWeight = (bendinessFactor * regularWeight);// / (0.5 + priority);
		double popularityWeight = popularityFactor * bendinessWeight;
		double weight = popularityWeight * highwaytypeWeight;
		return weight;
	}

	private double getHighwaytypeWeight(int value, boolean useValue) {
		if (useValue){
			/* if (value == 0)
				return 1;
			else if (value == 1)
				return 1.5;
			else if (value == 2)
				return 2.5;
			else if (value == 3)
				return 5;
			else if (value == 4)
				return 30;
				*/
			return Math.pow(1.3, value);
			
		}
		return 1;
	}
	
	// Wie stark (mit wieviel Prozent) soll die Kurvigkeit oder die Popularität in die Berechnung einfließen? 
	private double getCorrectionFactor(double weight, double percent){
		return (1 - ((1- weight) * percent/100));
	}

	protected double getRoadSpeed(EdgeIteratorState edge, boolean reverse) {
		return reverse ? flagEncoder.getReverseSpeed(edge.getFlags()) : flagEncoder.getSpeed(edge.getFlags());
	}
	public int getAvoidMotorway() {
		return avoidMotorway;
	}

	public void setAvoidMotorway(int avoidMotorway) {
		this.avoidMotorway = avoidMotorway;
	}

	public int getAvoidPrimary() {
		return avoidPrimary;
	}

	public void setAvoidPrimary(int avoidPrimary) {
		this.avoidPrimary = avoidPrimary;
	}

	public int getAvoidSecondary() {
		return avoidSecondary;
	}

	public void setAvoidSecondary(int avoidSecondary) {
		this.avoidSecondary = avoidSecondary;
	}

	public int getAvoidTertiary() {
		return avoidTertiary;
	}

	public void setAvoidTertiary(int avoidTertiary) {
		this.avoidTertiary = avoidTertiary;
	}

	public int getAvoidTunnel() {
		return avoidTunnel;
	}

	public void setAvoidTunnel(int avoidTunnel) {
		this.avoidTunnel = avoidTunnel;
	}

	public int getAvoidToll() {
		return avoidToll;
	}

	public void setAvoidToll(int avoidToll) {
		this.avoidToll = avoidToll;
	}

	public int getAvoidFerry() {
		return avoidFerry;
	}

	public void setAvoidFerry(int avoidFerry) {
		this.avoidFerry = avoidFerry;
	}

	public int getAvoidFord() {
		return avoidFord;
	}

	public void setAvoidFord(int avoidFord) {
		this.avoidFord = avoidFord;
	}

	
	public int getPreferUnpaved() {
		return preferUnpaved;
	}

	public void setPreferUnpaved(int preferUnpaved) {
		this.preferUnpaved = preferUnpaved;
	}
	
	public int getAvoidResidential() {
		return avoidResidential;
	}

	public void setAvoidResidential(int avoidResidential) {
		this.avoidResidential = avoidResidential;
	}

	public int getPreferCurved() {
		return preferCurved;
	}

	public void setPreferCurved(int preferCurved) {
		this.preferCurved = preferCurved;
	}

	public int getPreferPopular() {
		return preferPopular;
	}

	public void setPreferPopular(int preferPopular) {
		this.preferPopular = preferPopular;
	}

	@Override
	public String getName() {
		return "popular";
	}
}
