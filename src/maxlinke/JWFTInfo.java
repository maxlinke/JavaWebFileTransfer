package maxlinke;

import java.io.File;
import java.io.Serializable;

public class JWFTInfo implements Serializable{

	private static final long serialVersionUID = (long)Main.version.hashCode();

	public final String version;
	public final String fileName;
	public final long fileSizeInBytes;
	
	public JWFTInfo (File file) {
		version = Main.version;
		fileName = file.getName();
		fileSizeInBytes = file.length();
	}

}
