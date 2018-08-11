package maxlinke;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileSender extends FileTransceiver{

	private Path sourcePath;
	private int port;
	private JWFTInfo fileInfo;
	
	private boolean gotFile;
	private boolean gotPort;
	
	public FileSender () {
		super();
		gotFile = false;
		gotPort = false;
	}
	
	public boolean tryParsePort(String portString){
		if(!isDone){
			return false;
		}else{
			gotPort = false;
			String message = "";
			try{
				port = makePortFromString(portString);
				gotPort = true;
				message = "Successfully parsed port (" + Integer.toString(port) + ")";
			}catch(Exception e){
				message = e.getMessage();
			}
			updateLatestMessage(message);
			return gotPort;
		}
	}
	
	public void setSourceFile(File selectedFile){
		if(!isDone){
			//do nothing
		}else{
			Path tempPath = Paths.get(selectedFile.getAbsolutePath());
			if (!Files.isReadable(tempPath)){
				gotFile = false;
				updateLatestMessage("Could not get file");
			}else{
				sourcePath = tempPath;
				fileInfo = new JWFTInfo(selectedFile);
				gotFile = true;
				updateLatestMessage("Selected \"" + fileInfo.fileName + "\"");
			}	
		}
			
	}
	
	public void tryToSend(){
		if(!isDone){
			return;
		}
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
				isDone = false;
				try{
					sendFileInfo();
					sendFile();
				}catch(Exception e){
					e.printStackTrace();
					updateLatestMessage(e.getMessage());
				}finally{
					isDone = true;
				}
			}
		}).start();
	}
	
	private void sendFileInfo() throws IOException{
		try(ServerSocket socket = new ServerSocket(port)){
			updateLatestMessage(getHostingMessageText());
			Socket connection = socket.accept();
			updateLatestMessage("Sending to " + connection.getInetAddress());
			ObjectOutputStream oos = new ObjectOutputStream(connection.getOutputStream());
			oos.writeObject(fileInfo);
			oos.flush();
			oos.close();
		}
	}
	
	private String getHostingMessageText(){
		String LANIP, WANIP;
		try{LANIP = IPGetter.getLANIP();}
		catch(Exception e){LANIP = "<unavailable>";}
		try{WANIP = IPGetter.getWANIP();}
		catch(Exception e){WANIP = "<unavailable>";}
		return "Hosting on : LAN " + LANIP + " / WAN " + WANIP;
	}
	
	private void sendFile() throws IOException, InterruptedException{
		TwoPartTransporter reader = new TwoPartTransporter(Files.newInputStream(sourcePath));
		try(ServerSocket socket = new ServerSocket(port)){
			updateLatestMessage("Waiting for download request");
			Socket connection = socket.accept();
			TwoPartTransporter writer = new TwoPartTransporter(connection.getOutputStream());
			startProgressMessageUpdaterThread(reader, fileInfo, "Sending");
			transceive(reader, writer);
		}
	}
}
