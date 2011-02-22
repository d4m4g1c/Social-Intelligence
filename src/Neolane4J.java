import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import javax.xml.namespace.QName;
import javax.xml.soap.*;

import org.w3c.dom.NodeList;


/**
 * Classe gérant les requêtes SQL par SOAP pour Neolane.
 * @author David
 *
 */
public class Neolane4J {
	
	/*-------------*/
	/*  SINGLETON  */
	/*-------------*/
	
	private static final Neolane4J INSTANCE = new Neolane4J();
	
	private Neolane4J() {
	}
	
	/**
	 * Donne une instance de Neolane4J.
	 * @return une instance Neolane4J
	 */
	public static Neolane4J getInstance() {
		return INSTANCE;
	}
	
	/*--------------*/
	/*  PARAMETERS  */
	/*--------------*/
	
	/**
	 * Le token de session permettant de se logguer.
	 */
	public String SESSION_TOKEN = "admin/KuejLyn3";
	
	/**
	 * L'adresse du routeur SOAP de Neolane.
	 */
	public String SOAP_ROUTER = "http://ecp-social.neolane.net/nl/jsp/soaprouter.jsp";
	
	/*-------------*/
	/*  UTILITIES  */
	/*-------------*/
	
	/**
	 * Permet de créer un message SOAP de base.
	 * @return un message SOAP de base
	 * @throws SOAPException
	 */
	private SOAPMessage newSOAPMessage() throws SOAPException {
		SOAPMessage soapMsg = MessageFactory.newInstance().createMessage();
		SOAPEnvelope soapEnv = soapMsg.getSOAPPart().getEnvelope();
		
		soapEnv.getHeader().detachNode();
		soapEnv.addAttribute(new QName("", "xsd", "xmlns"), "http://www.w3.org/2001/XMLSchema");
		soapEnv.addAttribute(new QName("", "xsi", "xmlns"), "http://www.w3.org/2001/XMLSchema-instance");
		soapEnv.addAttribute(new QName("", "ns", "xmlns"), "http://xml.apache.org/xml-soap");
		return soapMsg;
	}
	
