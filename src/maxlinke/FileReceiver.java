package maxlinke;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
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
				JWFTInfo fileInfo = receiveFileInfo();
				checkVersion(fileInfo);
				if(!isDone){
					updateLatestMessage("Received file info (" + fileInfo.fileName + ")");
					receiveFile(fileInfo);
				}
			}
		}).start();
	}
	
	private static JWFTInfo receiveFileInfo(){
		JWFTInfo fileInfo = null;
		if(!isDone){
			try(Socket connection = new Socket(hostAddress, hostPort)){
				updateLatestMessage("Receiving from " + connection.getInetAddress());
				try(ObjectInputStream ois = new ObjectInputStream(connection.getInputStream())){
					fileInfo = (JWFTInfo) ois.readObject();
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
		return fileInfo;
	}
	
	private static void checkVersion(JWFTInfo fileInfo){
		if(!fileInfo.version.equals(Main.version)){
			updateLatestMessage("Aborting, sender has other version (" + fileInfo.version + ")");
			isDone = true;
		}
	}
	
	private static Path createSinkPath(JWFTInfo fileInfo){
		return Paths.get(downloadDirectory + "/" + fileInfo.fileName);
	}
	
	private static void receiveFile(JWFTInfo fileInfo){
		updateLatestMessage("Starting receiving file");
		Path sinkPath = createSinkPath(fileInfo);
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
			
			Thread readThread = new Thread(reader);		//TODO also set daemon?
			Thread writeThread = new Thread(writer);
			
			readThread.start();
			writeThread.start();
			
			new Thread(){		//TODO code duplication galore between receiver and sender...
				@Override 
				public void run(){
					while(!isDone){
						long bytesProcessed = reader.getBytesProcessed();
						long fraction = (1000L * bytesProcessed) / fileInfo.fileSizeInBytes;
						int percent = (int)(fraction / 10L);
						int remainder = (int)(fraction % 10L);
						if(!isDone) updateLatestMessage("Receiving from " + connection.getInetAddress() + " (" + percent + "." + remainder + "%)");		//TODO might have to kill this thread manually
						yield();
					}
				}
			}.start();
						
			readThread.join();
			writeThread.join();
			
			isDone = true;
			
			if(reader.getBytesProcessed() == fileInfo.fileSizeInBytes){
				updateLatestMessage("Successfully finished file transfer");
			}else{
				updateLatestMessage("File transfer finished incomplete");
			}
		}catch(Exception e){
			updateLatestMessage("An error occured receiving the file");
			e.printStackTrace();
		}finally{
			isDone = true;
		}
	}
}
