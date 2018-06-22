package maxlinke;

import java.io.BufferedReader;
import java.io.IOException;
//import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
//import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileReceiver {

	private static String downloadDirectory;
	private static String hostAddress;
	private static int hostPort;
	private static String latestMessage;
	private static boolean gotUnreadMessages;
	
	private static boolean isDone;
	private static boolean gotAddress;
	private static boolean gotPort;
	
	static{
		gotUnreadMessages = false;
		gotAddress = false;
		gotPort = false;
		isDone = true;
	}
	
	public static boolean hasNewMessages(){
		return gotUnreadMessages;
	}
	
	public static String getLatestMessage(){
		gotUnreadMessages = false;
		return latestMessage;
	}
	
	public static void setDownloadDirectory(String directory){
		downloadDirectory = directory;
	}
	
	private static void updateLatestMessage(String message){
		System.out.println(message);
		gotUnreadMessages = true;
		latestMessage = message;
	}

	public static boolean tryParseInternetAdress(String stringAddress){
		gotAddress = false;
		gotPort = false;
		String[] parts = stringAddress.split(":");
		if(parts.length == 2){
			String tempHost = parts[0];
			try{
				int tempPort = Integer.parseInt(parts[1]);
				if(tempPort < 0){
					updateLatestMessage("No negative ports allowed");
				}
				hostAddress = tempHost;
				hostPort = tempPort;
				gotAddress = true;
				gotPort = true;
				updateLatestMessage("Successfully parsed host address (" + hostAddress + ":" + hostPort + ")");
			}catch(NumberFormatException e){
				updateLatestMessage("Could not parse port");
			}
			
		}else{
			updateLatestMessage("Could not parse host address (format : \"HOST:PORT\")");
		}
		if(gotAddress && gotPort) return true;
		else return false;
	}
	
	public static void tryToReceive(){
		isDone = false;
		if(!gotAddress || !gotPort){
			updateLatestMessage("Address not set");
			isDone = true;
			return;
		}
		updateLatestMessage("Attempting to connect");
		new Thread(new Runnable(){
			@Override
			public void run() {
//				try(Socket connection = new Socket(hostAddress, hostPort)){
//					updateLatestMessage("Receiving from " + connection.getInetAddress());
//					InputStream inputStream = connection.getInputStream();
//					Path sinkPath = receiveFilename(inputStream);
//					if(sinkPath != null) receiveFile(inputStream, sinkPath);
//					inputStream.close();
//				}catch(Exception e){
//					if(e instanceof UnknownHostException) updateLatestMessage("An error occured - Unknown Host");
//					else updateLatestMessage("An error occured");
//					e.printStackTrace();
//				}
				Path sinkPath = receiveFilename();
				if(!isDone){
					updateLatestMessage("Received filename (" + sinkPath + ")");
					receiveFile(sinkPath);
				}
			}
		}).start();
	}
	
	private static Path receiveFilename(){
		Path sinkPath = null;
		if(!isDone){
			try(Socket connection = new Socket(hostAddress, hostPort)){
				updateLatestMessage("Receiving from " + connection.getInetAddress());
				try(BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))){
					sinkPath = Paths.get(downloadDirectory + "/" + br.readLine());
				}catch(IOException e){
					isDone = true;
					updateLatestMessage("An error occured receiving the file info");
					e.printStackTrace();
				}
			}catch(Exception e){
				updateLatestMessage("Connection error");
				isDone = true;
				e.printStackTrace();
			}
		}
		return sinkPath;
	}
	
	private static void receiveFile(Path sinkPath){
		updateLatestMessage("Starting receiving file");
		TwoPartTransporter writer = null;
		try{
			writer = new TwoPartTransporter(Files.newOutputStream(sinkPath));
		}catch(IOException e){
			isDone = true;
			updateLatestMessage("An error occured creating the file");
			e.printStackTrace();
		}
		try(Socket connection = new Socket(hostAddress, hostPort)){
			updateLatestMessage("Receiving from " + connection.getInetAddress());
			
			TwoPartTransporter reader = new TwoPartTransporter(connection.getInputStream());
			reader.link(writer);
			
			Thread readThread = new Thread(reader);
			Thread writeThread = new Thread(writer);
			
			readThread.start();
			writeThread.start();
			
			readThread.join();
			writeThread.join();
			
			updateLatestMessage("Successfully finished file transfer");
		}catch(Exception e){
			updateLatestMessage("An error occured receiving the file");
			e.printStackTrace();
		}finally{
			isDone = true;
		}
	}
	
	/*
	private static Path receiveFilename(InputStream inputStream) throws IOException{
		Path sinkPath = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
		sinkPath = Paths.get(downloadDirectory + "/" + br.readLine());
		return sinkPath;
	}
	
	private static void receiveFile(InputStream inputStream, Path sinkPath) throws IOException, InterruptedException{
		TwoPartTransporter reader = new TwoPartTransporter(inputStream);
		TwoPartTransporter writer = new TwoPartTransporter(Files.newOutputStream(sinkPath));
		reader.link(writer);
		
		Thread readThread = new Thread(reader);
		Thread writeThread = new Thread(writer);
		
		readThread.start();
		writeThread.start();
		
		readThread.join();
		writeThread.join();
		
		updateLatestMessage("Successfully finished file transfer");
		
	}
	*/
	
}
