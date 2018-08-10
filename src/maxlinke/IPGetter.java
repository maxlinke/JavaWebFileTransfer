package maxlinke;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

public class IPGetter {

	public static String getLANIP() throws UnknownHostException {
		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			throw e;
		}
	}
	
	public static String getWANIP () throws MalformedURLException, IOException {
		try {
			URL url = new URL("http://checkip.amazonaws.com/");
			URLConnection connection = url.openConnection();
			connection.connect();
			BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String address = br.readLine();
			br.close();
			return address;
		} catch (MalformedURLException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		}
	}
	
}
