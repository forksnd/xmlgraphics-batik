/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.dom.util;

import org.apache.batik.util.XMLUtilities;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * A collection of utility functions for the DOM.
 *
 * @author <a href="mailto:stephane@hillion.org">Stephane Hillion</a>
 * @version $Id$
 */
public class DOMUtilities extends XMLUtilities {
    /**
     * Do not need to be instantiated.
     */
    protected DOMUtilities() {
    }

    /**
     * Deep clones a document using the given DOM implementation.
     */
    public static Document deepCloneDocument(Document doc, DOMImplementation impl) {
        Element root = doc.getDocumentElement();
        Document result = impl.createDocument(root.getNamespaceURI(),
                                              root.getNodeName(),
                                              null);
        Element rroot = result.getDocumentElement();
        boolean before = true;
        for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n == root) {
                before = false;
                if (root.hasAttributes()) {
                    NamedNodeMap attr = root.getAttributes();
                    int len = attr.getLength();
                    for (int i = 0; i < len; i++) {
                        rroot.setAttributeNode((Attr)deepCloneNode(attr.item(i),
                                                                   result));
                    }
                }
                deepCloneChildren(root, rroot, result);
            } else {
                if (n.getNodeType() != Node.DOCUMENT_TYPE_NODE) {
                    if (before) {
                        result.insertBefore(deepCloneNode(n, result), rroot);
                    } else {
                        result.appendChild(deepCloneNode(n, result));
                    }
                }
            }
        }
        return result;
    }

    /**
     * Deep clones the given node, using the given document as factory.
     */
    protected static Node deepCloneNode(Node n, Document doc) {
        switch (n.getNodeType()) {
        case Node.ELEMENT_NODE:
            Node res = doc.createElementNS(n.getNamespaceURI(), n.getNodeName());
            deepCloneChildren(n, res, doc);
            if (n.hasAttributes()) {
                NamedNodeMap attr = n.getAttributes();
                int len = attr.getLength();
                Element elt = (Element)res;
                for (int i = 0; i < len; i++) {
                    elt.setAttributeNode((Attr)deepCloneNode(attr.item(i), doc));
                }
            }
            return res;
        case Node.ATTRIBUTE_NODE:
            res = doc.createAttributeNS(n.getNamespaceURI(), n.getNodeName());
            deepCloneChildren(n, res, doc);
            return res;
        case Node.TEXT_NODE:
            return doc.createTextNode(n.getNodeValue());
        case Node.CDATA_SECTION_NODE:
            return doc.createCDATASection(n.getNodeValue());
        case Node.ENTITY_REFERENCE_NODE:
            res = doc.createEntityReference(n.getNodeName());
            deepCloneChildren(n, res, doc);
            return res;
        case Node.PROCESSING_INSTRUCTION_NODE:
            return doc.createProcessingInstruction(n.getNodeName(), n.getNodeValue());
        case Node.COMMENT_NODE:
            return doc.createComment(n.getNodeValue());
        default:
            throw  new Error("Internal error (" + n.getNodeType() + ")");
        }
    }

    /**
     * Clones the children of the source node and appends them to the
     * destination node.
     */
    protected static void deepCloneChildren(Node source, Node dest, Document doc) {
        for (Node n = source.getFirstChild(); n != null; n = n.getNextSibling()) {
            dest.appendChild(deepCloneNode(n, doc));
        }
    }

    /**
     * Tests whether the given string is a valid name.
     */
    public static boolean isValidName(String s) {
	char c = s.charAt(0);
        int d = c / 32;
        int m = c % 32;
	if ((NAME_FIRST_CHARACTER[d] & (1 << m)) == 0) {
	    return false;
	}
	int len = s.length();
	for (int i = 1; i < len; i++) {
	    c = s.charAt(i);
	    d = c / 32;
	    m = c % 32;
	    if ((NAME_CHARACTER[d] & (1 << m)) == 0) {
		return false;
	    }
	}
	return true;
    }
    
    /**
     * Tests whether the given string is a valid prefix.
     * This method assume that isValidName(s) is true.
     */
    public static boolean isValidPrefix(String s) {
	return s.indexOf(':') == -1;
    }

    /**
     * Gets the prefix from the given qualified name.
     * This method assume that isValidName(s) is true.
     */
    public static String getPrefix(String s) {
	int i = s.indexOf(':');
	return (i == -1 || i == s.length()-1)
	    ? null
	    : s.substring(0, i);
    }
    
    /**
     * Gets the local name from the given qualified name.
     * This method assume that isValidName(s) is true.
     */
    public static String getLocalName(String s) {
	int i = s.indexOf(':');
	return (i == -1 || i == s.length()-1)
	    ? s
	    : s.substring(i + 1);
    }

    /**
     * Parses a 'xml-stylesheet' processing instruction data section and
     * puts the pseudo attributes in the given table.
     */
    public static void parseStyleSheetPIData(String data, HashTable table) {
        // !!! Internationalization
	char c;
	int i = 0;
	// Skip leading whitespaces
	while (i < data.length()) {
	    c = data.charAt(i);
	    if (!XMLUtilities.isXMLSpace(c)) {
		break;
	    }
	    i++;
	}
	while (i < data.length()) {
	    // Parse the pseudo attribute name
	    c = data.charAt(i);
	    int d = c / 32;
	    int m = c % 32;
	    if ((NAME_FIRST_CHARACTER[d] & (1 << m)) == 0) {
		throw new DOMException(DOMException.INVALID_CHARACTER_ERR,
				       "Wrong name initial:  " + c);
	    }
	    StringBuffer ident = new StringBuffer();
	    ident.append(c);
	    while (++i < data.length()) {
		c = data.charAt(i);
		d = c / 32;
		m = c % 32;
		if ((NAME_CHARACTER[d] & (1 << m)) == 0) {
		    break;
		}
		ident.append(c);
	    }
	    if (i >= data.length()) {
		throw new DOMException(DOMException.SYNTAX_ERR,
				       "Wrong xml-stylesheet data: " + data);
	    }
	    // Skip whitespaces
	    while (i < data.length()) {
		c = data.charAt(i);
		if (!XMLUtilities.isXMLSpace(c)) {
		    break;
		}
		i++;
	    }
	    if (i >= data.length()) {
		throw new DOMException(DOMException.SYNTAX_ERR,
				       "Wrong xml-stylesheet data: " + data);
	    }
	    // The next char must be '='
	    if (data.charAt(i) != '=') {
		throw new DOMException(DOMException.SYNTAX_ERR,
				       "Wrong xml-stylesheet data: " + data);
	    }
	    i++;
	    // Skip whitespaces
	    while (i < data.length()) {
		c = data.charAt(i);
		if (!XMLUtilities.isXMLSpace(c)) {
		    break;
		}
		i++;
	    }
	    if (i >= data.length()) {
		throw new DOMException(DOMException.SYNTAX_ERR,
				       "Wrong xml-stylesheet data: " + data);
	    }
	    // The next char must be '\'' or '"'
	    c = data.charAt(i);
	    i++;
	    StringBuffer value = new StringBuffer();
	    if (c == '\'') {
		while (i < data.length()) {
		    c = data.charAt(i);
		    if (c == '\'') {
			break;
		    }
		    value.append(c);
		    i++;
		}
		if (i >= data.length()) {
		    throw new DOMException(DOMException.SYNTAX_ERR,
					   "Wrong xml-stylesheet data: " +
                                           data);
		}
	    } else if (c == '"') {
		while (i < data.length()) {
		    c = data.charAt(i);
		    if (c == '"') {
			break;
		    }
		    value.append(c);
		    i++;
		}
		if (i >= data.length()) {
		    throw new DOMException(DOMException.SYNTAX_ERR,
					   "Wrong xml-stylesheet data: " +
                                           data);
		}
	    } else {
		throw new DOMException(DOMException.SYNTAX_ERR,
				       "Wrong xml-stylesheet data: " + data);
	    }
	    table.put(ident.toString().intern(), value.toString());
	    i++;
	    // Skip whitespaces
	    while (i < data.length()) {
		c = data.charAt(i);
		if (!XMLUtilities.isXMLSpace(c)) {
		    break;
		}
		i++;
	    }
	}
    }
}
