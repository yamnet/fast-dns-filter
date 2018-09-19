package net.yam.fastdnsfilter;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DNSServer implements Runnable {
	
	DatagramSocket receiver;
	App manager;
	boolean stopped = false;
	int listenPort;
	InetAddress listenAddress=null;

	
	static Logger logger = LoggerFactory.getLogger(DNSServer.class);

	public DNSServer(App manager, int listenPort, InetAddress listenAddress) throws SocketException {
		this.listenPort = listenPort;
		this.listenAddress=listenAddress;
		this.manager=manager;

		try {
			if (listenAddress!=null) {
				receiver = new DatagramSocket(listenPort, listenAddress);
			} else {
				receiver = new DatagramSocket(listenPort);
			}
		} catch (SocketException eio) {
			logger.error("Exception:Cannot open DNS port "+listenPort+"! "+eio.getMessage());
			throw eio;
		}
		logger.error("DNS Server listeing on port "+listenPort+(listenAddress!=null?" on "+listenAddress.getHostAddress():""));
		
	}

	public DNSServer(int listenPort) {
        if (listenPort==0) {
        	listenPort=53;
        }
		this.listenPort = listenPort;
	}
	
	DatagramSocket getSocket() {
		return receiver;
	}
	
	
	
	public void run() {

		/*
		try {
			requestManager = new App();
		} catch (IOException e1) {
			// TODO Bloc catch généré automatiquement
			e1.printStackTrace();
			return;
		}
		*/		
		
		while (!stopped) {
			try {
				DNSRequest request = DNSRequest.receive(receiver);
				manager.process(request);	
			} catch (Exception e) {
				if (!stopped)
					logger.error("Exception!", e);
			}
		}
		logger.info("DNSServer stopped!");
				
	}

}
