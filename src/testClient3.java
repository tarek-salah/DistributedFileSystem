import java.io.IOException;

public class testClient3 {
	/**
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws MessageNotFoundException
	 */
	public static void main(String[] args) throws IOException,
			InterruptedException {
		Client c = new Client("localhost", 8080, 7072);
		c.connect("primaryServer");
		long id = c.getNewTxn("newFile2.txt");
		System.out.println("trans ID " + id);
		int resp = c.writeSingleMessage(id, 1, "client3\n".getBytes());
		Thread.sleep(5000);
		System.out.println("Sending message " + resp);
		resp = c.writeSingleMessage(id, 2, "client3\n".getBytes());
		Thread.sleep(5000);
		System.out.println("Sending message " + resp);
		resp = c.writeSingleMessage(id, 3, "client3\n".getBytes());
		Thread.sleep(5000);
		System.out.println("Sending message " + resp);
		resp = c.writeSingleMessage(id, 4, "client3\n".getBytes());
		Thread.sleep(5000);
		System.out.println("Sending message " + resp);

		try {
			resp = c.commitTrans(id, 4);
			System.out.println("commit " + resp);
		} catch (MessageNotFoundException e) {
		}
	}

}
