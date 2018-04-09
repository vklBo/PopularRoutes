package de.popularRoutes.http;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.http.GHBaseServlet;

import de.popularRoutes.db.JSONGenerator;

public class PopRoutesServlet extends GHBaseServlet {
	private static final long serialVersionUID = 1L;
	private final Logger logger = LoggerFactory.getLogger(getClass()); 

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		logger.info("minLon: " + req.getParameter("minLon") + " - maxLat: " + req.getParameter("maxLat"));
		String respText = new JSONGenerator().createJson(
				Double.parseDouble(req.getParameter("minLon")),
				Double.parseDouble(req.getParameter("maxLon")),
				Double.parseDouble(req.getParameter("minLat")),
				Double.parseDouble(req.getParameter("maxLat")),
				Integer.parseInt(req.getParameter("categorie")),
				Integer.parseInt(req.getParameter("zoom")));
		
		writeResponse(resp, req.getParameter("categorie") + respText);
	}
}
