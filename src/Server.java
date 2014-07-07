import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

@SuppressWarnings("serial")
public class Server extends UnicastRemoteObject implements ServerInterface {

	private Registry registry;
	private Registry secRegistry;
	private ServerInterface secondaryServer;

	private ArrayList<String[]> replicas;
	private ReplicaServerInterface[] replicasInterfaces;

	private int port;
	private String address;
	private String serverType;
	private String serverPath;

	private BufferedWriter trans;
	private String transactionsPath = "transLog\\";
	private String clientPath = "ClientsFiles\\";

	private long transID = 1;

	private HashMap<Long, Integer> txnState;
	private HashMap<Long, String> txnMapping;
	private HashMap<String, Long> mutedFiles;
	private HashMap<String, String> clients;
	private HashMap<Long, Timer> timOut;

	public Server(int p, String serverType, String serverPath,
			String serverKeyword) throws IOException, NotBoundException {
		super();
		this.address = "localhost";
		this.port = p;
		this.serverType = serverType;
		this.serverPath = serverPath + "\\";

		this.txnState = new HashMap<Long, Integer>();
		this.txnMapping = new HashMap<Long, String>();
		this.mutedFiles = new HashMap<String, Long>();
		this.clients = new HashMap<String, String>();
		this.timOut = new HashMap<Long, Timer>();

		this.replicas = new ArrayList<String[]>();

		this.trans = new BufferedWriter(new FileWriter(this.serverPath
				+ "transactions", true));

		InitLoad();
		this.replicasInterfaces = new ReplicaServerInterface[replicas.size()];
		if (serverType.equals("primary"))
			connectToSecondaryServer();
		if (!serverType.startsWith("replica"))
			connectToReplicas();
		try {
			System.out.println("this address=" + address + ",port=" + port);
			registry = LocateRegistry.createRegistry(port);
			registry.rebind(serverKeyword, this);
		} catch (RemoteException e) {
			System.out.println("remote exception" + e);
		}
	}

