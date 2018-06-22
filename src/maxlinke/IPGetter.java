package maxlinke;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

public class IPGetter {

	public static String getLANIP(){
		String address = null;
		try {
			address = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return address;
	}
	
	public static String getWANIP(){
		String address = null;
		try{
			URL url = new URL("http://checkip.amazonaws.com/");
			URLConnection connection = url.openConnection();
			connection.connect();
			BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			address = br.readLine();
			br.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return address;
	}
	
}
