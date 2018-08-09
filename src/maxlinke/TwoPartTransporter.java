package maxlinke;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class TwoPartTransporter implements Runnable{

	boolean isFileReader;
	InputStream input;
	OutputStream output;
	TwoPartTransporter other;
	private long bytesProcessed;
	
	public TwoPartTransporter(InputStream input){
		this.input = input;
		isFileReader = true;
	}
	
	public TwoPartTransporter(OutputStream output){
		this.output = output;
		isFileReader = false;
	}
	
	public void link(TwoPartTransporter other) throws IOException{
		this.other = other;
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
	
	@Override
	public void run() {
		bytesProcessed = 0L;
		try{
			final byte[] buffer = new byte[0x10000];
			for (int bytesRead = input.read(buffer); bytesRead != -1; bytesRead = input.read(buffer)) {
				output.write(buffer, 0, bytesRead);
				bytesProcessed += bytesRead;
			}
		}catch(Exception e){
			System.out.println(">>> " + (isFileReader ? "reader " : "writer ") + "says :");
			if(e instanceof IOException) System.out.println("> an error occured while transmitting <\n" + e.getMessage());
			else throw new AssertionError();
			e.printStackTrace();
		}finally{
			System.out.println((isFileReader ? "reader-" : "writer-") + "thread finished");
			try {
				if(isFileReader){
					this.input.close();
					this.output.close();
				}else{
					this.output.close();
					this.input.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
}
