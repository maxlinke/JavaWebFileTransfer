package maxlinke;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

public class JWFTAppController {
			
	private File fileOpenPath = new File(".");
	private String downloadDirectory = "jwft_downloads";	
	private boolean portFieldUnclicked;
	private boolean ipFieldUnclicked;
	
	private FileSender fileSender;
	private FileReceiver fileReceiver;

    @FXML
    private Label messageLabel;

    @FXML
    private TextField portInputField;
    
    @FXML
    private TextField ipInputField;

    @FXML
	public void initialize(){
		portFieldUnclicked = true;
		ipFieldUnclicked = true;
		messageLabel.setText("JWFT v" + Main.version);
		try {
			Files.createDirectories(Paths.get(downloadDirectory));
		} catch (IOException e) {
			String message = "";
			message += "Could not create download directory \"" + downloadDirectory + "\"\n";
			message += "This might cause problems but you can use the program anyway";
			Alert alert = new Alert(AlertType.WARNING, message, ButtonType.OK);
			alert.showAndWait();
			
			downloadDirectory = "";
			e.printStackTrace();
		}		
		fileSender = new FileSender();
		fileReceiver = new FileReceiver();
		fileReceiver.setDownloadDirectory(downloadDirectory);
		startLabelUpdater();
	}
    
    @FXML
    void selectFile() {
    	FileChooser fileChooser = new FileChooser();
    	fileChooser.setInitialDirectory(fileOpenPath); 
    	File selectedFile = fileChooser.showOpenDialog(null);
    	if(selectedFile != null) {
    		fileOpenPath = selectedFile.getParentFile();
    		fileSender.setSourceFile(selectedFile);
    	}
    }
	
	@FXML
	public void portEntered(){
		fileSender.tryParsePort(portInputField.getText());
	}
	
	@FXML
	public void ipEntered(){
		fileReceiver.tryParseInternetAdress(ipInputField.getText());
	}
	
	@FXML
	public void openSocket(){
		if(fileSender.tryParsePort(portInputField.getText())){
			fileSender.tryToSend();
		}
	}
	
	@FXML
	public void startReceiving(){
		if(fileReceiver.tryParseInternetAdress(ipInputField.getText())){
			fileReceiver.tryToReceive();
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
							if(fileSender.hasNewMessages()) messageLabel.setText(fileSender.getLatestMessage());
							if(fileReceiver.hasNewMessages()) messageLabel.setText(fileReceiver.getLatestMessage());
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
