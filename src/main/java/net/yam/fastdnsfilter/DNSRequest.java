package net.yam.fastdnsfilter;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;

import org.xbill.DNS.Message;

public class DNSRequest {	
	
	public static int DATAGRAM_MAX_SIZE = 1024;
	private DatagramPacket datagramPacket;

	private Message requestMessage;

	private DNSRequest() {
		
	}
	
	public DNSRequest(DatagramPacket requestPacket) throws IOException {
		this.datagramPacket = requestPacket;
		this.requestMessage = new Message(requestPacket.getData());
	}
	
	public Message getMessage() {
		return requestMessage;
	}

	public InetAddress getInetddress() {
		return datagramPacket.getAddress();
	}
	
	public SocketAddress getSocketAddress() {
		return datagramPacket.getSocketAddress();
	}

	public byte[] getData() {
		return datagramPacket.getData();
	}

	public int getDataLength() {
		return datagramPacket.getLength();
	}
	
	public int getId() {
		return requestMessage.getHeader().getID();
	}
	
	public int getRequestType() {
		return requestMessage.getQuestion().getType();
	}

	public String getRequestName() {
		return requestMessage.getQuestion().getName().toString(true);
	}
	
	public  String getRequestId() {
		return datagramPacket.getAddress().getHostAddress()
				+"|"+datagramPacket.getPort()
				+"|"+requestMessage.getHeader().getID();
	}
	
	public static DNSRequest receive(DatagramSocket socket) throws IOException {
		byte[] data = new byte[DATAGRAM_MAX_SIZE];
		DatagramPacket packet = new DatagramPacket(data,0, data.length);
		DNSRequest dnsrequest = new DNSRequest();
		socket.receive(packet);
		dnsrequest.datagramPacket = packet;
		dnsrequest.requestMessage = new Message(dnsrequest.datagramPacket.getData());
		return dnsrequest;
	}

}
