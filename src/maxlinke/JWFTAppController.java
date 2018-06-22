package maxlinke;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

public class JWFTAppController {
	
	//TODO massive todo: when the application gets terminated, kill the threads too
		
	private File fileOpenPath = new File(".");
	private String downloadDirectory = "jwft_downloads";	
	private boolean portFieldUnclicked;
	private boolean ipFieldUnclicked;

    @FXML
    private Label messageLabel;

    @FXML
    private TextField portInputField;
    
    @FXML
    private TextField ipInputField;

    @FXML
    void selectFile() {
    	FileChooser fileChooser = new FileChooser();
    	fileChooser.setInitialDirectory(fileOpenPath); 
    	File selectedFile = fileChooser.showOpenDialog(null);
    	if(selectedFile != null) {
    		fileOpenPath = selectedFile.getParentFile();
    		FileSender.setSourceFile(selectedFile);
    	}
    }

	@FXML
	public void initialize(){
		//TODO export now and test (here and on pc)
		//TODO send the filename and the file in one go (use a bufferedwriter first and then switch) (changes in both)
		portFieldUnclicked = true;
		ipFieldUnclicked = true;
		messageLabel.setText("JWFT v" + Main.version);
		try {
			Files.createDirectories(Paths.get(downloadDirectory));
		} catch (IOException e) {
			downloadDirectory = "";
			messageLabel.setText("Could not create download directory");
			e.printStackTrace();
		}
		FileReceiver.setDownloadDirectory(downloadDirectory);
		startLabelUpdater();
	}
	
	@FXML
	public void portEntered(){
		FileSender.tryParsePort(portInputField.getText());
	}
	
	@FXML
	public void ipEntered(){
		FileReceiver.tryParseInternetAdress(ipInputField.getText());
	}
	
	@FXML
	public void openSocket(){
		if(FileSender.tryParsePort(portInputField.getText())){
			FileSender.tryToSend();
		}
	}
	
	//updateLatestMessage("Hosting on : LAN " + IPGetter.getLANIP() + " / WAN " + IPGetter.getWANIP());
	
	/*
	
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
	
	 */
	
	@FXML
	public void startReceiving(){
		if(FileReceiver.tryParseInternetAdress(ipInputField.getText())){
			FileReceiver.tryToReceive();
		}
	}
	
	@FXML
	public void portFieldClicked(){
		if(portFieldUnclicked){
			portInputField.setText("");
			portFieldUnclicked = false;
		}
	}
	
	@FXML
	public void ipFieldClicked(){
		if(ipFieldUnclicked){
			ipInputField.setText("");
			ipFieldUnclicked = false;
		}
	}
	
	private void startLabelUpdater(){
		Task<Void> labelUpdateTask = new Task<Void>(){
			@Override
			public Void call() throws Exception{
				while(true){
					Platform.runLater(new Runnable(){
						@Override
						public void run(){
							if(FileSender.hasNewMessages()) messageLabel.setText(FileSender.getLatestMessage());
							if(FileReceiver.hasNewMessages()) messageLabel.setText(FileReceiver.getLatestMessage());
						}
					});
					Thread.sleep(100);
				}
			}
		};
		Thread labelUpdateThread = new Thread(labelUpdateTask);
		labelUpdateThread.setDaemon(true);
		labelUpdateThread.start();
	}

}
