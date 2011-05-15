package com.challengeandresponse.configfilereader;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.jdom.*;
import org.jdom.input.SAXBuilder;

/**
 * Read an XML configuration file and populate a key,value mapped data structure with the contents.
 * The structure can then be plumbed for config data. The file is not held open - it is loaded and read into a DOM
 * structure.
 * 
 * <p>Config files are generally in the simple, one-dimensional form (but keep reading for new methods that can do more):<br />
 * <pre>
 * &lt;rootnode&gt;
 * &lt;name1&gt;value&lt;/name1&gt;
 * &lt;name2&gt;value&lt;/name2&gt;
 * &lt;name3&gt;value&lt;/name3&gt;
 * &lt;/rootnode&gt;
 * </pre>
 * 
 * <p>This depends on the JDOM library.</p>
 * <p>This package handles the file io, DOM navigation, and error-catching.
 * Callers just init a data structure to hold the results, then make repeated calls to fetch each configuration item by name.
 *</p>
 *<p>This version handles integers, longs, strings, and booleans.
 *
 *<p>Config file format MUST look exactly like this:<br />
 *<pre>
 *&lt;config&gt;
 *	&lt;section1&gt;
 *		&lt;element1ofsection1&gt;blah&lt;/element1ofsection1&gt;
 *		&lt;element2ofsection1&gt;blah&lt;/element2ofsection1&gt;
 *	&lt;/section1&gt;
 *	&lt;section2&gt;
 *		&lt;element1ofsection2&gt;blah&lt;/element1ofsection2&gt;
 *		&lt;element2ofsection2&gt;blah&lt;/element2ofsection2&gt;
 *	&lt;/section2&gt;
 *&lt;/config&gt;
 *</pre>
 *
 * <p>
 * the new getMap(elementName,attributeName,continueOnError) method can retrieve config chunks that include a value and also an attribute, and that may occur more than once:<br />
 * <code>
 * HashMap <String,String> hm = cfr.getMap("client","id",false);
 * </code>
 * <pre>
 * &lt;client id="client1"&gt;12345&lt;/client&gt;
 * &lt;client id="client2"&gt;dfwop24ur90uqw&lt;/client&gt;
 * </pre>
 * </p>
 * 
 * <p>
 * the new getMap(elementName) method can retrieve config chunks that include a value and many attributes, 
 * for element names that may occur more than once. Note that in this case, the KEY is the value of the
 * element, and the internal HashMap contains all the (attribute,value) pairs.<br />
 * <code>
 * HashMap <String,HashMap<String,String>> hm = cfr.getMap("client");
 * </code>
 * <pre>
 * &lt;client car="Honda" wife="Glenda"&gt;client1&lt;/client&gt;
 * &lt;client&gt;client2&lt;/client&gt;
 * </pre>
 * yields:
 * <pre>
 * (client1,(car,Honda; wife,Glenda))
 * (client2,())
 * </pre>
 * </p>
 * 
 * <p>
 * the new getList() method can retrieve Strings that may occur more than once:<br />
 * <code>
 * List <String> l = cfr.getList("flavor");
 * </code>
 * <pre>
 * &lt;flavor&gt;chocolate&lt;/flavor&gt;
 * &lt;flavor&gt;vanilla&lt;/flavor&gt;
 * &lt;flavor&gt;rum raisin&lt;/flavor&gt;
 * </pre>
 * </p>
 * 
 * <p>
 * The ONLY hardcoded string in the above is the opening and closing root element "config". Everything
 * else can be named as you please, of course. The recommendation is that each section relate to the actual
 * package and class that will be reading and using that section, for example "com.challengeandresponse.imoperator.com"
 * 
 * 
 * </p>
 * @author jim
 * @version 0.1 of 2008-02-06
 *
 */
public class ConfigFileReader {

	public static final String PRODUCT_SHORT = "ConfigFileReader";
	public static final String PRODUCT = "Configuration File Reader";
	public static final String VERSION = "0.2";
	public static final String VERSION_FULL = PRODUCT+" "+VERSION;
	public static final String COPYRIGHT = "Copyright (c) 2008 Challenge/Response LLC, Cambridge, MA";

