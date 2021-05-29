/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.io;

import hep.dataforge.NamedKt;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaNode;
import hep.dataforge.values.Value;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A writer for XML represented Meta
 *
 * @author Alexander Nozik
 */
public class XMLMetaWriter implements MetaStreamWriter {

    @Override
    public void write(@NotNull OutputStream stream, @NotNull Meta meta) {
        try {

            Document doc = getXMLDocument(meta);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();

            //PENDING add constructor customization of writer?
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
//            transformer.setOutputProperty(OutputKeys.METHOD, "text");
//            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(stream));
        } catch (TransformerException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Document getXMLDocument(Meta meta) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element element = getXMLElement(meta, doc);
            doc.appendChild(element);
            return doc;
        } catch (ParserConfigurationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String encodeName(String str) {
        return str.replaceFirst("^(\\d)", "_$1")
                .replace("@", "_at_");
    }

    private Element getXMLElement(Meta meta, Document doc) {
        String elementName;
        if (NamedKt.isAnonymous(meta)) {
            elementName = MetaNode.DEFAULT_META_NAME;
        } else {
            elementName = meta.getName();
        }
        Element res = doc.createElement(encodeName(elementName));


        meta.getValueNames(true).forEach(valueName -> {
            List<Value> valueList = meta.getValue(valueName).getList();
            if (valueList.size() == 1) {
                String value = valueList.get(0).getString();
                if (value.startsWith("[")) {
                    value = "[" + value + "]";
                }
                res.setAttribute(encodeName(valueName), value);
            } else {
                String val = valueList
                        .stream()
                        .map(Value::getString)
                        .collect(Collectors.joining(", ", "[", "]"));
                res.setAttribute(encodeName(valueName), val);
            }
        });

        meta.getNodeNames(true).forEach(nodeName -> {
            List<? extends Meta> elementList = meta.getMetaList(nodeName);
            if (elementList.size() == 1) {
                Element el = getXMLElement(elementList.get(0), doc);
                res.appendChild(el);
            } else {
                for (Meta element : elementList) {
                    Element el = getXMLElement(element, doc);
                    res.appendChild(el);
                }
            }

        });
        return res;
    }
}
