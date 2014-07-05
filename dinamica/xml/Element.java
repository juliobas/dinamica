package dinamica.xml;

import java.util.HashMap;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import dinamica.StringUtil;

/**
 * Esta clase representa a un nodo o elemento del documento XML,
 * encapsula el acceso a un objeto de tipo Node del DOM API.
 * <br><br>
 * Framework Dinamica - www.martincordova.com<br>
 * Distribuido bajo licencia LGPL<br>
 * (c) 2008 Martin Cordova y Asociados C.A.<br>
 * @author martin.cordova@gmail.com
 */

public class Element {
	
	Node _node = null;
	
	/**
	 * Se inicializa con un Nodo DOM
	 * @param node
	 */
	public Element(Node node) {
		_node = node;
	}

	/**
	 * Retorna el valor del nodo, sin caracteres TAB, CR ni espacios a los lados. 
	 * @return
	 */
	public String getValue() {
		if (_node.getFirstChild()==null)
			return null;
		String value = _node.getFirstChild().getNodeValue();
		value = StringUtil.replace(value, "\t", "");
		value = StringUtil.replace(value, "\n", "");
		value = value.trim();
		return value;		
	}
	
	/**
	 * Exte metodo existe solo para mantener compatibilidad con el API Electric XML
	 * usado anteriormente, es otra manera de llamar a getValue(). Aplican las mismas
	 * condiciones.
	 * @return
	 */
	public String getString() {
		return getValue();
	}
	
	/**
	 * Retorna el valor de un atributo del elemento
	 * @param name Nombre del atributo
	 * @return El valor o NULL si el atributo no tiene valor o no existe
	 */
	public String getAttribute(String name) {
		String value = ((org.w3c.dom.Element)_node).getAttribute(name);
		if (value.trim().equals(""))
			value = null;
		return value;
	}
	
	/**
	 * Retorna un HashMap con los valores de los atributos y sus nombres.
	 * El nombre se usa como indice en el HashMap.
	 * @return HashMap conteniendo la lista de atributos y sus valores
	 */
	public HashMap<String, String> getAttributes() {
		HashMap<String, String> a = new HashMap<String, String>(5);
		NamedNodeMap n = _node.getAttributes();
		for (int i = 0; i < n.getLength(); i++) {
			a.put(n.item(i).getNodeName(), n.item(i).getNodeValue());
		}
		return a;
	}
	
	public String getNodeName() {
		return _node.getNodeName();
	}
	
	/**
	 * Retorna el objeto DOM de tipo Node
	 * @return Un nodo DOM
	 */
	public Node getNode() {
		return _node;
	}
	
	/**
	* Retorna un array con los nombres de los nodos hijos de este elemento.
	* @return Array con los nombres de los nodos hijos
	*/
	public String[] getChildNames()
	{
		NodeList nodes = getNode().getChildNodes();
		if (nodes==null) 
			return null;
		else {
			int nlen = nodes.getLength();
			String nodeName[] = new String[nlen];
			for (int i = 0; i < nlen; i++) {
				Node n =  nodes.item(i);
				nodeName[i] = n.getNodeName();
			}
			return nodeName;
		}
	}	
	
}
