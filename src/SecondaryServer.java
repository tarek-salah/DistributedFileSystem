import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Timer;
import java.util.TimerTask;

public class SecondaryServer {

	@SuppressWarnings("unused")
	private ServerInterface rmiServer;
	private ClientInterface rmiClient;
	private Registry sRegistry;
	private Registry cRegistry;
	@SuppressWarnings("unused")
	private Server secondary;
	private String secondaryPath;

	private int sPort;

	public SecondaryServer(int sPort, String path) throws IOException,
			NotBoundException {
		super();
		this.sPort = sPort;
		this.secondaryPath = path + "\\";
		secondary = new Server(sPort, "secondary", path, path);
	}

	public void getHeartBeats() throws NotBoundException, IOException {
		sRegistry = LocateRegistry.getRegistry("localhost",
				(new Integer(8080)).intValue());
		final Timer t = new Timer();
		t.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					sRegistry.lookup("primaryServer");
				} catch (Exception e) {
					t.cancel();
					System.out.println("primary server is down");
					try {
						BufferedReader r = new BufferedReader(new FileReader(
								secondaryPath + "Clients"));
						String s = r.readLine();
						while (s != null) {
							String[] line = s.split(" ");
							cRegistry = LocateRegistry.getRegistry("localhost",
									Integer.parseInt(line[1]));
							rmiClient = (ClientInterface) (cRegistry
									.lookup(line[1]));
							rmiClient.updateServerIP("localhost", sPort);
							s = r.readLine();
						}
					} catch (IOException e1) {
						e1.printStackTrace();
					} catch (NotBoundException e1) {
						e1.printStackTrace();
					}
				}
			}
		}, 1, 10000);
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException,
			NotBoundException {
		SecondaryServer s = new SecondaryServer(8081, "secondary");
		s.getHeartBeats();
	}

}
