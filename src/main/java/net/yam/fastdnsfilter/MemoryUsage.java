package net.yam.fastdnsfilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple class for reporting memory usage
 * @author yamnet
 *
 */
public class MemoryUsage {
	
	static Logger logger = LoggerFactory.getLogger(MemoryUsage.class);
	
	private static String toMBStr(long l) {
		return ""+(l/1048576l)+" MB";
	}

	public static void memoryUsage() {
        // Get the Java runtime
        Runtime runtime = Runtime.getRuntime();
        // Run the garbage collector
        runtime.gc();
        // Calculate the used memory
        logger.warn("Memory usage: Total="+toMBStr(runtime.totalMemory())+", Free="+toMBStr(runtime.freeMemory()));
       
	}
}