	public void connectToSecondaryServer() {
		final Timer t = new Timer();
		t.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					secRegistry = LocateRegistry.getRegistry("localhost", 8081);
					secondaryServer = (ServerInterface) secRegistry
							.lookup("secondary");
					System.out.println("Connected to secondary Server");
					t.cancel();
				} catch (ConnectException e) {
				} catch (NotBoundException e) {
				} catch (RemoteException e) {

				}
			}
		}, 1, 5000);
	}

	public void connectToReplicas() {
		final Timer t = new Timer();
		t.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					for (int i = 0; i < replicas.size(); i++) {
						String[] line = replicas.get(i);
						Registry registry = LocateRegistry.getRegistry(line[1],
								Integer.parseInt(line[2]));
						replicasInterfaces[i] = (ReplicaServerInterface) registry
								.lookup(line[0]);
					}
				} catch (ConnectException e) {
				} catch (NotBoundException e) {
				} catch (RemoteException e) {

				}

			}
		}, 1, 5000);
	}

	@Override
	public FileContents read(String fileName) throws FileNotFoundException,
			IOException, RemoteException {
		File file = new File(serverPath + clientPath + fileName);
		FileInputStream fin = new FileInputStream(file);
		byte fileContent[] = new byte[(int) file.length()];
		fin.read(fileContent);
		return new FileContents(fileContent);
	}

	@Override
	public long newTxn(String fileName) throws RemoteException, IOException {
		// return newTrans(fileName);
		String s = transID + "";
		final long txnID = Long.parseLong(s);
		txnState.put(txnID, 0);
		txnMapping.put(txnID, fileName);
		trans.append(txnID + " " + fileName + " falseCommit\n");
		trans.flush();
		Timer t = new Timer();
		t.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				File f = new File(serverPath + transactionsPath + txnID);
				f.delete();
				mutedFiles.put(txnMapping.get(txnID), (long) 0);
				txnState.put(txnID, 2);
				try {
					trans.append(txnID + " " + txnMapping.get(txnID)
							+ " deleted\n");
					trans.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}, 150000000, 10000000);
		timOut.put(txnID, t);
		if (!mutedFiles.containsKey(fileName)) {
			mutedFiles.put(fileName, (long) 0);
		}
		if (serverType.equals("primary"))
			try {
				secondaryServer.newTxn(fileName);
			} catch (Exception e) {
				System.out.println("the secondary server isn't running1");
			}
		return transID++;
	}

	@Override
	public int write(long txnID, long msgSeqNum, byte[] data)
			throws RemoteException, IOException {
		if (txnMapping.containsKey(txnID)) {
			if (txnState.get(txnID) == 0) {
				BufferedWriter bw = new BufferedWriter(new FileWriter(
						serverPath + transactionsPath + txnID, true));
				bw.append(msgSeqNum + " =!=!= " + new String(data));
				bw.newLine();
				bw.append("==x=!==EnD_oF_MeSsAgE==!=x==");
				bw.newLine();
				bw.flush();
				bw.close();

				trans.append(txnID + " " + txnMapping.get(txnID)
						+ " reciving message : " + msgSeqNum + "\n");
				trans.flush();

				if (serverType.equals("primary"))
					try {
						secondaryServer.write(txnID, msgSeqNum, data);
					} catch (Exception e) {
						System.out
								.println("the secondary server isn't running2");
					}
				return ACK;
			} else if (txnState.get(txnID) == 1) {
				return ACK;
			} else {
				return INVALID_OPERATION;
			}
		} else {
			return INVALID_TRANSACTION_ID;
		}
	}

	public synchronized long newTrans(String fileName) throws IOException {
		String s = transID + "";
		final long txnID = Long.parseLong(s);
		txnState.put(txnID, 0);
		txnMapping.put(txnID, fileName);
		trans.append(txnID + " " + fileName + " falseCommit\n");
		trans.flush();
		Timer t = new Timer();
		t.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				File f = new File(serverPath + transactionsPath + txnID);
				f.delete();
				mutedFiles.put(txnMapping.get(txnID), (long) 0);
				txnState.put(txnID, 2);
				try {
					trans.append(txnID + " " + txnMapping.get(txnID)
							+ " deleted\n");
					trans.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}, 150000000, 10000000);
		timOut.put(txnID, t);
		if (!mutedFiles.containsKey(fileName)) {
			mutedFiles.put(fileName, (long) 0);
		}
		if (serverType.equals("primary"))
			try {
				secondaryServer.newTxn(fileName);
			} catch (Exception e) {
				System.out.println("the secondary server isn't running1");
			}
		transID++;
		return txnID;
	}

	public void writeInReplicas(long txnID, HashMap<Integer, String> file) {
		for (int i = 0; i < replicasInterfaces.length; i++) {
			try {
				replicasInterfaces[i].writeInReplicas(txnMapping.get(txnID),
						file);
			} catch (Exception e) {

			}
		}
	}

	@Override
	public int commit(long txnID, long numOfMsgs)
			throws MessageNotFoundException {
		if (txnState.get(txnID) == 0) {
			if (mutedFiles.get(txnMapping.get(txnID)) == txnID
					|| mutedFiles.get(txnMapping.get(txnID)) == 0) {
				if (mutedFiles.get(txnMapping.get(txnID)) == 0)
					mutedFiles.put(txnMapping.get(txnID), txnID);
				try {
					BufferedReader r = new BufferedReader(new FileReader(
							serverPath + transactionsPath + txnID));
					HashMap<Integer, String> file = new HashMap<Integer, String>();
					long count = 0;
					String s = r.readLine();
					int messageNum = 0;
					String messageText = "";
					while (s != null) {
						if (s.equals("==x=!==EnD_oF_MeSsAgE==!=x==")) {
							count++;
							messageText = "";
							s = r.readLine();
						} else {
							if (s.contains(" =!=!= ")) {
								String[] message = s.split(" =!=!= ");
								messageNum = Integer.parseInt(message[0]);
								messageText += message[1];
								file.put(messageNum, messageText);
								s = r.readLine();
							} else {
								messageText += "\n" + s;
								file.put(messageNum, messageText);
								s = r.readLine();
							}
						}
					}
					if (count == numOfMsgs) {
						r.close();
						timOut.get(txnID).cancel();
						BufferedWriter bw = new BufferedWriter(
								new FileWriter(serverPath + clientPath
										+ txnMapping.get(txnID), true));
						for (int i = 1; i <= numOfMsgs; i++) {
							bw.append(file.get(i));
							bw.flush();
						}
						bw.close();
						trans.append(txnID + " " + txnMapping.get(txnID)
								+ " trueCommit\n");
						trans.flush();
						mutedFiles.put(txnMapping.get(txnID), (long) 0);
						txnState.put(txnID, 1);
						if (serverType.equals("primary")) {
							try {
								secondaryServer.commit(txnID, numOfMsgs);
							} catch (Exception e) {
								System.out
										.println("the secondary server isn't running3");
							}
							writeInReplicas(txnID, file);
						}
						if (serverType.equals("secondary"))
							try {
								Registry registry = LocateRegistry.getRegistry(
										"localhost", 8080);
								registry.lookup("primaryServer");
							} catch (Exception e) {
								writeInReplicas(txnID, file);
							}
						return ACK;
					} else {
						System.out.println("not completed message");
						int[] noOfLosses = new int[(int) (numOfMsgs - count)];
						int j = 0;
						for (int i = 1; i <= numOfMsgs; i++) {
							if (!file.containsKey(i)) {
								noOfLosses[j] = i;
								j++;
							}
						}
						MessageNotFoundException mnfe = new MessageNotFoundException();
						mnfe.setMsgNum(noOfLosses);
						throw mnfe;
					}
				} catch (IOException e) {
					return ACK_RSND;
				}
			} else {
				return ACK_RSND;
			}
		} else if (txnState.get(txnID) == 1) {
			return ACK;
		} else {
			return INVALID_TRANSACTION_ID;
		}
	}

	@Override
	public int abort(long txnID) throws RemoteException {
		if (txnMapping.containsKey(txnID)) {
			try {
				File file = new File(serverPath + transactionsPath + txnID);
				file.delete();
				trans.append(txnID + " " + txnMapping.get(txnID)
						+ " aborting\n");
				trans.flush();
				mutedFiles.put(txnMapping.get(txnID), (long) 0);
				txnState.put(txnID, 2);
				timOut.get(txnID).cancel();
				if (serverType.equals("primary"))
					try {
						secondaryServer.abort(txnID);
					} catch (Exception e) {
						System.out
								.println("the secondary server isn't running");
					}
				return ACK;
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			return INVALID_TRANSACTION_ID;
		}
		return 0;
	}

	@Override
	public boolean registerClient(ClientInterface client) {
		try {
			BufferedWriter clients = new BufferedWriter(new FileWriter(
					serverPath + "Clients"));
			String[] data = client.getMyData();
			this.clients.put(data[1], data[0]);
			for (String s : this.clients.keySet()) {
				clients.append(this.clients.get(s) + " " + s);
				clients.newLine();
				clients.flush();
			}
			clients.close();
			if (serverType.equals("primary"))
				try {
					secondaryServer.registerClient(client);
				} catch (Exception e) {
					System.out.println("the secondary server isn't running");
				}
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public boolean unregisterClient(ClientInterface client)
			throws RemoteException {
		try {
			BufferedWriter clients = new BufferedWriter(new FileWriter(
					serverPath + "Clients"));
			String[] data = client.getMyData();
			this.clients.remove(data[1]);
			for (String s : this.clients.keySet()) {
				clients.append(this.clients.get(s) + " " + s);
				clients.newLine();
				clients.flush();
			}
			clients.close();
			if (serverType.equals("primary"))
				try {
					secondaryServer.unregisterClient(client);
				} catch (Exception e) {
					System.out.println("the secondary server isn't running");
				}
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public void InitLoad() throws IOException, NotBoundException {
		BufferedReader log = new BufferedReader(new FileReader(serverPath
				+ "transactions"));
		String s = log.readLine();
		long max = 0;
		while (s != null) {
			String[] line = s.split(" ");
			if (Long.parseLong(line[0]) > max)
				max = Long.parseLong(line[0]);
			final long txnID = Long.parseLong(line[0]);
			if (s.contains("falseCommit")) {
				txnMapping.put(txnID, line[1]);
				mutedFiles.put(line[1], txnID);
				txnState.put(txnID, 0);
				Timer t = new Timer();
				t.scheduleAtFixedRate(new TimerTask() {
					@Override
					public void run() {
						File f = new File(serverPath + transactionsPath + txnID);
						f.delete();
						mutedFiles.put(txnMapping.get(txnID), (long) 0);
						txnState.put(txnID, 2);
						try {
							trans.append(txnID + " " + txnMapping.get(txnID)
									+ " deleted\n");
							trans.flush();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}, 1000000, 10000000);
				timOut.put(txnID, t);
			} else if (s.contains("trueCommit")) {
				mutedFiles.put(line[1], (long) 0);
				txnState.put(txnID, 1);
				timOut.get(txnID).cancel();
			} else if (s.contains("deleted") || s.contains("aborting")) {
				mutedFiles.put(line[1], (long) 0);
				txnState.put(txnID, 2);
				timOut.get(txnID).cancel();
			}
			s = log.readLine();
		}
		transID = max + 1;

		BufferedReader r = new BufferedReader(new FileReader(serverPath
				+ "repServers.txt"));
		s = r.readLine();
		while (s != null) {
			final String[] line = s.split(" ");
			replicas.add(line);
			s = r.readLine();
		}
	}

	public void exit(String directory) {
		File f = new File(directory);
		f.deleteOnExit();
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws RemoteException
	 * @throws FileNotFoundException
	 * @throws NotBoundException
	 */
	public static void main(String[] args) throws FileNotFoundException,
			RemoteException, IOException, NotBoundException {
		String invoke = "server -ip localhost -port 8080 -dir ClientsFiles";
		String[] line = invoke.split(" ");
		new Server(Integer.parseInt(line[4]), "primary", "primary",
				"primaryServer");

	}

}
