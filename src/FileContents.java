import java.io.IOException;
import java.io.Serializable;

public class FileContents implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private byte[] contents; // file contents

	public FileContents(byte[] contents) {
		this.contents = contents;
	}

	public void print() throws IOException {
		System.out.println("FileContents = " + contents);
	}

	public byte[] get() {
		return contents;
	}
}