package drfoliberg.master.api.node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import drfoliberg.common.network.messages.ClusterProtocol;
import drfoliberg.common.network.messages.cluster.ConnectMessage;
import drfoliberg.common.network.messages.cluster.Message;
import drfoliberg.common.network.messages.cluster.StatusReport;

public class MasterHandler implements Runnable {

	private MasterNodeServletListener listener;
	private Socket s;

	public MasterHandler(Socket s, MasterNodeServletListener listener) {
		this.listener = listener;
		this.s = s;
	}

	@Override
	public void run() {
		try {
			ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
			ObjectInputStream in = new ObjectInputStream(s.getInputStream());
			out.flush();
			while (!s.isClosed()) {
				Object request = in.readObject();
				if (request instanceof Message) {
					Message requestMessage = (Message) request;
					switch (requestMessage.getCode()) {
					case CONNECT_ME:
						if (requestMessage instanceof ConnectMessage) {
							String unid = listener.connectRequest((ConnectMessage) requestMessage);
							out.writeObject(unid);
						} else {
							out.writeObject(new Message(ClusterProtocol.BAD_REQUEST));
						}
						out.flush();
						s.close();
						break;
					case STATUS_REPORT:
						if (requestMessage instanceof StatusReport) {
							listener.readStatusReport((StatusReport) requestMessage);
							out.writeObject(new Message(ClusterProtocol.BYE));
						} else {
							out.writeObject(new Message(ClusterProtocol.BAD_REQUEST));
						}
						out.flush();
						s.close();
						break;
					case DISCONNECT_ME:
						if (requestMessage instanceof ConnectMessage) {
							listener.disconnectRequest((ConnectMessage) requestMessage);
							out.writeObject(new Message(ClusterProtocol.BYE));
						} else {
							out.writeObject(new Message(ClusterProtocol.BAD_REQUEST));
						}
						out.flush();
						s.close();
						break;
					default:
						out.writeObject(new Message(ClusterProtocol.BAD_REQUEST));
						out.flush();
						s.close();
						break;
					}
				} else {
					out.writeObject(new Message(ClusterProtocol.BAD_REQUEST));
					out.flush();
					s.close();
				}
			}
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (s != null && !s.isClosed()) {
				try {
					s.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
