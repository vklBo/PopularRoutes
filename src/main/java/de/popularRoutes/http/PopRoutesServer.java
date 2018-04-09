package de.popularRoutes.http;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.graphhopper.http.GHServer;
import com.graphhopper.http.PtModule;
import com.graphhopper.util.CmdArgs;

/**
 * Simple server similar to integration tests setup.
 */
public class PopRoutesServer extends GHServer {
	private final CmdArgs myargs;
	// private final Logger logger = LoggerFactory.getLogger(getClass());
	// private Server server;

	public PopRoutesServer(CmdArgs args) {
		super(args);
		this.myargs = args;
	}

	public static void main(String[] args) throws Exception {
		new PopRoutesServer(CmdArgs.read(args)).start();
	}

	protected Module createModule() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				binder().requireExplicitBindings();
				if (myargs.has("gtfs.file")) {
					// switch to different API implementation when using Pt
					install(new PtModule(myargs));
				} else {
					install(new PopRoutesModule(myargs));
				}
				install(new PopRoutesServletModule(myargs));
			}
		};
	}
}
