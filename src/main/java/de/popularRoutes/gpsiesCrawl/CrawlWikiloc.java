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

public class CrawlWikiloc {
	private final String USER_AGENT = "Mozilla/5.0";

	public CrawlWikiloc() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws Exception {
		CrawlWikiloc http = new CrawlWikiloc();

		String fileID;
		String filename = "/home/vk/Routenplaner/neuetouren/wikiloc/fileIDs.txt";
		BufferedReader br = new BufferedReader(new FileReader(filename));

		System.out.println("\nRead Wikiloc-Tracks");
		while (br.ready()) {
			fileID = br.readLine();
			http.sendPost(fileID);
		}
		br.close();
	}

	// HTTP GET request
	// Noch nicht verwendet
	/*
	 * private void sendGet() throws Exception {
	 * 
	 * String url = "http://www.google.com/search?q=mkyong";
	 * 
	 * URL obj = new URL(url); HttpURLConnection con = (HttpURLConnection)
	 * obj.openConnection();
	 * 
	 * // optional default is GET con.setRequestMethod("GET");
	 * 
	 * //add request header con.setRequestProperty("User-Agent", USER_AGENT);
	 * 
	 * int responseCode = con.getResponseCode();
	 * System.out.println("\nSending 'GET' request to URL : " + url);
	 * System.out.println("Response Code : " + responseCode);
	 * 
	 * BufferedReader in = new BufferedReader( new
	 * InputStreamReader(con.getInputStream())); String inputLine; StringBuffer
	 * response = new StringBuffer();
	 * 
	 * while ((inputLine = in.readLine()) != null) { response.append(inputLine);
	 * } in.close();
	 * 
	 * //print result System.out.println(response.toString());
	 * 
	 * }
	 */

	// HTTP POST request
	private void sendPost(String fileId) throws Exception {

		String url = "https://de.wikiloc.com/wikiloc/downloadToFile.do";
		URL obj = new URL(url);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

		String filename = "/home/vk/Routenplaner/neuetouren/wikiloc/tracks/" + fileId + ".gpx";

		File file = new File(filename);
		if (file.exists()) {
			System.out.println(filename + " existiert bereits.");
			return;
		}

		Thread.sleep(1000);

		BufferedWriter bw = new BufferedWriter(new FileWriter(filename));

		// add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		con.setRequestProperty("Cookie",
				"cfduid=d98d6c2ef9c4694da65aa8cb63f34a4aa1504369457; SESSION=8bfbe45a-8c01-4de0-90dc-53a53e59ebd7; cookieNotice=1; _ga=GA1.2.1532423812.1504369458; _gid=GA1.2.226392477.1504369458");
		con.setRequestProperty("Referer", "https://de.wikiloc.com/wikiloc/download.do?id=" + fileId);

		String urlParameters = "id=" + fileId
				+ "&event=download&format=gpx&selFormat=gpx&filter=original&g-recaptcha-response=03AOmkcwKxk6UACTYSqY9JLKCOkP273k3L9LoYkjFZalHf6xau86L19N4w8diEBt8ktweILQdvi4FFS8tWJwePOqSFNNruxSk--7mrmkGg4Oy5nrMGthHFHjdY8MBoH_TwVE5UDWQttOo1nBY3cq7vO-YDvasHz3xzcXOLR6islWDnM2aOWhz1fkRsbEVFuopZ68hNTV0PFUdAuyHdEOobx1TNyA-yhAU2myUbH8o2hoqrswHRKGVKtu81_V0EusCpGZAfQRC7vQ4O6YXRyVNMHmUEMoYni0fUrwgalKuMjeex09w_TRN02IAUiGm-TZdU8ZfGjUT38YhTfU8jDYsEfCe5_t70cJaljQ";

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
