package net.yam.fastdnsfilter;

import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.lang3.StringUtils;
import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Header;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

/**
 * Class for generating a "blackhole" response to a DNS request.
 * @author yamnet
 *
 */
public class BlackholeResponse {

	// static IP Address
	private InetAddress blackholeAddressA=null;
	// Static IPV6 Address
	private InetAddress blackholeAddressAAAA=null;
	

	/**
	 * Set a response with these IP addresses
	 * @param staticIP4 IP Address
	 * @param staticIP6 IP v6 Address
	 * @throws UnknownHostException
	 */
	public BlackholeResponse(String staticIP4, String staticIP6) throws UnknownHostException {
		if (StringUtils.isNotBlank(staticIP4)) {
			blackholeAddressA= Inet4Address.getByName(staticIP4);			
		}
		if (StringUtils.isNotBlank(staticIP6)) {
			blackholeAddressAAAA = Inet6Address.getByName(staticIP6);			
		}
	}
	
	/**
	 * Set a response with these IP addresses. 
	 * null values are accepted.
	 * @param staticIP4 IP Address
	 * @param staticIP6 IP v6 Address
	 * @throws UnknownHostException
	 */
	public BlackholeResponse(Inet4Address staticIP, Inet6Address staticIP6) {
		this.blackholeAddressA = staticIP;
		this.blackholeAddressAAAA = staticIP6;
	}
	
	/**
	 * Compute a DNS Response to a DNS Request, with an IPv4 or IPv6 defined in the constructor. If there is no address, response will return a NXDOMAIN error code (host not found).
	 * if there is a request for MX domain (searching for mail server), answear will be CNAME with name 'mx.domain', with static IP Addresses.
	 * @param request
	 * @return a datagram Packet, ready to be sent.
	 */
	DatagramPacket getBlackHoleResponse(DNSRequest request) {
		
		Record requestQuestion = request.getMessage().getQuestion();
		Message message = new Message();
		Header header = message.getHeader();
		message.addRecord(request.getMessage().getQuestion(), Section.QUESTION);
		
		header.setID(request.getId());
		header.setFlag(Flags.QR);
		// Authoritative answear
		header.setFlag(Flags.AA);
		// recursion staff
		if (request.getMessage().getHeader().getFlag(Flags.RD)) {
			header.setFlag(Flags.RD);
			header.setFlag(Flags.RA);
		}
		// Authoritative data ;-)
		header.setFlag(Flags.AD);
		
		int qtype=message.getQuestion().getType();
		boolean answered=false;
		
		if ( (qtype==Type.ANY||qtype==Type.A) && blackholeAddressA!=null) {
			message.addRecord(
					new ARecord(requestQuestion.getName(), DClass.IN, 0, blackholeAddressA), Section.ANSWER);
			answered=true;
		}
		if ( (qtype==Type.ANY||qtype==Type.AAAA) && blackholeAddressAAAA!=null) {
			message.addRecord(
					new AAAARecord(requestQuestion.getName(), DClass.IN, 0, blackholeAddressAAAA), 
					Section.ANSWER);
			answered=true;
		}
		if (qtype==Type.ANY||qtype==Type.MX) {
			Name mxhost = requestQuestion.getName();
			try {
				mxhost = new Name("mx."+mxhost.toString(true)+".");
			} catch (TextParseException e) {
				// TODO Bloc catch généré automatiquement
				e.printStackTrace();
			}
			message.addRecord(new MXRecord(requestQuestion.getName(), DClass.IN, 0, 1, mxhost), Section.ANSWER);
			if (blackholeAddressA!=null) {
				message.addRecord(new ARecord(mxhost, DClass.IN, 0, blackholeAddressA), Section.ADDITIONAL);
			}
			if (blackholeAddressAAAA!=null) {
				message.addRecord(new ARecord(mxhost, DClass.IN, 0, blackholeAddressAAAA), Section.ADDITIONAL);
			}
			answered=true;
		}
		// returns a Domain not found error if there is no answear
		if (!answered) {
			message.getHeader().setRcode(Rcode.NXDOMAIN);
		}
		byte[] abytes = message.toWire();
		
		DatagramPacket response = new DatagramPacket(abytes, abytes.length, request.getSocketAddress());
		return response;
	}

}
