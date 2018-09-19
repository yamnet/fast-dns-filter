package net.yam.fastdnsfilter;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.CNAMERecord;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

public class DNSProxy {
	
	static Logger logger = LoggerFactory.getLogger(DNSProxy.class);
	
	DatagramSocket clientSideSocket;
	DatagramSocket serverSideSocket;
	InetSocketAddress upstreamAddress;
	
	SessionCache sessions = new SessionCache();
	boolean stopped = false;
	
	
	public DNSProxy(DatagramSocket clientSideSocket, InetSocketAddress upstreamAddress) throws SocketException {		
		this.clientSideSocket = clientSideSocket;
		this.upstreamAddress = upstreamAddress;
		serverSideSocket = new DatagramSocket();
		// New thread for async forward replies
		new Thread(new Listener()).start();
	}
	
	private class SessionCache {
		
		static final long SESSION_TIMELIFE=60000l;
		
		List<Session> sessionsList = new LinkedList<Session>(); 
		
		public Session findById(int id) {
			synchronized(sessionsList) {
				for (Session s : sessionsList) {
					if (s.id==id) {
						s.timestamp=System.currentTimeMillis();
						return s;
					}
				}
			}
			return null;
		}
		
		public Session findByClientId(String clientId) {
			synchronized(sessionsList) {
				for (Session s : sessionsList) {
					if (clientId.equals(s.clientId)) {
						s.timestamp=System.currentTimeMillis();
						return s;
					}
				}
			}
			return null;
		}
		
		public void add(Session s) {
			cleanCache();
			synchronized(sessionsList) {
				sessionsList.add(s);
			}
		}

		public void cleanCache() { 
			long t=System.currentTimeMillis()-SESSION_TIMELIFE;
			synchronized(sessionsList) {
				for (int i=sessionsList.size()-1; i>=0; i--) {
					if (sessionsList.get(i).timestamp<t) {
						sessionsList.remove(i);
					}
				}
			}
		}
	}
	
	private static class Session {
		static int seqId = 1;
		
		long timestamp;
		int id;
		String clientId;
		DNSRequest clientRequest; 
		
		public Session(DNSRequest clientRequest) {
			this.clientRequest = clientRequest;
			this.clientId=clientRequest.getRequestId();
			setNewId();
			timestamp=System.currentTimeMillis();
		}
		
		private synchronized void setNewId() {
			this.id=seqId++;
			seqId&=0xffff;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public int getId() {
			return id;
		}

		public String getClientId() {
			return clientId;
		}

		public DNSRequest getClientRequest() {
			return clientRequest;
		}
		
	}

	
	public void forwardToServer(DNSRequest clientRequest) throws IOException {
		// Recherche dans le cache une session
		Session session = sessions.findByClientId(clientRequest.getRequestId());
		if (session==null) {
			session = new Session(clientRequest);
			sessions.add(session);
		}		
		// Forward a new request to server
		Message msg=(Message) clientRequest.getMessage().clone();
		msg.getHeader().setID(session.getId());
		
		byte[] data = msg.toWire();
		DatagramPacket packet=new DatagramPacket(data, data.length);
		packet.setSocketAddress(upstreamAddress);
		serverSideSocket.send(packet);
		
	}
	
	private class Listener implements Runnable {

		public void run() {
			
			while (!stopped) {
				try {
					DNSRequest request = DNSRequest.receive(serverSideSocket);
					// Traitement du packet, à renvoyer au client.
					int id=request.getId();

					Session session=sessions.findById(id);
					if (session==null) {
						return;
					}
					DNSRequest clientRequest = session.getClientRequest();
					
					if (App.REQUEST_LOGGER.isInfoEnabled()) {
						Record[] r = request.getMessage().getSectionArray(Section.ANSWER);
						String name = request.getRequestName();						
						if (r.length==0) {
							// logger.debug("R.DNS: #"+id+" "+Rcode.string(request.getMessage().getHeader().getRcode()));
							App.REQUEST_LOGGER.info(StringUtils.leftPad(clientRequest.getInetddress().getHostAddress(), 15)+" "+name+" "+Type.string(request.getRequestType())+" "+Rcode.string(request.getMessage().getHeader().getRcode()));
							//
							
						} else {
							for (int n=0; (n<r.length && n<1); n++) {
								// logger.debug("R.DNS: #"+id+" "+r[n].toString());
								String aname;
								Record record = r[n];
								if (record instanceof ARecord) {
									aname=((ARecord)record).getAddress().getHostAddress();
								} else if (record instanceof AAAARecord) {
									aname=((AAAARecord)record).getAddress().getHostAddress();
								} else if (record instanceof CNAMERecord) {
									aname=((CNAMERecord)record).getAlias().toString(true);
								}  else if (record instanceof MXRecord) {
									aname=((MXRecord)record).getTarget().toString(true);
								} else {	
									aname=record.toString();
								}
								App.REQUEST_LOGGER.info(StringUtils.leftPad(clientRequest.getInetddress().getHostAddress(), 15)+" "+name+" "+Type.string(request.getRequestType())+" "+aname);
							}
						}
					}
					// Replace with old id
					Message msg=request.getMessage();
					msg.getHeader().setID(clientRequest.getId());
					byte[] data = msg.toWire();
					DatagramPacket packet=new DatagramPacket(data, data.length);
					packet.setSocketAddress(clientRequest.getSocketAddress());
					clientSideSocket.send(packet);
					
				} catch (IOException e) {
					if (!stopped)
						logger.error("Exception!", e);
				}
			}
			logger.info("DNSProxy stopped!");
			
		}
		
	}
	
	
}