	/**
	 * Ouvre une connexion et envoie un message SOAP.
	 * @param soapMsg	Le message à envoyer 
	 * @throws SOAPException
	 */
	private SOAPBody send(SOAPMessage soapMsg) throws SOAPException {
		
		try {// to be removed after testing
			FileOutputStream test = new FileOutputStream("test.xml");
			soapMsg.writeTo(test); // generated xml document
		} catch (Exception e) {}
		
		SOAPConnection connection = SOAPConnectionFactory.newInstance().createConnection();
		
		SOAPMessage response = null;
		try {
			URL endpoint = new URL(SOAP_ROUTER);
			response = connection.call(soapMsg, endpoint);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		try {// to be removed after testing
			FileOutputStream test2 = new FileOutputStream("test2.xml");
			response.writeTo(test2);
		} catch (IOException e) {
			e.printStackTrace();
		} // received xml document
		
		connection.close();
		SOAPBody soapBody = response.getSOAPBody();
		
		if (soapBody.hasFault()) {
			System.err.println(soapBody.getFault().getFaultString() + "\n"
					+ soapBody.getFault().getDetail().getTextContent());
			return null;
		}
		
		return soapBody;
	}
	
	
	/*------------*/
	/*   SELECT   */
	/*------------*/
	
	/**
	 * Construit un message de base pour faire un select.
	 * @param soapMsg
	 * @param namespace
	 * @param schema
	 * @param params
	 * @return un message de base pour select
	 * @throws SOAPException
	 * @throws IOException
	 */
	private SOAPElement select(SOAPMessage soapMsg,
			String namespace, String schema, String[] params)
			throws SOAPException, IOException {
		
		soapMsg.getMimeHeaders().addHeader("SOAPAction", "xtk:queryDef#ExecuteQuery");
		SOAPBody soapBody = soapMsg.getSOAPPart().getEnvelope().getBody();
		
		SOAPBodyElement bodyElement = soapBody.addBodyElement(new QName("urn:xtk:queryDef", "ExecuteQuery"));
		bodyElement.setEncodingStyle("http://schemas.xmlsoap.org/soap/encoding/");
		
		SOAPElement token = bodyElement.addChildElement(new QName("__sessiontoken"));
		token.addAttribute(new QName("", "type", "xsi"), "xsd:String");
		token.addTextNode(SESSION_TOKEN);
		
		SOAPElement entity = bodyElement.addChildElement(new QName("entity"));
		entity.addAttribute(new QName("", "type", "xsi"), "ns:Element");
		entity.setEncodingStyle("http://xml.apache.org/xml-soap/literalxml");
		
		SOAPElement queryDef = entity.addChildElement(new QName("queryDef"));
		queryDef.addAttribute(new QName("schema"), namespace + ":" + schema);
		queryDef.addAttribute(new QName("operation"), "select");
		
		SOAPElement select = queryDef.addChildElement(new QName("select"));
		for (int i=0; i<params.length; i++) {
			select.addChildElement(new QName("node")).addAttribute(new QName("expr"), "@" + params[i]);
		}
		
		return queryDef;
	}
	
	/**
	 * Ajoute une clause WHERE.
	 * @param queryDef
	 * @param whereParams
	 * @throws SOAPException
	 * @throws IOException
	 */
	private void selectWhere(SOAPElement queryDef, String[] whereParams)
			throws SOAPException, IOException {
		
		SOAPElement where = queryDef.addChildElement(new QName("where"));
		for (int i=0; i<whereParams.length; i++) {
			where.addChildElement(new QName("condition")).addAttribute(new QName("expr"), "@" + whereParams[i]);
		}
	}
	
	/**
	 * Ajoute une clause ORDERBY.
	 * @param queryDef
	 * @param orderByParams
	 * @throws SOAPException
	 * @throws IOException
	 */
	private void selectOrderBy(SOAPElement queryDef, String[] orderByParams)
			throws SOAPException, IOException {
		
		SOAPElement where = queryDef.addChildElement(new QName("orderBy"));
		for (int i=0; i<orderByParams.length; i++) {
			where.addChildElement(new QName("node")).addAttribute(new QName("expr"), "@" + orderByParams[i]);
		}
	}
	
	/**
	 * Permet de lire le corps SOAP et d'en extraire les résultats.
	 * @param soapBody
	 * @return une NodeList représentant les résultats
	 * @throws SOAPException
	 */
	private NodeList readSelect(SOAPBody soapBody)
			throws SOAPException {
		
		if (soapBody == null) {
			System.err.println("Pas de SOAPBody à lire!");
			return null;
		} else {
			org.w3c.dom.Node resultCollection = soapBody.getChildNodes().item(0).getChildNodes().item(0).getChildNodes().item(0);
			return resultCollection.hasChildNodes() ? resultCollection.getChildNodes() : null;
		}
	}
	
	private ArrayList<Hashtable<String, String>> readSelectAsHastable(SOAPBody soapBody)
			throws SOAPException {
		
		NodeList results = readSelect(soapBody);
		for (int i=0; i<results.getLength(); i++) {
			System.out.println(results.item(i).getAttributes().item(0));
		}
		return null;
	}
	
	/**
	 * Enumération sur le type de clause.
	 * @author David
	 *
	 */
	public enum RequestType{WHERE, ORDERBY};
	
	/**
	 * Permet de faire un select.
	 * @param namespace
	 * @param schema
	 * @param params
	 * @throws SOAPException
	 * @throws IOException
	 */
	public void select(String namespace, String schema, String[] params)
			throws SOAPException, IOException {
		
		SOAPMessage soapMsg = newSOAPMessage();
		select(soapMsg, namespace, schema, params);
		
		readSelect(send(soapMsg));
	}
	
	/**
	 * Permet de faire un select en ajoutant une clause.
	 * @param namespace
	 * @param schema
	 * @param params
	 * @param type		Précise le type de clause
	 * @param params2	Arguments de la clause
	 * @throws SOAPException
	 * @throws IOException
	 */
	public void select(String namespace, String schema, String[] params,
			RequestType type, String[] params2)
			throws SOAPException, IOException {
		
		SOAPMessage soapMsg = newSOAPMessage();
		SOAPElement queryDef = select(soapMsg, namespace, schema, params);
		
		switch (type) {
		case WHERE:
			selectWhere(queryDef, params2);
			break;
		case ORDERBY:
			selectOrderBy(queryDef, params2);
			break;
		}
		
		readSelect(send(soapMsg));
	}
	
	/**
	 * Permet de faire un select avec plusieurs clauses.
	 * @param namespace
	 * @param schema
	 * @param params
	 * @param whereParams	Arguments de la clause WHERE
	 * @param orderByParams	Arguments de la clause ORDERBY
	 * @throws SOAPException
	 * @throws IOException
	 */
	public void select(String namespace, String schema, String[] params,
			String[] whereParams, String[] orderByParams)
			throws SOAPException, IOException {
		
		SOAPMessage soapMsg = newSOAPMessage();
		SOAPElement queryDef = select(soapMsg, namespace, schema, params);
		selectWhere(queryDef, whereParams);
		selectOrderBy(queryDef, orderByParams);
		
		readSelect(send(soapMsg));
	}
	
	
	/*-------------*/
	/*    WRITE    */
	/*-------------*/
	
	/**
	 * Construit un message de base pour faire un write.
	 * @param soapMsg
	 * @param namespace
	 * @param schema
	 * @param params
	 * @param values
	 * @return
	 * @throws SOAPException
	 * @throws IOException
	 */
	private SOAPElement write(SOAPMessage soapMsg,
			String namespace, String schema, String[] params, String[] values)
			throws SOAPException, IOException {
		
		soapMsg.getMimeHeaders().addHeader("SOAPAction", "xtk:persist#Write");
		SOAPBody soapBody = soapMsg.getSOAPPart().getEnvelope().getBody();
		
		SOAPBodyElement bodyElement = soapBody.addBodyElement(new QName("urn:xtk:persist", "Write"));
		bodyElement.setEncodingStyle("http://schemas.xmlsoap.org/soap/encoding/");
		
		SOAPElement token = bodyElement.addChildElement(new QName("__sessiontoken"));
		token.addAttribute(new QName("", "type", "xsi"), "xsd:String");
		token.addTextNode(SESSION_TOKEN);
		
		SOAPElement domDoc = bodyElement.addChildElement(new QName("domDoc"));
		domDoc.addAttribute(new QName("", "type", "xsi"), "ns:Element");
		domDoc.setEncodingStyle("http://xml.apache.org/xml-soap/literalxml");
		
		SOAPElement entry = domDoc.addChildElement(new QName(schema));
		entry.addAttribute(new QName("xtkschema"), namespace + ":" + schema);
		for (int i=0; i<params.length; i++) {
			entry.addAttribute(new QName(params[i]), values[i]);
		}
		
		return entry;
	}
	
	/**
	 * Permet de faire un write.
	 * @param namespace
	 * @param schema
	 * @param params
	 * @param values
	 * @throws SOAPException
	 * @throws IOException
	 */
	public void write(String namespace, String schema, String[] params, String[] values)
			throws SOAPException, IOException {
		
		SOAPMessage soapMsg = newSOAPMessage();
		write(soapMsg, namespace, schema, params, values);
		
		send(soapMsg);
	}
	
	/**
	 * Permet de faire un write avec lien.
	 * @param namespace
	 * @param schema
	 * @param params
	 * @param values
	 * @param schema2
	 * @param params2
	 * @param values2
	 * @throws SOAPException
	 * @throws IOException
	 */
	public void write(String namespace, String schema, String[] params, String[] values,
			String schema2, String[] params2, String[] values2)
			throws SOAPException, IOException {
		
		SOAPMessage soapMsg = newSOAPMessage();
		SOAPElement entry = write(soapMsg, namespace, schema, params, values);
		
		SOAPElement entry2 = entry.addChildElement(new QName(schema2));
		for (int i=0; i<params2.length; i++) {
			entry2.addAttribute(new QName(params2[i]), values2[i]);
		}
		
		send(soapMsg);
	}
	
}