	public static final String ROOT_ELEMENT = "config";

	private Document doc;
	private Element rootE;
	private String rootSectionName;
	
	private Iterator <Element> stepper = null; // when stepping thru secondary elements
		/**
	 * @param configFile a File pointing to the configuration file to process
	 * @param sectionName the name of the node that contains all the config data
	 */
	public ConfigFileReader(File configFile, String sectionName)
	throws IOException, ElementNotFoundException {
		SAXBuilder saxb = new SAXBuilder();
		try {
			doc = saxb.build(configFile);
			this.rootSectionName = sectionName;
			setRootElement(null);
		}
		catch (JDOMException jdoe) {
			throw new IOException("ConfigFileReader:DOM exception building configuration from file: "+configFile+" " + jdoe.getMessage());
		}
		catch (IOException ioe) {
			throw new IOException("ConfigFileReader:IO exception building configuration from file: "+configFile+" "+ioe.getMessage());
		}
	}

	/**
	 * we are going to step through all elements that have the name 'elementName'...
	 * step into, then hasNextELement(), then stepToNextElenet() and then process 
	 * using cfr.get___ calls as normal
	 * 
	 * <pre>
	 * cfr.stepInto("nameOfElement");
	 * while (cfr.hasNext()) {
	 *	cfr.stepToNext();
	 *	int i = cfr.getInt(..);
	 * }
	 * cfr.stepInto(null); // reset
	 * </pre>
	 */
	@SuppressWarnings("unchecked")
	public void stepInto(String elementName)
	throws ElementNotFoundException {
		if (elementName == null) {
			stepper = null;
			setRootElement(null);			
		}
		else
			stepper = rootE.getChildren(elementName).iterator();
	}
	
	public boolean hasNext() {
		if (stepper == null)
			return false;
		return stepper.hasNext();
	}
	
	public void stepToNext()
	throws ElementNotFoundException {
		if (stepper == null)
			return;
		setRootElement(stepper.next());
	}
	
	
	/**
	 * Sets the "root element" to something other than the document root section in hand... useful
	 * if you are stepping through an element manually but want to still use the methdos of this clas,
	 * for example:<br />
	 * <pre>
	 * Iterator it = cfr.getElementIterator("client");
	 * while (it.hasNext()) {
	 *     Element el = (Element) it.nextElement();
	 *     cfr.setRootElement(el);
	 *     int i = cfr.getInt(0,false,"intval");
	 * }
	 * cfr.setRootElement(null); // reset things to normal
	 * 
	 */
	private void setRootElement(Element e)
	throws ElementNotFoundException {
		if (e == null) {
			if (doc.getRootElement().getChild(rootSectionName) == null)
				throw new ElementNotFoundException("Element:'"+rootSectionName+"' does not exist.");
			this.rootE = doc.getRootElement().getChild(rootSectionName);
		}
		else
			this.rootE = e;
	}
	
	

	

	/**
	 * Retrieve an integer value from the config file
	 * 
	 * @param defaultValue	if useDefault is true, and no element with name 'elementName' is found, return this value instead
	 * @param useDefault if true, then the default value is returned if the element is not found or if an error occurred trying to read it
	 * @param elementName the name of the element containing the desired value
	 * @throws ConfigFileReaderException if an error occurs AND useDefault is false. In all other cases, defaultValue is returned
	 */
	public int getInt(int defaultValue, boolean useDefault, String elementName)
	throws ConfigFileReaderException, ElementNotFoundException {
		try {
			String s = rootE.getChildText(elementName);
			// if the named element was not found, return the default value
			if (s == null) {
				if (useDefault)
					return defaultValue;
				else
					throw new ElementNotFoundException("ConfigFileReader:Element not found:"+elementName);
			}
			return Integer.parseInt(s);
		}
		catch (Exception e) {
			if (useDefault)
				return defaultValue;
			else
				throw new ConfigFileReaderException("ConfigFileReader:Exception reading integer from config file: "+e.getMessage());
		}
	}

