package net.yam.fastdnsfilter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DomainsListDB {

	static Logger logger = LoggerFactory.getLogger(DomainsListDB.class);

	Connection connection = null;
	HashMap<Integer, PreparedStatement> searchStmtsMap=new HashMap<Integer, PreparedStatement>();

    public DomainsListDB(String fileName) throws SQLException {
        String url = "jdbc:sqlite:" + fileName;
    	connection = DriverManager.getConnection(url);        	
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
    
		
	public boolean contains(String domain) throws SQLException { 
		
		String[] domainParts = StringUtils.split(domain, '.');
		
		PreparedStatement searchStmt=searchStmtsMap.get(domainParts.length);
		if (searchStmt==null) {
			// Construit une nouvelle searchStmt
			StringBuilder sql=new StringBuilder("select * from domains where name in ( ");
			for (int i=0; i<domainParts.length; i++) {
				sql.append("?,");				
			}
			sql.setLength(sql.length()-1);
			sql.append(")");
			searchStmt=connection.prepareStatement(sql.toString());
			searchStmtsMap.put(domainParts.length, searchStmt);
		}
		StringBuilder sb=new StringBuilder(domain.length()+1);
		int ix=1;		
		for (int n=domainParts.length-1; n>0; n--) {			
			for (int i=n; i<domainParts.length; i++) {
				sb.append(domainParts[i]).append('.');
			}
			sb.setLength(sb.length()-1);
			searchStmt.setString(ix++, sb.toString());
			sb.setLength(0);
		}
		searchStmt.setString(ix++, domain);
		ResultSet rs=searchStmt.executeQuery();
		
		ix=0;
		while (rs.next()) {
			ix++;
		}
		return ix>0;
	}
    
    
    public static void main(String[] args) {
    	try {
        	DomainsListDB domainListDB = new DomainsListDB("porno.db");
			domainListDB.close();
		} catch (Exception e) {
			// TODO Bloc catch généré automatiquement
			e.printStackTrace();
		}
    }    
}
