import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class Client extends UnicastRemoteObject implements ClientInterface {
	private String serverIP = "";
	private int serverPort;
	private int myPort;
	private ServerInterface rmiServer;
	private Registry serverRegistry;
	private Registry myRegistry;

	public Client(String serverIP, int sPort, int mport) throws IOException {
		super();
		this.serverIP = serverIP;
		this.serverPort = sPort;
		this.myPort = mport;

		myRegistry = LocateRegistry.createRegistry(myPort);
		myRegistry.rebind(myPort + "", this);
	}

	public void connect(String server) throws IOException {
		try {
			serverRegistry = LocateRegistry.getRegistry(this.serverIP,
					(new Integer(serverPort)).intValue());
			rmiServer = (ServerInterface) (serverRegistry.lookup(server));
			rmiServer.registerClient(this);
		} catch (RemoteException e) {
		} catch (NotBoundException e) {
			System.err.println(e);
		}
	}

	public long getNewTxn(String fileName) throws RemoteException, IOException {
		rmiServer.registerClient(this);
		return rmiServer.newTxn(fileName);
	}

	public void writeToFile(String fileName, ArrayList<byte[]> messages)
			throws RemoteException, IOException, InterruptedException {
		long transID = rmiServer.newTxn(fileName);
		for (int i = 1; i <= messages.size(); i++) {
			try {
				int response = rmiServer.write(transID, i, messages.get(i - 1));
				Thread.sleep(7000);
				if (response == ServerInterface.ACK) {
					System.out.println("Messge " + i + " has been sent");
				} else if (response == ServerInterface.INVALID_TRANSACTION_ID) {
					System.err.println("Invalid Transaction ID");
				}
			} catch (Exception ce) {
				System.out.println("the server down");
				System.out.println("Try to reconnect...");
				try {
					rmiServer = (ServerInterface) (serverRegistry
							.lookup("primaryServer"));
				} catch (ConnectException e) {
					Thread.sleep(5000);
					i--;
				} catch (NotBoundException e) {
				}
			}
		}
		// System.out.println(rmiServer.abort(transID) + " aborted");
		commitTrans(transID, messages);
	}

	public int writeSingleMessage(long transID, int msgNumber, byte[] message)
			throws RemoteException, IOException {
		return rmiServer.write(transID, msgNumber, message);
	}

	public int commitTrans(long transID, int numofmessages)
			throws RemoteException, MessageNotFoundException {
		rmiServer.unregisterClient(this);
		return rmiServer.commit(transID, numofmessages);
	}

	public int abort(long transID) throws RemoteException {
		return rmiServer.abort(transID);
	}

	public void commitTrans(long transID, ArrayList<byte[]> data)
			throws IOException, InterruptedException {
		try {
			while (true) {
				int resp = rmiServer.commit(transID, data.size());
				System.out.println(resp + " " + serverPort);
				if (resp == ServerInterface.ACK) {
					break;
				} else if (resp == ServerInterface.ACK_RSND) {
					System.out.println("The file is in use");
					System.out.println("Try to reconnect...");
					Thread.sleep(5000);
				}
			}
		} catch (MessageNotFoundException e) {
			int[] messages = e.getMsgNum();
			for (int i = 0; i < messages.length; i++) {
				try {
					int response = rmiServer.write(transID, messages[i],
							data.get(messages[i] - 1));
					if (response == ServerInterface.ACK) {
						System.out.println("Messge " + messages[i]
								+ " has been sent");
					} else if (response == ServerInterface.INVALID_TRANSACTION_ID) {
						System.err.println("Invalid Transaction ID");
					} else if (response == ServerInterface.ACK_RSND) {
						System.out.println("The file is in use");
						System.out.println("Try to reconnect...");
						Thread.sleep(5000);
						i--;
					}
				} catch (ConnectException ce) {
					System.out.println("the server down");
					System.out.println("Try to reconnect...");
					try {
						rmiServer = (ServerInterface) (serverRegistry
								.lookup("primaryServer"));
					} catch (ConnectException e1) {
						Thread.sleep(5000);
						i--;
					} catch (NotBoundException e1) {
					}
				}
			}
			commitTrans(transID, data);
		}
	}

	public void readFile(String fileName) throws RemoteException, IOException {
		try {
			FileContents fc = rmiServer.read(fileName);
			System.out.println(new String(fc.get()));
		} catch (FileNotFoundException f) {
			System.err.println("the file not found");
		}
	}

	@Override
	public void updateServerIP(String ip, int port) throws RemoteException {
		serverIP = ip;
		serverPort = port;
		try {
			connect("secondary");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String[] getMyData() throws RemoteException {
		return new String[] { "localhost", myPort + "" };
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws MessageNotFoundException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException,
			InterruptedException, MessageNotFoundException {
		Client c = new Client("localhost", 8080, 7070);
		c.connect("primaryServer");
		long id = c.getNewTxn("newFile4.txt");
		System.out.println("trans ID " + id);
		int resp = c.writeSingleMessage(id, 1, "client2\n".getBytes());
		Thread.sleep(5000);
		System.out.println("Sending message " + resp);
		resp = c.writeSingleMessage(id, 2, "client2\n".getBytes());
		Thread.sleep(5000);
		System.out.println("Sending message " + resp);
		resp = c.writeSingleMessage(id, 3, "client2\n".getBytes());
		Thread.sleep(5000);
		System.out.println("Sending message " + resp);
		resp = c.writeSingleMessage(id, 4, "client2\n".getBytes());
		Thread.sleep(5000);
		System.out.println("Sending message " + resp);

		try {
			resp = c.commitTrans(id, 4);
			System.out.println("commit " + resp);
		} catch (MessageNotFoundException e) {
		}
	}
}
