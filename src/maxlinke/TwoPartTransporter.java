package maxlinke;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class TwoPartTransporter implements Runnable{

	private boolean isFileReader;
	private InputStream input;
	private OutputStream output;
	private long bytesProcessed;
	
	public TwoPartTransporter(InputStream input){
		this.input = input;
		isFileReader = true;
		bytesProcessed = 0L;
	}
	
	public TwoPartTransporter(OutputStream output){
		this.output = output;
		isFileReader = false;
		bytesProcessed = 0L;
	}
	
	public void link(TwoPartTransporter other) throws IOException{
		if(this.isFileReader == other.isFileReader) throw new AssertionError("both streams are either in or output");
		PipedOutputStream pipedOut = new PipedOutputStream();
		PipedInputStream pipedIn = new PipedInputStream();
		if(isFileReader){
			this.output = pipedOut;
			other.input = pipedIn;
		}else{
			this.input = pipedIn;
			other.output = pipedOut;
		}
		pipedIn.connect(pipedOut);
	}
	
	public long getBytesProcessed(){
		return bytesProcessed;
	}
	
	private void closeStreams () throws IOException {
		if(isFileReader){
			this.input.close();
			this.output.close();
		}else{
			this.output.close();
			this.input.close();
		}
	}
	
	@Override
	public void run() {
		bytesProcessed = 0L;
		try{
			final byte[] buffer = new byte[0x10000];
			for (int bytesRead = input.read(buffer); bytesRead != -1; bytesRead = input.read(buffer)) {
				output.write(buffer, 0, bytesRead);
				bytesProcessed += bytesRead;
			}
		}catch(IOException e){
			e.printStackTrace();
		}finally{
			try {
				closeStreams();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
}