	public int getInt(String elementName)
	throws ConfigFileReaderException, ElementNotFoundException {
		return getInt(0,false,elementName);
	}
	
	
	/**
	 * Retrieve a long value from the config file
	 * 
	 * @param defaultValue	if useDefault is true, and no element with name 'elementName' is found, return this value instead
	 * @param useDefault if true, then the default value is returned if the element is not found or if an error occurred trying to read it
	 * @param elementName the name of the element containing the desired value
	 * @throws ConfigFileReaderException if an error occurs AND useDefault is false. In all other cases, defaultValue is returned
	 */
	public long getLong(long defaultValue, boolean useDefault, String elementName)
	throws ConfigFileReaderException, ElementNotFoundException {
		try {
			String s = rootE.getChildText(elementName);
			// if the named element was not found, return the default value
			if (s == null) {
				if (useDefault)
					return defaultValue;
				else
					throw new ElementNotFoundException("ConfigFileReader:Element not found:"+elementName);
			}
			return Long.parseLong(s);
		}
		catch (Exception e) {
			if (useDefault)
				return defaultValue;
			else
				throw new ConfigFileReaderException("ConfigFileReader:Exception reading long from config file: "+e.getMessage());
		}
	}
	
	public long getLong(String elementName)
	throws ConfigFileReaderException, ElementNotFoundException {
		return getLong(0L,false,elementName);
	}


	/**
	 * Retrieve a boolean value from the config file. The return value is evaluated by Boolean.parseBoolean(String s).
	 * 
	 * @param defaultValue	if useDefault is true, and no element with name 'elementName' is found, return this value instead
	 * @param useDefault if true, then the default value is returned if the element is not found or if an error occurred trying to read it
	 * @param elementName the name of the element containing the desired value
	 * @throws ConfigFileReaderException if an error occurs AND useDefault is false. In all other cases, defaultValue is returned
	 * @see java.lang.Boolean
	 */
	public boolean getBoolean(boolean defaultValue, boolean useDefault, String elementName)
	throws ConfigFileReaderException, ElementNotFoundException {
		try {
			String s = rootE.getChildText(elementName);
			// if the named element was not found, return the default value
			if (s == null) {
				if (useDefault)
					return defaultValue;
				else
					throw new ElementNotFoundException("ConfigFileReader:Element not found:"+elementName);
			}
			return Boolean.parseBoolean(s);
		}
		catch (Exception e) {
			if (useDefault)
				return defaultValue;
			else
				throw new ConfigFileReaderException("ConfigFileReader:Exception reading integer from config file: "+e.getMessage());
		}
	}

	public boolean getBoolean(String elementName)
	throws ConfigFileReaderException, ElementNotFoundException {
		return getBoolean(false,false,elementName);
	}

		
	/**
	 * Retrieve a String value from the config file
	 * 
	 * @param defaultValue	if useDefault is true, and no element with name 'elementName' is found, return this value instead
	 * @param useDefault if true, then the default value is returned if the element is not found or if an error occurred trying to read it
	 * @param elementName the name of the element containing the desired value
	 * @throws ConfigFileReaderException if an error occurs AND useDefault is false. In all other cases, defaultValue is returned
	 */
	public String getString(String defaultValue, boolean useDefault, String elementName)
	throws ConfigFileReaderException, ElementNotFoundException {
		try {
			String s = rootE.getChildText(elementName);
			// if the named element was not found, return the default value
			if (s == null) {
				if (useDefault)
					return defaultValue;
				else
					throw new ElementNotFoundException("ConfigFileReader:Element not found:"+elementName);
			}
			return s;
		}
		catch (Exception e) {
			if (useDefault)
				return defaultValue;
			else
				throw new ConfigFileReaderException("ConfigFileReader:Exception reading string from config file: "+e.getMessage());
		}
	}
	
	public String getString(String elementName)
	throws ConfigFileReaderException, ElementNotFoundException {
		return getString("",false,elementName);
	}


