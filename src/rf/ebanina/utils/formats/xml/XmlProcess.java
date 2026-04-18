package rf.ebanina.utils.formats.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.TreeMap;

public class XmlProcess {
    public static TreeMap<String, String> parseXmlToTreeMap(String xmlString) throws Exception {
        TreeMap<String, String> map = new TreeMap<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        byte[] bytes = xmlString.getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

        Document doc = builder.parse(inputStream);
        Element root = doc.getDocumentElement();
        NodeList nodeList = root.getChildNodes();

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String key   = element.getTagName();
                String value = element.getTextContent().trim();
                map.put(key, value);
            }
        }

        return map;
    }

    public static TreeMap<String, String> parseXmlToTreeMap(InputStream inputStream) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(inputStream);

        Element root = doc.getDocumentElement();
        NodeList nodeList = root.getChildNodes();

        TreeMap<String, String> map = new TreeMap<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String key   = element.getTagName();
                String value = element.getTextContent().trim();
                map.put(key, value);
            }
        }

        return map;
    }
}
