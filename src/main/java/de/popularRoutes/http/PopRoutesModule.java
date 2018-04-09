package de.popularRoutes.http;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.graphhopper.GraphHopper;
import com.graphhopper.GraphHopperAPI;
import com.graphhopper.http.CmdArgsModule;
import com.graphhopper.http.GraphHopperService;
import com.graphhopper.http.RouteSerializer;
import com.graphhopper.http.SimpleRouteSerializer;
import com.graphhopper.json.GHJson;
import com.graphhopper.json.GHJsonFactory;
import com.graphhopper.json.geo.JsonFeatureCollection;
import com.graphhopper.routing.lm.LandmarkStorage;
import com.graphhopper.routing.lm.PrepareLandmarks;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.spatialrules.DefaultSpatialRule;
import com.graphhopper.routing.util.spatialrules.Polygon;
import com.graphhopper.routing.util.spatialrules.SpatialRule;
import com.graphhopper.routing.util.spatialrules.SpatialRuleLookup;
import com.graphhopper.routing.util.spatialrules.SpatialRuleLookupBuilder;
import com.graphhopper.spatialrules.SpatialRuleLookupHelper;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.util.CmdArgs;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.TranslationMap;

import de.popularRoutes.PopRoutesHopper;
import de.popularRoutes.ghextensions.PopRoutesFlagEncoderFactory;

public class PopRoutesModule extends AbstractModule {
	protected final CmdArgs args;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public PopRoutesModule(CmdArgs args) {
		this.args = CmdArgs.readFromConfigAndMerge(args, "config", "graphhopper.config");
	}

	@Override
	protected void configure() {
		install(new CmdArgsModule(args));
		bind(GHJson.class).toInstance(new GHJsonFactory().create());
		bind(GraphHopperAPI.class).to(GraphHopper.class);
	}

	@Provides
	@Singleton
	GraphHopper createGraphHopper(CmdArgs args) {
		GraphHopper graphHopper = new PopRoutesHopper() {
			@Override
			protected void loadOrPrepareLM() {
				if (!getLMFactoryDecorator().isEnabled() || getLMFactoryDecorator().getPreparations().isEmpty())
					return;

				try {
					String location = args.get(Parameters.Landmark.PREPARE + "split_area_location", "");
					Reader reader = location.isEmpty()
							? new InputStreamReader(LandmarkStorage.class.getResource("map.geo.json").openStream())
							: new FileReader(location);
					JsonFeatureCollection jsonFeatureCollection = new GHJsonFactory().create().fromJson(reader,
							JsonFeatureCollection.class);
					if (!jsonFeatureCollection.getFeatures().isEmpty()) {
						SpatialRuleLookup ruleLookup = SpatialRuleLookupBuilder.buildIndex(jsonFeatureCollection,
								"area", new SpatialRuleLookupBuilder.SpatialRuleFactory() {
									@Override
									public SpatialRule createSpatialRule(String id, List<Polygon> polygons) {
										return new DefaultSpatialRule() {
											@Override
											public String getId() {
												return id;
											}
										}.setBorders(polygons);
									}
								});
						for (PrepareLandmarks prep : getLMFactoryDecorator().getPreparations()) {
							// the ruleLookup splits certain areas from each
							// other but avoids making this a permanent change
							// so that other algorithms still can route through
							// these regions.
							if (ruleLookup != null && ruleLookup.size() > 0) {
								prep.setSpatialRuleLookup(ruleLookup);
							}
						}
					}
				} catch (IOException ex) {
					logger.error("Problem while reading border map GeoJSON. Skipping this.", ex);
				}

				super.loadOrPrepareLM();
			}
		}.forServer();
		graphHopper.setFlagEncoderFactory(new PopRoutesFlagEncoderFactory());
		graphHopper.setEncodingManager(new EncodingManager(graphHopper.getFlagEncoderFactory(), "popular_motorcycle", 4));

		SpatialRuleLookupHelper.buildAndInjectSpatialRuleIntoGH(graphHopper, args);

		graphHopper.init(args);
		return graphHopper;
	}

	@Provides
	@Singleton
	TranslationMap getTranslationMap(GraphHopper graphHopper) {
		return graphHopper.getTranslationMap();
	}

	@Provides
	@Singleton
	RouteSerializer getRouteSerializer(GraphHopper graphHopper) {
		return new SimpleRouteSerializer(graphHopper.getGraphHopperStorage().getBounds());
	}

	@Provides
	@Singleton
	GraphHopperStorage getGraphHopperStorage(GraphHopper graphHopper) {
		return graphHopper.getGraphHopperStorage();
	}

	@Provides
	@Singleton
	EncodingManager getEncodingManager(GraphHopper graphHopper) {
		return graphHopper.getEncodingManager();
	}

	@Provides
	@Singleton
	LocationIndex getLocationIndex(GraphHopper graphHopper) {
		return graphHopper.getLocationIndex();
	}

	@Provides
	@Singleton
	@Named("hasElevation")
	boolean hasElevation(GraphHopper graphHopper) {
		return graphHopper.hasElevation();
	}

	@Provides
	GraphHopperService getGraphHopperService(GraphHopper graphHopper) {
		return new GraphHopperService() {
			@Override
			public void start() {
				graphHopper.importOrLoad();
				logger.info("loaded graph at:" + graphHopper.getGraphHopperLocation() + ", data_reader_file:"
						+ graphHopper.getDataReaderFile() + ", flag_encoders:" + graphHopper.getEncodingManager() + ", "
						+ graphHopper.getGraphHopperStorage().toDetailsString());

			}

			@Override
			public void close() throws Exception {
				graphHopper.close();
			}
		};
	}

}
