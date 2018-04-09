package de.popularRoutes.http;

import javax.inject.Singleton;

import com.graphhopper.http.CORSFilter;
import com.graphhopper.http.ChangeGraphServlet;
import com.graphhopper.http.GraphHopperServlet;
import com.graphhopper.http.GraphHopperServletModule;
import com.graphhopper.http.HeadFilter;
import com.graphhopper.http.I18NServlet;
import com.graphhopper.http.IPFilter;
import com.graphhopper.http.InfoServlet;
import com.graphhopper.http.InvalidRequestServlet;
import com.graphhopper.http.NearestServlet;
import com.graphhopper.util.CmdArgs;

public class PopRoutesServletModule extends GraphHopperServletModule {

	public PopRoutesServletModule(CmdArgs args) {
		super(args);
	}

	@Override
	protected void configureServlets() {
		//super.configureServlets();

        filter("*").through(HeadFilter.class);
        bind(HeadFilter.class).in(Singleton.class);

        filter("*").through(CORSFilter.class, params);
        bind(CORSFilter.class).in(Singleton.class);

        filter("*").through(IPFilter.class);
        bind(IPFilter.class).toInstance(new IPFilter(args.get("jetty.whiteips", ""), args.get("jetty.blackips", "")));

        serve("/popularRoutes/i18n*").with(I18NServlet.class);
        bind(I18NServlet.class).in(Singleton.class);

        serve("/popularRoutes/info*").with(InfoServlet.class);
        bind(InfoServlet.class).in(Singleton.class);

        serve("/popularRoutes/route*").with(GraphHopperServlet.class);
        bind(GraphHopperServlet.class).in(Singleton.class);

        serve("/popularRoutes/nearest*").with(NearestServlet.class);
        bind(NearestServlet.class).in(Singleton.class);

        if (args.getBool("web.change_graph.enabled", false)) {
            serve("/popularRoutes/change*").with(ChangeGraphServlet.class);
            bind(ChangeGraphServlet.class).in(Singleton.class);
        }

        // Can't do this because otherwise we can't add more paths _after_ this module.
        // Instead, put this route explicitly into Jetty.
        // (We really need a web service framework.)
        // serve("/*").with(InvalidRequestServlet.class);
        bind(InvalidRequestServlet.class).in(Singleton.class);

		//serve("/popularEdges*").with(PopRoutesServlet.class);
		//bind(PopRoutesServlet.class).in(Singleton.class);
		serve("/popularRoutes/popularEdges*").with(PopRoutesServlet.class);
		bind(PopRoutesServlet.class).in(Singleton.class);
		
		//serve("/weights*").with(PopRoutesWeightsServlet.class);
		//bind(PopRoutesWeightsServlet.class).in(Singleton.class);
		serve("/popularRoutes/weights*").with(PopRoutesWeightsServlet.class);
		bind(PopRoutesWeightsServlet.class).in(Singleton.class);

	}
}
