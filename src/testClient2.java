import java.io.IOException;
import java.util.ArrayList;

public class testClient2 {

	/**
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws MessageNotFoundException
	 */
	public static void main(String[] args) throws IOException,
			InterruptedException {
		Client c = new Client("localhost", 8080, 7071);
		c.connect("primaryServer");
		long id = c.getNewTxn("newFile4.txt");
		System.out.println("trans ID " + id);

		// int resp = c.writeSingleMessage(id, 1, "hello world1\n".getBytes());
		// System.out.println("Sending message " + resp);
		// resp = c.writeSingleMessage(id, 2, "hello world2\n".getBytes());
		// System.out.println("Sending message " + resp);
		// resp = c.writeSingleMessage(id, 3, "hello world3\n".getBytes());
		// System.out.println("Sending message " + resp);
		// resp = c.writeSingleMessage(id, 4, "hello world4\n".getBytes());
		// System.out.println("Sending message " + resp);

		// resp = c.abort(id);
		// System.out.println("abort " + resp);

		// Client c1 = new Client("localhost", 8080, 7072);
		// c1.connect("primaryServer");
		// c1.readFile("newFile4.txt");
		//
		// try {
		// resp = c.commitTrans(id, 4);
		// System.out.println("commit " + resp);
		// } catch (MessageNotFoundException e) {
		// }
		//
		// c1.readFile("newFile4.txt");

		// ==============loss in messages=========================
		// int resp = c
		// .writeSingleMessage(id, 1, "loss in messages1\n".getBytes());
		// System.out.println("Sending message " + resp);
		// resp = c.writeSingleMessage(id, 3, "loss in messages3\n".getBytes());
		// System.out.println("Sending message " + resp);
		// try {
		// resp = c.commitTrans(id, 4);
		// System.out.println("commit " + resp);
		// } catch (MessageNotFoundException m) {
		// int[] arr = m.getMsgNum();
		// for (int i = 0; i < arr.length; i++) {
		// resp = c.writeSingleMessage(id, arr[i],
		// "deleted message\n".getBytes());
		// System.out.println("Sending message " + resp);
		// }
		// try {
		// resp = c.commitTrans(id, 4);
		// System.out.println("commit " + resp);
		// } catch (MessageNotFoundException e) {
		//
		// }
		// }
		// ==============================================================================

		// int resp = c.writeSingleMessage(id, 1, "client1\n".getBytes());
		// Thread.sleep(5000);
		// System.out.println("Sending message " + resp);
		// resp = c.writeSingleMessage(id, 2, "client1\n".getBytes());
		// Thread.sleep(5000);
		// System.out.println("Sending message " + resp);
		// resp = c.writeSingleMessage(id, 3, "client1\n".getBytes());
		// Thread.sleep(5000);
		// System.out.println("Sending message " + resp);
		// resp = c.writeSingleMessage(id, 4, "client1\n".getBytes());
		// Thread.sleep(5000);
		// System.out.println("Sending message " + resp);
		//
		// try {
		// resp = c.commitTrans(id, 4);
		// System.out.println("commit " + resp);
		// } catch (MessageNotFoundException e) {
		// }

		// ============================================================
		// int resp = c.writeSingleMessage(id, 1, "client1\n".getBytes());
		// System.out.println("Sending message " + resp);
		// resp = c.writeSingleMessage(id, 2, "client1\n".getBytes());
		// System.out.println("Sending message " + resp);
		// resp = c.writeSingleMessage(id, 3, "client1\n".getBytes());
		// System.out.println("Sending message " + resp);
		// resp = c.writeSingleMessage(id, 4, "client1\n".getBytes());
		// System.out.println("Sending message " + resp);
		// System.exit(0);
		// =====================================================
		ArrayList<byte[]> messages = new ArrayList<byte[]>();
		messages.add("failure test1\n".getBytes());
		messages.add("failure test2\n".getBytes());
		messages.add("failure test3\n".getBytes());
		messages.add("failure test4\n".getBytes());
		c.writeToFile("newFile2.txt", messages);
	}

}
