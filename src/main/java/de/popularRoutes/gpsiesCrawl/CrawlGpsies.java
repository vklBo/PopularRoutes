package de.popularRoutes.gpsiesCrawl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class CrawlGpsies {
	private final String USER_AGENT = "Mozilla/5.0";

	public CrawlGpsies() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws Exception {
		CrawlGpsies http = new CrawlGpsies();

		String fileID;
		String filename = "/home/vk/Routenplaner/neuetouren/gpsies/fileIDs.txt";
		BufferedReader br = new BufferedReader(new FileReader(filename));

		System.out.println("\nRead GPSies-Tracks");
		while (br.ready()) {
			fileID = br.readLine();
			http.sendPost(fileID);
		}
		br.close();
	}

	// HTTP POST request
	private void sendPost(String fileId) throws Exception {

		String url = "https://www.gpsies.com/download.do";
		URL obj = new URL(url);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

		String filename = "/home/vk/Routenplaner/neuetouren/gpsies/tracks/" + fileId + ".gpx";

		File file = new File(filename);
		if (file.exists()) {
			System.out.println(filename + " existiert bereits.");
			return;
		}
		Thread.sleep(800);

		BufferedWriter bw = new BufferedWriter(new FileWriter(filename));

		// add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		con.setRequestProperty("Referer", "https://www.gpsies.com/map.do?fileId=" + fileId);

		String urlParameters = "fileId=" + fileId + "&speed=10&filetype=gpxTrk&submitButton=&inappropriate=";

		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();

		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'POST' request to URL : " + url);
		System.out.println("Post parameters : " + urlParameters);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
			bw.write(inputLine);
		}
		in.close();
		bw.close();
		// print result
		System.out.println(fileId + " geschrieben.");

	}

}
