package net.yam.fastdnsfilter.utils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.yam.fastdnsfilter.DomainsListTree;

/**
 * Utility class for generating db files from a list of domains blacklist files 
 * @author yamnet
 *
 */
public class CreateDomainsListDB {

	static Logger logger = LoggerFactory.getLogger(CreateDomainsListDB.class);

	Connection connection = null;

	public void open(String filename) throws SQLException {
	    String url = "jdbc:sqlite:" + filename;
		connection = DriverManager.getConnection(url);
	}
	
    public void createTable() throws SQLException {    	
    	Statement stmt = connection.createStatement();
    	// Create table
        stmt.execute("create table if not exists domains (name text unique not null);");
    }
    
    public void createIndex() throws SQLException {    	
    	Statement stmt = connection.createStatement();
        // Create index for domains for getting faster in lookups
        stmt.execute("create unique index idx_name on domains (name);");
    }
    
    public void close() {
		try {
	    	if (connection!=null) {
	    		connection.close();
	    	}
		} catch (SQLException e) {
			// nothing to do
		}
    }
    
	public int insert(Reader reader) throws SQLException {
		LineIterator li=new LineIterator(reader);
		int n=0;
		String line=null;
		PreparedStatement stmt = connection.prepareStatement("insert into domains values (?);");
		connection.setAutoCommit(false);
		while (li.hasNext()) {
			n++;
			line=li.next();
			stmt.setString(1, line);
			stmt.execute();
		}
		try {
			li.close();
		} catch (IOException e) {
			// Nothing to do
			System.err.println(line);
		}
		connection.commit();
		stmt.close();
		return n;
	}

	public int insert(DomainsListTree domains) throws SQLException {
		int n=0;
		PreparedStatement stmt = connection.prepareStatement("insert into domains values (?);");
		connection.setAutoCommit(false);
		for (String domain : domains) {
			n++;
			stmt.setString(1, domain);
			stmt.execute();
		}
		connection.commit();
		stmt.close();
		return n;
	}
	
	public static String reverse(String name) {
		String[] a = StringUtils.split(name, '.');
		int l=a.length;
		if (a.length==1) {
			return name;
		}
		String[] r=new String[l];
		l=l-1;
		for (int i=0; i<=l; i++) {
			r[l-i]=a[i];
		}
		return StringUtils.join(r, '.');
	}
	

	/**
	 * Main app: Create the db sqlite file from a list of blicklist files.
	 * @param args
	 */
    public static void main(String[] args) {
    	try {    		
    		if (args.length<2) {
    			System.err.println("Usage: CreateDomainsListDB <newFile.db> file1 [file2 ...[file n]]");
    			return;
    		}
    		File f = new File(args[0]);
    		if (f.exists()) {
    			System.err.println("Error: File '"+args[0]+"' exists");
    			return;
    		}
    		
    		System.out.println("*** Reading Files ***");
    		DomainsListTree domains = new DomainsListTree();
			for (int i=1; i<args.length; i++) {
				System.out.print(args[i]+" ");
				Reader reader = new FileReader(new File(args[i]));
				int n=domains.readDomainsList(reader);
				System.out.println(" "+n);
			}
    		System.out.println("*** Writing database ***");
        	CreateDomainsListDB domainListDB = new CreateDomainsListDB();
			domainListDB.open(args[0]);
			domainListDB.createTable();
			int n=domainListDB.insert(domains);
			System.out.println("Wrote "+n+" entries");
    		System.out.println("Create index");
			domainListDB.createIndex();
			domainListDB.close();
    		System.out.println("Done.");
		} catch (Exception e) {
			// TODO Bloc catch automatically generated
			e.printStackTrace();
		}
    }    
}