	/**
	 * Retrieve many String values from the config file (where each value is in an element of the same name)
	 * 
	 * @param elementName the name of the element containing the desired values
	 * @throws ConfigFileReaderException if an error occurs AND useDefault is false. In all other cases, defaultValue is returned
	 */
	public List<String> getList(String elementName)
	throws ConfigFileReaderException {
		try {
			ArrayList <String> result = new ArrayList <String> ();
			List <Element> l = rootE.getChildren(elementName);
			Iterator <Element> it = l.iterator();
			while (it.hasNext()) {
				String s = (it.next()).getText();
				result.add(s);
			}
			return result;
		}
		catch (Exception e) {
			throw new ConfigFileReaderException("ConfigFileReader:Exception reading strings from config file: "+e.getMessage());
		}
	}


	/**
	 * Retrieve a mapping from many entries in the config file.
	 * the attribute 'attrib' is the key for the map, and the value of the element is the value.<br />
	 * <pre> String values from the config file (where each value is in an element of the same name)
	 * example:
	 * ---------
	 * code:
	 * ConfigFileReader cfr = new ConfigFileReader(...whatever...);
	 * private HashMap <String, String> idsToSecrets = new HashMap<String,String>();
	 * idsToSecrets = cfr.getMap("client", "id");
	 * populates idsToSecrets with these entries (Key,Value):
	 * (client1,12345)
	 * (client2,dfwop24ur90uqw)
	 * ---------
	 * config file:
	 * &lt;client id="client1"&gt;12345&lt;/client&gt;
	 * &lt;client id="client2"&gt;dfwop24ur90uqw&lt;/client&gt;
	 * ---------
	 * 
	 * @param elementName the name of the element containing the desired values
	 * @param continueIfPossible try to read the map even if some entries cannot be read - skip them (e.g. an element lacks a properly-named attribute, meaning its entry can't be added to the map)
	 * @throws ConfigFileReaderException if there are XML problems but AND continueOnError is false, or if some other exception occurs preventing the file from being read (e.g. the file does not exist)
	 */
	public HashMap<String,String> getMap(String elementName, String attributeName, boolean continueIfPossible)
	throws ConfigFileReaderException {
		try {
			HashMap <String,String> result = new HashMap <String,String> ();
			List <Element> l = rootE.getChildren(elementName);
			Iterator <Element> it = l.iterator();
			while (it.hasNext()) {
				Element e = it.next();
				String key = e.getAttributeValue(attributeName);
				if (key == null) {
					if (continueIfPossible)
						continue;
					else
						throw new Exception("Element "+e.toString()+" lacks an attribute '"+attributeName+"'");
				}
				String value = e.getText();
				result.put(key,value);
			}
			return result;
		}
		catch (Exception e) {
			throw new ConfigFileReaderException("ConfigFileReader:Exception reading map from config file: "+e.getMessage());
		}
	}

