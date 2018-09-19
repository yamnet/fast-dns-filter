package net.yam.fastdnsfilter;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;

/**
 * DomainsListTree is a reccursive object for dealing with list of domains: Domains are split into subdomains, and main DomainsListTree have childs for its subdomains.
 * TODO write detailed documentation.
 * @author yamnet
 */
public class DomainsListTree implements Iterable<String> {
	
	/**
	 * Static and constant object representing a leaf in the domain tree: This specific implementation cannot contains
	 * any sub-domains. 
	 */
	static final DomainsListTree LEAF = new DomainsListTree() {		
		
		public void add(String domain) {
			throw new IllegalStateException("Illegal operation for a LEAF");
		}
		public boolean contains(String domain) {
			return true;
		}
	};
	
	/**
	 * List if subdomains.
	 */
	Map<String, DomainsListTree> elements = new HashMap<String, DomainsListTree>();
	
	/**
	 * Public constructor: an empty tree.
	 */
	public DomainsListTree() {
		
	}

	protected DomainsListTree(String domain) {
		if (StringUtils.isBlank(domain)) {
			throw new IllegalArgumentException();
			// return
		}
		add(domain);
	}

	/**
	 * Adds a domain in the tree. The domain name will be reccursively split between dot char.
	 * @param domain
	 */
	public void add(String domain) {
		int n=domain.lastIndexOf('.');
		if (n<0) {
			elements.put(domain, LEAF);
			return;
		} 
		String prefix=domain.substring(0, n);
		String suffix=domain.substring(n+1);
		DomainsListTree element = elements.get(suffix);
		if (element==null) {
			elements.put(suffix, new DomainsListTree(prefix));
		} else if (element!=LEAF) {
			element.add(prefix);
		}
	}

	/**
	 * 
	 * @param domain
	 * @return true is this domain is contained in the tree.
	 */
	public boolean contains(String domain) {
		int n=domain.lastIndexOf('.');
		String prefix=null, suffix=null;
		if (n<0) {
			suffix=domain;
		} else {
			prefix=domain.substring(0, n);
			suffix=domain.substring(n+1);
		}
		DomainsListTree element = elements.get(suffix);
		if (element==LEAF) {
			return true;
		}
		if ((element==null)||(prefix==null)) {
			return false;
		}
		return element.contains(prefix);
	}
	
	public int readDomainsList(Reader reader) {
		LineIterator li=new LineIterator(reader);
		int n=0;
		while (li.hasNext()) {
			n++;
			add(li.next());
		}
		try {
			li.close();
		} catch (IOException e) {
			// Nothing to do
		}	
		return n;
	}
	
	
	private Iterator<String> iterator(final String suffix) {
		
		return new Iterator<String>() {
			// Create a new Treeset just for having alphabetical ordered list
			Iterator<String> itKeys = new TreeSet<String>(elements.keySet()).iterator();
			Iterator<String> itLeaf = null;
			
			public boolean hasNext() {
				if (itLeaf!=null) {
					if (itLeaf.hasNext()) {
						return true;
					}
					itLeaf=null;
				}
				return itKeys.hasNext();
			}
			
			public String next() {
				if (itLeaf!=null) {
					if (itLeaf.hasNext()) {
						if (suffix==null) {
							return itLeaf.next();
						}
						return itLeaf.next();
					}
					itLeaf=null;
				}
				String key = itKeys.next();
				DomainsListTree next = elements.get(key);
				if (next==LEAF) {
					if (suffix==null) {
						return key;
					}
					return key+suffix;
				}
				if (suffix==null) {
					itLeaf=next.iterator("."+key);
				} else {
					itLeaf=next.iterator("."+key+suffix);
				}
				return itLeaf.next();
			}
		};
		
	}
	
	/**
	 * Returns an Iterator of all domains contained in this domain Tree.
	 */
	public Iterator<String> iterator() {
		return iterator(null);
	}
	
}
