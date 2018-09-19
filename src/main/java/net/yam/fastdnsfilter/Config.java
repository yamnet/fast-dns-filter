package net.yam.fastdnsfilter;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for managin configuration file.
 * @TODO: Should be refactored.
 * @author yamnet
 *
 */
public class Config {
	
	public static final String CONFIG_FILE="config.properties";
	private static Properties props=new Properties();
	
	static Logger log = LoggerFactory.getLogger(Config.class);
	
	private static String serverAddress;
	private static int serverPort;
	
	private static int upstreamPort;
	private static String upstreamAddress;

	private static String bypassDomain;

	private static String blackholeIP;
	private static String blackholeIP6;

	private static String blacklistFile;

	private Config()  {
		
	}
	
	public static void loadProperties() throws IOException {
		props.load(FileUtils.openInputStream(new File(CONFIG_FILE)));
		serverAddress=getString("server.listen.address");
		serverPort=getInt("server.listen.port");
		upstreamAddress=getString("upstream.address");
		upstreamPort=getInt("upstream.port");
		bypassDomain=getString("bypass.domain");
		blackholeIP=getString("blackhole.ip");
		blackholeIP6=getString("blackhole.ip6");
		blacklistFile=getString("blacklist.file");
	}
	
	private static int getInt(String key) {
		String value=props.getProperty(key);
		if (props.get(key)==null) {
			log.error("Error in config: property '"+key+"' does not exist");
			return 0;
		}
		return NumberUtils.toInt(value);
	}

	private static String getString(String key) {
		String value=props.getProperty(key);
		if (props.get(key)==null) {
			log.error("Error in config: property '"+key+"' does not exist");
			return null;
		}
		return value;
	}
	
	public static String getServerAddress() {
		return serverAddress;
	}

	public static int getServerPort() {
		return serverPort;
	}

	public static int getUpstreamPort() {
		return upstreamPort;
	}

	public static String getUpstreamAddress() {
		return upstreamAddress;
	}

	public static String getBypassDomain() {
		return bypassDomain;
	}

	public static String getBlackholeIP() {
		return blackholeIP;
	}

	public static String getBlackholeIP6() {
		return blackholeIP6;
	}

	public static String getBlacklistFile() {
		return blacklistFile;
	}

	
}
