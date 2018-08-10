package maxlinke;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileReceiver extends FileTransceiver{

	private String downloadDirectory;
	private String hostAddress;
	private int hostPort;
	
	private boolean gotAddress;
	
	public FileReceiver () {
		super();
		gotAddress = false;
	}
	
	public void setDownloadDirectory(String directory){
		downloadDirectory = directory;
	}

	public boolean tryParseInternetAdress(String stringAddress){
		if(!isDone){
			return false;
		}else{
			gotAddress = false;
			String message = "";
			String[] parts = stringAddress.split(":");
			if(parts.length != 2){
				message = "Could not parse host address (Format : \"HOST:PORT\")";
			}else{
				try{
					hostPort = makePortFromString(parts[1]);
					hostAddress = parts[0];
					gotAddress = true;
					message = "Successfully parsed host address (" + hostAddress + ":" + hostPort + ")";
				}catch(Exception e){
					message = e.getMessage();
				}
			}
			updateLatestMessage(message);		
			return gotAddress;
		}
	}
	
	public void tryToReceive(){
		if(!isDone){
			return;
		}
		if(!gotAddress){
			updateLatestMessage("Address not set");
			isDone = true;
			return;
		}
		updateLatestMessage("Attempting to connect");
		new Thread(new Runnable(){
			@Override
			public void run() {
				isDone = false;
				try{
					JWFTInfo fileInfo = receiveFileInfo();
					checkVersion(fileInfo);
					receiveFile(fileInfo);				
				}catch(Exception e){
					e.printStackTrace();
					updateLatestMessage(e.getMessage());
				}finally{
					isDone = true;
				}
			}
		}).start();
	}
	
	private JWFTInfo receiveFileInfo() throws ClassNotFoundException, IOException{
		try(Socket connection = new Socket(hostAddress, hostPort)){
			updateLatestMessage("Receiving from " + connection.getInetAddress());
			ObjectInputStream ois = new ObjectInputStream(connection.getInputStream());
			return (JWFTInfo) ois.readObject();
		}
	}
	
	private void checkVersion(JWFTInfo fileInfo){
		if(!fileInfo.version.equals(Main.version)){
			throw new IllegalArgumentException("Aborting, sender has other version (" + fileInfo.version + ")");
		}
	}
	
	private void receiveFile(JWFTInfo fileInfo) throws UnknownHostException, IOException, InterruptedException{
		Path sinkPath = Paths.get(downloadDirectory + "/" + fileInfo.fileName);
		TwoPartTransporter writer = new TwoPartTransporter(Files.newOutputStream(sinkPath));
		try(Socket connection = new Socket(hostAddress, hostPort)){			
			TwoPartTransporter reader = new TwoPartTransporter(connection.getInputStream());
			startProgressMessageUpdaterThread(reader, fileInfo, "Receiving");
			transceive(reader, writer);
			if(reader.getBytesProcessed() == fileInfo.fileSizeInBytes){
				updateLatestMessage("Successfully finished file transfer");
			}else{
				updateLatestMessage("File transfer finished incomplete");
			}
		}
	}
}
