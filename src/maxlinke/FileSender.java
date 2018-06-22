package maxlinke;

import java.io.File;
import java.io.IOException;
//import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileSender {

	private static Path sourcePath;
	private static int port;
	private static String fileName;
	private static String latestMessage;
	private static boolean gotUnreadMessages;
	
	private static boolean isDone;
	private static boolean gotFile;
	private static boolean gotPort;
	
	static{
		gotUnreadMessages = false;
		gotFile = false;
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
	
	private static void updateLatestMessage(String message){
		System.out.println(message);
		gotUnreadMessages = true;
		latestMessage = message;
	}
	
	public static boolean tryParsePort(String portString){
		gotPort = false;
		try{
			int tempPort = Integer.parseInt(portString);
			if(tempPort < 0){
				updateLatestMessage("No negative ports allowed");
			}else{
				port = tempPort;
				gotPort = true;
				updateLatestMessage("Successfully parsed port (" + Integer.toString(port) + ")");
			}
		}catch(NumberFormatException e){
			updateLatestMessage("The port could not be parsed");
		}
		if(gotPort) return true;
		else return false;
	}
	
	public static void setSourceFile(File selectedFile){
		Path tempPath = Paths.get(selectedFile.getAbsolutePath());
		if (!Files.isReadable(tempPath)){
			gotFile = false;
			updateLatestMessage("Could not get file");
		}else{
			sourcePath = tempPath;
			fileName = selectedFile.getName();
			gotFile = true;
			updateLatestMessage("Selected \"" + fileName + "\"");
		}		
	}
	
	public static void tryToSend(){
		isDone = false;
		if(!gotFile){
			updateLatestMessage("No file selected");
			isDone = true;
			return;
		}
		if(!gotPort){
			updateLatestMessage("Port not set");
			isDone = true;
			return;
		}
		new Thread(new Runnable(){
			@Override
			public void run() {
//				try(ServerSocket socket = new ServerSocket(port)){
//					updateLatestMessage("Hosting on : LAN " + IPGetter.getLANIP() + " / WAN " + IPGetter.getWANIP());
//					Socket connection = socket.accept();
//					updateLatestMessage("Sending to " + connection.getInetAddress());
//					OutputStream outputStream = connection.getOutputStream();
//					sendFileInfo(outputStream);
//					sendFile(outputStream);
//					outputStream.close();
//				}catch (Exception e){
//					updateLatestMessage("An error occured sending the file");
//					e.printStackTrace();
//				}
				sendFileInfo();
				if(!isDone){
					updateLatestMessage("Transmitted file info");
					sendFile();
				}
			}
		}).start();
	}
	
	private static void sendFileInfo(){
		try(ServerSocket socket = new ServerSocket(port)){
			updateLatestMessage("Hosting on : LAN " + IPGetter.getLANIP() + " / WAN " + IPGetter.getWANIP());
			Socket connection = socket.accept();
			updateLatestMessage("Sending to " + connection.getInetAddress());
			try(PrintWriter firstOut = new PrintWriter(connection.getOutputStream(), true)){
				firstOut.println(fileName);
			}catch(IOException e){
				isDone = true;
				updateLatestMessage("An error occured sending the file name");
				e.printStackTrace();
			}
		} catch (Exception e) {
			updateLatestMessage("An error occured sending the file info");
			isDone = true;
			e.printStackTrace();
		}
	}
	
	private static void sendFile(){
		try {
			TwoPartTransporter reader = new TwoPartTransporter(Files.newInputStream(sourcePath));
			try(ServerSocket socket = new ServerSocket(port)){
				updateLatestMessage("Waiting for download request");
				Socket connection = socket.accept();
				updateLatestMessage("Sending to " + connection.getInetAddress());			
				if(!isDone){
					TwoPartTransporter writer = new TwoPartTransporter(connection.getOutputStream());
					reader.link(writer);
					
					Thread readThread = new Thread(reader);
					Thread writeThread = new Thread(writer);
					
					readThread.setDaemon(true);
					writeThread.setDaemon(true);
					
					readThread.start();
					writeThread.start();
					
					readThread.join();
					writeThread.join();
					
					updateLatestMessage("Successfully finished file transfer");
				}
				
			} catch (Exception e) {
				updateLatestMessage("An error occured sending the file");
				e.printStackTrace();
			} finally {
				isDone = true;
			}
		} catch (IOException e) {
			isDone = true;
			updateLatestMessage("An error occured reading the file");
			e.printStackTrace();
		}
	}
	
	/*
	private static void sendFileInfo(OutputStream outputStream){
		PrintWriter pw = new PrintWriter(outputStream, true);
		pw.println(fileName);
	}
	
	private static void sendFile(OutputStream outputStream) throws IOException, InterruptedException{
		TwoPartTransporter reader = new TwoPartTransporter(Files.newInputStream(sourcePath));
		TwoPartTransporter writer = new TwoPartTransporter(outputStream);
		reader.link(writer);
		
		Thread readThread = new Thread(reader);
		Thread writeThread = new Thread(writer);
		
		readThread.setDaemon(true);
		writeThread.setDaemon(true);
		
		readThread.start();
		writeThread.start();
		
		readThread.join();
		writeThread.join();
		
		updateLatestMessage("Successfully finished file transfer");
	}
	*/
	
}
