import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

@SuppressWarnings("serial")
public class ReplicaServer extends UnicastRemoteObject implements
		ReplicaServerInterface {

	private Registry sRegistry;
	private String replicaPath;
	private int sPort;

	public ReplicaServer(int sPort, String path) throws IOException,
			NotBoundException {
		super();
		this.sPort = sPort;
		this.replicaPath = path + "\\";
		System.out.println("this address=" + "localhost" + ",port=" + sPort);
		sRegistry = LocateRegistry.createRegistry(sPort);
		sRegistry.rebind(path, this);
	}

	@Override
	public int writeInReplicas(String filename, HashMap<Integer, String> file)
			throws RemoteException {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(replicaPath
					+ filename, true));
			for (int i = 1; i <= file.size(); i++) {
				System.out.println(file.get(i));
				bw.append(file.get(i));
				bw.flush();
			}
			bw.close();
		} catch (IOException e) {
			return 200;
		}
		return 201;
	}

	public static void main(String[] args) throws IOException,
			NotBoundException {
		BufferedReader r = new BufferedReader(new FileReader("repServers.txt"));
		String str = r.readLine();
		while (str != null) {
			String[] line = str.split(" ");
			ReplicaServer s = new ReplicaServer(Integer.parseInt(line[2]),
					line[0]);
			str = r.readLine();
		}
	}

}
