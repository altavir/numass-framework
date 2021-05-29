/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.io;

import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.values.LateParseValue;
import hep.dataforge.values.NamedValue;
import hep.dataforge.values.ValueFactory;
import kotlin.text.Charsets;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static javax.xml.parsers.DocumentBuilderFactory.newInstance;

/**
 * A default reader for XML represented Meta
 *
 * @author <a href="mailto:altavir@gmail.com">Alexander Nozik</a>
 */
public class XMLMetaReader implements MetaStreamReader {
    @Override
    public MetaBuilder read(@NotNull InputStream stream, long length) throws IOException, ParseException {
        try {
            DocumentBuilderFactory factory = newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            InputSource source;
            if (length < 0) {
                source = new InputSource(new InputStreamReader(stream, Charsets.UTF_8.newDecoder()));
            } else {
                byte[] bytes = new byte[(int) length];
                stream.read(bytes);
                source = new InputSource(new ByteArrayInputStream(bytes));
            }

            Element element = builder.parse(source).getDocumentElement();
            return buildNode(element);
        } catch (SAXException | ParserConfigurationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private MetaBuilder buildNode(Element element) {
        MetaBuilder res = new MetaBuilder(decodeName(element.getTagName()));
        List<NamedValue> values = getValues(element);
        List<Element> elements = getElements(element);

        for (NamedValue value : values) {
            res.putValue(decodeName(value.getName()), value.getAnonymous());
        }

        for (Element e : elements) {
            //Оставляем только те элементы, в которых есть что-то кроме текста.
            //Те, в которых только текст уже посчитаны как значения
            if (e.hasAttributes() || e.getElementsByTagName("*").getLength() > 0) {
                res.putNode(buildNode(e));
            }
        }

        //записываем значения только если нет наследников
        if (!element.getTextContent().isEmpty() && (element.getElementsByTagName("*").getLength() == 0)) {
            res.putValue(decodeName(element.getTagName()), element.getTextContent());
        }
        //res.putContent(new AnnotatedData("xmlsource", element));

        return res;
    }

    /**
     * Возвращает список всех подэлементов
     *
     * @param element
     * @return
     */
    private List<Element> getElements(Element element) {
        List<Element> res = new ArrayList<>();
        NodeList nodes = element.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element elNode = (Element) nodes.item(i);
            if (elNode.getParentNode().equals(element)) {
                res.add(elNode);
            }
        }
        return res;
    }

    private List<NamedValue> getValues(Element element) {
        List<NamedValue> res = new ArrayList<>();
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node node = attributes.item(i);
            String name = node.getNodeName();
            res.add(new NamedValue(name, new LateParseValue(normalizeValue(node.getNodeValue()))));
        }

        List<Element> elements = getElements(element);
        for (Element elNode : elements) {
            if (!(elNode.getElementsByTagName("*").getLength() > 0 || elNode.hasAttributes())) {
                String name = elNode.getTagName();
                if (elNode.getTextContent().isEmpty()) {
                    res.add(new NamedValue(name, ValueFactory.of(Boolean.TRUE)));
                } else {
                    res.add(new NamedValue(name, new LateParseValue(elNode.getTextContent())));
                }

            }
        }
        return res;

    }

    private String normalizeValue(String value) {
        return value.replace("\\n", "\n");
    }

    private String decodeName(String str) {
        return str.replaceFirst("^_(\\d)", "$1")
                .replace("_at_", "@");
    }
}
