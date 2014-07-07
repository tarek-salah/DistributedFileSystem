import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;

public interface ReplicaServerInterface extends Remote {
	
	public int writeInReplicas(String filename, HashMap<Integer, String> file)
			throws RemoteException;
	
}
