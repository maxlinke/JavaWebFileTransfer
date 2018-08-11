package maxlinke;

import java.io.IOException;

public abstract class FileTransceiver {

	protected String latestMessage;
	protected boolean gotUnreadMessages;
	protected boolean isDone;
	
	public FileTransceiver () {
		latestMessage = "";
		gotUnreadMessages = false;
		isDone = true;
	}
	
	protected void updateLatestMessage(String message){
		System.out.println(message);
		gotUnreadMessages = true;
		latestMessage = message;
	}
	
	public boolean hasNewMessages(){
		return gotUnreadMessages;
	}
	
	public String getLatestMessage(){
		gotUnreadMessages = false;
		return latestMessage;
	}
	
	protected void transceive (TwoPartTransporter reader, TwoPartTransporter writer) throws IOException, InterruptedException {
		reader.link(writer);
		Thread readThread = new Thread(reader);
		Thread writeThread = new Thread(writer);
		
		readThread.setDaemon(true);
		writeThread.setDaemon(true);
		
		readThread.start();
		writeThread.start();
		
		readThread.join();
		writeThread.join();
	}
	
	protected void startProgressMessageUpdaterThread (TwoPartTransporter tpt, JWFTInfo fileInfo, String messageBeforePercentage) {
		Thread thread = getProgressMessageUpdaterThread (tpt, fileInfo, messageBeforePercentage);
		thread.setDaemon(true);
		thread.start();
	}

	protected Thread getProgressMessageUpdaterThread (TwoPartTransporter tpt, JWFTInfo fileInfo, String messageBeforePercentage) {
		return new Thread () {
			@Override 
			public void run(){
				while(!isDone){
					long bytesProcessed = tpt.getBytesProcessed();
					long fraction = (1000L * bytesProcessed) / fileInfo.fileSizeInBytes;
					int percent = (int)(fraction / 10L);
					int remainder = (int)(fraction % 10L);
					updateLatestMessage(messageBeforePercentage + " (" + percent + "." + remainder + "%)");
					yield();
				}
				if(tpt.getBytesProcessed() == fileInfo.fileSizeInBytes){
					updateLatestMessage("Successfully finished file transfer");
				}else{
					updateLatestMessage("File transfer finished incomplete");
				}
			}
		};
	}
	
	protected int makePortFromString (String portString) throws InvalidPortException {
		try{
			int port = Integer.parseInt(portString);
			if(port < 0 || port > 65535){
				throw new InvalidPortException("Invalid Port  (Allowed : 0 - 65535)");
			}
			return port;
		}catch(NumberFormatException e){
			throw new InvalidPortException("Port \"" + portString + "\" is not a number");
		}
	}
	
	
}