	/**
	 * Retrieve a mapping from many entries in the config file, picking up each element with the given name, and ALL its attributes.
	 * There may be multiple items for each value within an element.
	 * String values from the config file (where each value is in an element of the same name).
	 * example:
	 * <pre>
	 * ---------
	 * code:
	 *  ConfigFileReader cfr = new ConfigFileReader(...whatever...);
	 *  private HashMap <String, HashMap<String,String>> namesToPreferences = new HashMap<String,HashMap<String,String>>();
	 *  namesToPreferences = cfr.getMap("client");
	 * ---------
	 * populates idsToSecrets with these entries (Key,(attrib,value)):
	 *  (client1,(car,Honda; wife,Glenda); (car,Buick))
	 *  (client2,())
	 * ---------
	 * given this config file:
	 *  &lt;client car="Honda" wife="Glenda"&gt;client1&lt;/client&gt;
	 *  &lt;client car="Buick"&gt;client1&lt;/client&gt;
	 *  &lt;client&gt;client2&lt;/client&gt;
	 * ---------
	 * </pre>
	 * 
	 * @param elementName the name of the element containing the desired values
	 * @throws ConfigFileReaderException if there are XML problems but AND continueOnError is false, or if some other exception occurs preventing the file from being read (e.g. the file does not exist)
	 */
	public HashMap<String,ArrayList<HashMap<String,String>>> getMaps(String elementName)
	throws ConfigFileReaderException {
		try {
			HashMap <String,ArrayList<HashMap<String,String>>> result = new HashMap <String,ArrayList<HashMap<String,String>>> ();

			List <Element> l = rootE.getChildren(elementName);
			Iterator <Element> it = l.iterator();
			while (it.hasNext()) {
				Element e = it.next();
				String key = e.getText();
				// build the table of attributes
				List <Attribute> attributes = e.getAttributes();
				HashMap <String,String> resultAttributes = new HashMap<String,String>();
				for (Attribute a : attributes) {
					resultAttributes.put(a.getName(),a.getValue());
				}
				if (result.containsKey(key)) {
					ArrayList <HashMap<String,String>> al = result.get(key);
					al.add(resultAttributes);
				}
				else {
					ArrayList <HashMap<String,String>> al = new ArrayList<HashMap<String,String>>();
					al.add(resultAttributes);
					result.put(key,al);
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new ConfigFileReaderException("ConfigFileReader:Exception reading map from config file: "+e.getMessage());
		}
	}



	/**
	 * for testing
	 * @param args
	 */
	public static void main(String[] args)
	throws Exception {
		File f = new File("/Users/jim/Projects/RandD_Projects/ConfigFileReader/config.xml");
		System.out.println("test cases will use this config file:"+f.getPath());
		ConfigFileReader cfr = new ConfigFileReader(f,"section1");


		System.out.println("Trying to fetch config file with non-existent root element called 'section1XXXZ'. Should throw ElementNotFoundException");
		try {
			new ConfigFileReader(f,"section1XXXZ");
			System.out.println("ERROR: ElementNotFoundException was not thrown");
		}
		catch (ElementNotFoundException e) {
			System.out.println("OK: ElementNotFoundException thrown as expected:"+e.getMessage());
		}
		
		System.out.println("trying to fetch String element called 'element1a':"+cfr.getString("",false,"element1a"));
		System.out.println("trying to fetch String element called 'element1b':"+cfr.getString("",false,"element1b"));
		System.out.println("trying to fetch Integer element called 'element1d':"+cfr.getInt(0,false,"element1d"));

		System.out.println("trying to fetch String element called 'element1z (should throw exception)':");
		try {
			System.out.println(cfr.getString("",false,"element1z"));
			System.out.println("ERROR: Exception was not thrown");
		}
		catch (ConfigFileReaderException e) {
			System.out.println("OK: Exception thrown as expected");
		}

		// test an error case
		//		System.out.println("trying to fetch element called 'errorfive', should return default value of '500':"+cfr.getInt(500,true,"errorfive"));
		//		System.out.println("trying to fetch element called 'errorfive, should throw exception:");
		//		System.out.println(cfr.getInt(500,false,"errorfive"));

		// testing the getMap() method only
		System.out.println("testing getMap with an element called 'client' and an attribute named 'id' from 'section3' of the test file");
		ConfigFileReader cfr2 = new ConfigFileReader(f,"section3");
		System.out.println("attempting to load the map");
		HashMap <String,String> hm = cfr2.getMap("client", "id", true);
		System.out.println("Map:\n"+hm);
		
		// testing the FULL getMap() method only
		System.out.println("testing getMap with an element called 'client' and all attributes from 'section3b' of the test file");
		ConfigFileReader cfr2b = new ConfigFileReader(f,"section3b");
		System.out.println("attempting to load the map");
		HashMap<String,ArrayList<HashMap<String,String>>> hm2 = cfr2b.getMaps("client");
		System.out.println("Map:\n"+hm2);

		System.out.println("Testing the stepping feature 'stepInto' with 'section4'- there are (3) entries labeled 'subsection'");
		ConfigFileReader cfr4 = new ConfigFileReader(f,"section4");
		cfr4.stepInto("subsection");
		while (cfr4.hasNext()) {
			cfr4.stepToNext();
			System.out.println("name:"+cfr4.getString("name"));
			System.out.println("age:"+cfr4.getInt("age"));
		}
		cfr.stepInto(null);
	}


}
