package net.yam.fastdnsfilter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

/** 
 * Main java application for DNS Filtering server
 * @author yamnet
 *
 */
public class App {
	
	static Logger logger = LoggerFactory.getLogger(App.class);
	
	public static Logger REQUEST_LOGGER = LoggerFactory.getLogger("DNS");
	
	InetSocketAddress upstream_dns = new InetSocketAddress(Config.getUpstreamAddress(), Config.getUpstreamPort());
	
	// Domains blacklist
	// DomainListTree blacklist;
	DomainsListDB blacklist;

	BlackholeResponse blackHoleResponse;
	
	// Server
	DNSServer server;
	
	// Proxy
	DNSProxy proxy;
	
	// bypass domain name
	String bypassDomain;
	
	// Addresses who have a temporary pass ;-)
	Map<InetAddress, Long> guestlist = new HashMap<InetAddress, Long>();
	
	
	/**
	 * Constructor: setup server and environment
	 * @throws IOException
	 */
	public App() throws IOException {
		
		// sqlite database
		try {
			blacklist=new DomainsListDB(Config.getBlacklistFile());
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
		
        String straddr =  Config.getServerAddress();
        InetAddress addr=null;
        if (StringUtils.isNotBlank(straddr)) {
        	addr=InetAddress.getByName(straddr);
        }
        
        //
        bypassDomain = Config.getBypassDomain();
        if (StringUtils.isBlank(bypassDomain)) {
        	bypassDomain=null;
        } else if (!bypassDomain.startsWith(".")) {
        	bypassDomain="."+bypassDomain;
        }
        
        server = new DNSServer(this, Config.getServerPort(), addr);
		
		blackHoleResponse = new BlackholeResponse(Config.getBlackholeIP(), Config.getBlackholeIP6());			
		
		proxy=new DNSProxy(server.getSocket(), new InetSocketAddress(Config.getUpstreamAddress(), Config.getUpstreamPort()));

	}

	
	public void run() {
		MemoryUsage.memoryUsage();
        server.run();
	}
	
	/**
	 * Process one DNS request (from the client side)
	 * @param request
	 * @throws IOException
	 * @throws SQLException
	 */
	void process(DNSRequest request) throws IOException, SQLException {
		Record question = request.getMessage().getQuestion();
		if (question!=null) {
			String name = request.getRequestName();
			
			// Backdoor, a Magic Workaround: Put host on a guest list for bypassing filters.
			if (bypassDomain!=null && name.endsWith(bypassDomain)) {
				String v=name.substring(0, name.lastIndexOf(".pass"));
				Integer hours = 0;
				try { 
					hours = Integer.parseInt(v);
					setOnGestList(request.getInetddress(),  hours);
					sendFilteredResponse(request);
				} catch (NumberFormatException e) {
					// Nothing to do
				}				
				// Return a blackhole response for that special request.
				sendFilteredResponse(request);
			}
			
			// Check if domain/host requested is contained in the blacklist 
			if (blacklist.contains(name) && !isOnGuestList(request.getInetddress())) {
				REQUEST_LOGGER.warn(StringUtils.leftPad(request.getInetddress().getHostAddress(), 15)+" "+name+" "+Type.string(request.getRequestType())+" BLACKHOLE");
				sendFilteredResponse(request);
				return;
			}
		}
	
		// This dns request is OK: Forward the request to the upstream DNS Server.
		proxy.forwardToServer(request);		
	}
	
	/** 
	 * Add an address on the guestlist
	 * @param addr IP Address
	 * @param hours 
	 */
	void setOnGestList(InetAddress addr, int hours) {
		Long validity;
		if (hours<0) {
			validity=Long.MAX_VALUE;
		} else {
			validity=System.currentTimeMillis()+hours*3600000l;
		}
		guestlist.put(addr,  validity);
	}
	
	/**
	 * Test if the IP Address 
	 * @param addr
	 * @return
	 */
	boolean isOnGuestList(InetAddress addr) {
		Long validity = guestlist.get(addr);
		if (validity!=null) {
			if (System.currentTimeMillis()<validity) {
				return true;
			} else {
				guestlist.remove(addr);
			}
		}
		return false;
	}
	
	void sendFilteredResponse(DNSRequest request) throws IOException {
		server.getSocket().send(blackHoleResponse.getBlackHoleResponse(request));
	}
	
	
    public static void main( String[] args )
    {    	
    	try {
			Config.loadProperties();
	        logger.info("Starting Fast DNS Filter");
	        App app = new App();
	        app.run();
	        logger.info("The End");
		} catch (Exception e) {
			// TODO Bloc catch généré automatiquement
			e.printStackTrace();
		}
    }
	
}
