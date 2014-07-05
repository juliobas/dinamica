package dinamica.xml;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;
import javax.xml.xpath.*;
import java.io.*;

/**
 * Esta clase encapsula el API JAXP/DOM para parsear documentos XML, los
 * cuales pueden ser provistos a la clase ya sea como un InputStream o como
 * un String. Es una clase de conveniencia, para facilitar el uso de estos APIs.
 * <br><br>
 * Framework Dinamica - www.martincordova.com<br>
 * Distribuido bajo licencia LGPL<br>
 * (c) 2008 Martin Cordova y Asociados C.A.<br>
 * @author martin.cordova@gmail.com
 *
 */
public class Document {

	private org.w3c.dom.Document _doc = null; 
	private org.w3c.dom.Element _root = null;
	private XPath _xpath = null;
	
	/**
	 * Cargar y parsear un documento, usa JAXP con DOM, provisto por Java 6.
	 * @param stream Un InputStream, por ejemplo: getContext().getResourceAsStream("/WEB-INF/labels.xml")
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public Document(InputStream stream) throws ParserConfigurationException, SAXException, IOException 
	{
		
        //preparar parser XML estandar (JAXP)
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        dbf.setNamespaceAware(false);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setIgnoringComments(true);
        
        //parsear el documento y crear el objeto XPath para las busquedas
        DocumentBuilder db = dbf.newDocumentBuilder();
        _doc = db.parse(stream);		
        _root = _doc.getDocumentElement();
        _xpath = XPathFactory.newInstance().newXPath(); 
        
	}

	/**
	 * Cargar y parsear un documento, usa JAXP con DOM, provisto por Java 6. Esta variante del constructor
	 * permite pasar un EntityResolver, que es util si esta parseando un web.xml y no quiere perder tiempo
	 * buscando en internet el DTD sino que lo suple localmente.<br>
	 * @param stream Un InputStream, por ejemplo: getContext().getResourceAsStream("/WEB-INF/labels.xml")
	 * @param resolver Una instancia de una clase que implemente la interfaz EntityResolver
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public Document(InputStream stream, EntityResolver resolver) throws ParserConfigurationException, SAXException, IOException 
	{
		
        //preparar parser XML estandar (JAXP)
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        dbf.setNamespaceAware(false);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setIgnoringComments(true);
        
        
        //parsear el documento y crear el objeto XPath para las busquedas
        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setEntityResolver(resolver);
        _doc = db.parse(stream);		
        _root = _doc.getDocumentElement();
        _xpath = XPathFactory.newInstance().newXPath(); 
        
	}
	
	
	/**
	 * Retorna el elemento raíz del documento.
	 * @return El primer elemento que contiene a todos los demas
	 */
	public Element getRoot() {
		return new Element(_root);
	}
	
	/**
	 * Constructor alternativo, recibe un String conteniendo el XML
	 * a ser parseado.
	 * @param document Documento XML a parsear
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public Document(String document) throws ParserConfigurationException, SAXException, IOException {
		this(new ByteArrayInputStream(document.getBytes()));
	}
	
	
	/**
	 * Retorna un elemento dada una expresion XPath o un simple nombre de Tag o Elemento que debe
	 * ser hijo del elemento raiz (root element).
	 * @param tagName nombre del elemento o expresion XPath
	 * @return El elemento si lo encuentra, sino NULL
	 * @throws XPathExpressionException Throwable Si la expresion XPath es incorrecta  
	 */
	public dinamica.xml.Element getElement(String tagName) throws XPathExpressionException
	{
		Node node = (Node)_xpath.evaluate(tagName, _root, XPathConstants.NODE);
		if (node==null) 
			return null;
		else
			return new dinamica.xml.Element(node);
	}

	/**
	 * Analoga al metodo getElement(String tagName), solo que hace la busqueda a partir
	 * de un nodo del documento que se le pasa como parametro.
	 * @param e Nodo o elemento a partir del cual se hace la busqueda
	 * @param tagName nombre del elemento o expresion XPath
	 * @return El elemento si lo encuentra, sino NULL
	 * @throws XPathExpressionException Si la expresion XPath es incorrecta  
	 */
	public dinamica.xml.Element getElement(Element e, String tagName) throws XPathExpressionException 
	{
		Node node = (Node)_xpath.evaluate(tagName, e.getNode(), XPathConstants.NODE);
		if (node==null) 
			return null;
		else
			return new dinamica.xml.Element(node);
	}
	
	/**
	 * Retorna un array de elementos que comparten el mismo nombre.
	 * @param tagName nombre del elemento o expresion XPath
	 * @return El array de elementos si los encuentra, sino NULL
	 * @throws XPathExpressionException Si la expresion XPath es incorrecta
	 */
	
	public dinamica.xml.Element[] getElements(String tagName) throws XPathExpressionException
	{
		NodeList nodes = (NodeList)_xpath.evaluate(tagName, _root, XPathConstants.NODESET);
		if (nodes==null) 
			return null;
		else {
			int nlen = nodes.getLength();
			Element elems[] = new Element[nlen];
			for (int i = 0; i < nlen; i++) {
				elems[i] = new Element(nodes.item(i));
			}
			return elems;
		}
	}
	
	/**
	 * Retorna el objeto nativo del parser XML
	 * que representa al documento, para que pueda
	 * ser manipulado usando el API JAXP/DOM
	 * @return El objeto que referencia al documento DOM
	 */
	public org.w3c.dom.Document getJAXPDocument() {
		return _doc;
	}
	
	/**
	 * Retorna un array de elementos hijos dado el elemento principal.
	 * @param e Elemento principal
	 * @return El array de elementos si los encuentra, sino NULL
	 * @throws XPathExpressionException Si la expresion XPath es incorrecta
	 */
	public dinamica.xml.Element[] getElements(Element e) throws XPathExpressionException
	{
		NodeList nodes = e.getNode().getChildNodes();
		if (nodes==null) 
			return null;
		else {
			int nlen = nodes.getLength();
			Element elems[] = new Element[nlen];
			for (int i = 0; i < nlen; i++) {
				elems[i] = new Element(nodes.item(i));
			}
			return elems;
		}
	}
	
}
