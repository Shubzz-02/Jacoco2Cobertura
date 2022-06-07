package org.shubzz.j2c.core;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XMLMain {

    private final File jacocoFile;

    private final String[] source_roots;

    private final String destination;

    private DocumentBuilderFactory jacocoFactory, coberturaFactory;

    private DocumentBuilder jBuilder, cBuilder;

    private Document jacocoDocument, coberturaDocument;

    private Element jacocoRoot, coberturaRoot;

    public XMLMain(String jacocoFle, String destination, String[] source_roots) {
        this.jacocoFile = new File(jacocoFle);
        this.source_roots = source_roots;
        this.destination = destination;
    }

    public void init() throws ParserConfigurationException, IOException, SAXException {
        jacocoFactory = DocumentBuilderFactory.newInstance();
        jacocoFactory.setValidating(false);
        jacocoFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",
                false);
        jacocoFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        jBuilder = jacocoFactory.newDocumentBuilder();
        jacocoDocument = jBuilder.parse(this.jacocoFile);
        jacocoDocument.getDocumentElement().normalize();
        jacocoRoot = jacocoDocument.getDocumentElement();

        coberturaFactory = DocumentBuilderFactory.newInstance();
        coberturaFactory.setValidating(false);
        cBuilder = coberturaFactory.newDocumentBuilder();
        coberturaDocument = cBuilder.newDocument();
    }


    public void createCoberturaCoverageRootTag() {

        coberturaRoot = coberturaDocument.createElement("coverage");
        coberturaDocument.appendChild(coberturaRoot);

        BigDecimal total = new BigDecimal(
                jacocoRoot.getElementsByTagName("sessioninfo").item(0).getAttributes()
                        .getNamedItem("start").getNodeValue());
        total = total.divide(new BigDecimal("1000"), MathContext.DECIMAL64);

        coberturaRoot.setAttribute("timestamp", total.toString());

        add_counters(jacocoRoot, coberturaRoot);

        createSourcesTag();
        createPackageTag();
        createXml();
    }

    private void createSourcesTag() {
        Element j_sources = coberturaDocument.createElement("sources");
        for (String source_root : source_roots) {
            Element j_source = coberturaDocument.createElement("source");
            j_source.appendChild(coberturaDocument.createTextNode(source_root));
            j_sources.appendChild(j_source);
        }
        coberturaRoot.appendChild(j_sources);
    }

    private void createPackageTag() {
        NodeList j_package = jacocoRoot.getElementsByTagName("package");

        Element c_packages = coberturaDocument.createElement("packages");
        coberturaRoot.appendChild(c_packages);
        for (int i = 0; i < j_package.getLength(); i++) {
            Node node = j_package.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element j_package_node = (Element) node;
                Element c_package = coberturaDocument.createElement("package");
                c_package.setAttribute("name",
                        j_package_node.getAttribute("name").replaceAll("/", "."));

                add_counters(j_package_node, c_package);

                createClassesTag(c_package, j_package_node);
                c_packages.appendChild(c_package);
            }
        }

    }

    private void createClassesTag(Element c_package, Element j_package_node) {

        NodeList j_class = j_package_node.getElementsByTagName("class");

        Element c_classes = coberturaDocument.createElement("classes");
        c_package.appendChild(c_classes);
        for (int i = 0; i < j_class.getLength(); i++) {
            Node node = j_class.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element c_class = coberturaDocument.createElement("class");
                c_class.setAttribute("name",
                        node.getAttributes().getNamedItem("name").getNodeValue()
                                .replaceAll("/", "."));
                c_class.setAttribute("filename",
                        getFileName(node.getAttributes().getNamedItem("name").getNodeValue()));

                add_counters((Element) node, c_class);

                NodeList all_j_lines = getAllLine(c_class.getAttribute("filename"), j_package_node);

                createMethodsTag(c_class, (Element) node, all_j_lines);

                c_class.appendChild(convert_lines(all_j_lines));

                c_classes.appendChild(c_class);
            }
        }
    }

    private void createMethodsTag(Element c_class, Element j_class, NodeList all_j_lines) {

        NodeList j_methods = j_class.getElementsByTagName("method");
        Element c_methods = coberturaDocument.createElement("methods");
        c_class.appendChild(c_methods);

        for (int i = 0; i < j_methods.getLength(); i++) {
            Node node = j_methods.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;

                Element c_method = coberturaDocument.createElement("method");
                c_method.setAttribute("name", element.getAttribute("name"));
                c_method.setAttribute("signature", element.getAttribute("desc"));

                add_counters(element, c_method);

                List<Node> j_method_lines = getMethodLines(element, j_methods, all_j_lines);
                c_method.appendChild(convert_lines(j_method_lines));

                c_methods.appendChild(c_method);
            }
        }

    }

    private Element convert_lines(List<Node> j_lines) {
        Element c_lines = coberturaDocument.createElement("lines");
        for (int i = 0; i < j_lines.size(); i++) {
            Element element = (Element) j_lines.get(i);
            getData(c_lines, element);
        }
        return c_lines;

    }

    private void getData(Element c_lines, Element element) {
        int mb = Integer.parseInt(element.getAttribute("mb"));
        int cb = Integer.parseInt(element.getAttribute("cb"));
        int ci = Integer.parseInt(element.getAttribute("ci"));

        Element c_line = coberturaDocument.createElement("line");
        c_line.setAttribute("number", element.getAttribute("nr"));
        c_line.setAttribute("hits", (ci > 0) ? "1" : "0");
        if (mb + cb > 0) {
            String percentage = (int) (100 * ((double) cb / ((double) cb + (double) mb))) + "%";
            c_line.setAttribute("branch", "true");
            c_line.setAttribute("condition-coverage",
                    percentage + " (" + cb + "/" + (cb + mb) + ")");

            Element conditions = coberturaDocument.createElement("conditions");
            Element condition = coberturaDocument.createElement("condition");
            condition.setAttribute("number", "0");
            condition.setAttribute("type", "jump");
            condition.setAttribute("coverage", percentage);
            conditions.appendChild(condition);
            c_line.appendChild(conditions);
        } else {
            c_line.setAttribute("branch", "false");
        }
        c_lines.appendChild(c_line);
    }

    private Element convert_lines(NodeList j_lines) {

        Element c_lines = coberturaDocument.createElement("lines");
        for (int i = 0; i < j_lines.getLength(); i++) {

            Element element = (Element) j_lines.item(i);
            getData(c_lines, element);
        }
        return c_lines;
    }

    private void add_counters(Element source, Element target) {
        target.setAttribute("line-rate", counter(source, "LINE", '/'));
        target.setAttribute("branch-rate", counter(source, "BRANCH", '/'));
        target.setAttribute("complexity", counter(source, "COMPLEXITY", '+'));

    }

    private String counter(Element source, String type, char op) {
        NodeList j_counter = source.getElementsByTagName("counter");
        Node n_type = null;
        for (int i = 0; i < j_counter.getLength(); i++) {
            Node node = j_counter.item(i);
            if (node.getAttributes().getNamedItem("type").getNodeValue().equalsIgnoreCase(type)) {
                n_type = node;
            }
        }
        double covered = 0f, missed = 0f;

        if (n_type != null) {
            covered = Double.parseDouble(
                    n_type.getAttributes().getNamedItem("covered").getNodeValue());
            missed = Double.parseDouble(
                    n_type.getAttributes().getNamedItem("missed").getNodeValue());
            if (op == '/') {
                return Double.toString(covered / (covered + missed));
            } else {
                return Double.toString(covered + missed);
            }
        } else {
            return "0.0";
        }
    }


    private List<Node> getMethodLines(Element node, NodeList j_methods, NodeList all_j_lines) {
        int start_line = Integer.parseInt(node.getAttribute("line"));
        List<Integer> larger = new ArrayList<>();
        for (int i = 0; i < j_methods.getLength(); i++) {
            Element jm = (Element) j_methods.item(i);
            int in = Integer.parseInt(jm.getAttributes().getNamedItem("line").getNodeValue());
            if (in > start_line) {
                larger.add(in);
            }
        }
        Collections.sort(larger);
        int end_line = (larger.size() == 0) ? 99999999 : larger.get(0);

        List<Node> nodeList = new ArrayList<>();
        for (int i = 0; i < all_j_lines.getLength(); i++) {
            Node j_line = all_j_lines.item(i);
            if (Integer.parseInt(
                    String.valueOf(j_line.getAttributes().getNamedItem("nr").getNodeValue()))
                    >= start_line && Integer.parseInt(
                    String.valueOf(j_line.getAttributes().getNamedItem("nr").getNodeValue()))
                    < end_line) {
                nodeList.add(j_line);
            }
        }
        return nodeList;
    }


    private String getFileName(String name) {
        Matcher matcher = Pattern.compile("([^$]*)").matcher(name);
        if (matcher.lookingAt()) {
            return matcher.group() + ".java";
        } else {
            return name + ".java";
        }
    }

    private NodeList getAllLine(String filename, Element j_package_node) {
        NodeList sourceFile = j_package_node.getElementsByTagName("sourcefile");
        NodeList list = null;
        for (int i = 0; i < sourceFile.getLength(); i++) {
            Element sourceFileNode = (Element) sourceFile.item(i);
            Path p = Path.of(filename);
            if (sourceFileNode.getAttribute("name").equals(p.getFileName().toString())) {
                list = sourceFileNode.getElementsByTagName("line");
            }
        }
        return list;
    }


    private void createXml() {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(coberturaRoot);
            StreamResult result;
            if (destination.endsWith(".xml") || destination.endsWith(".XML")) {
                result = new StreamResult(new File(destination));
                System.out.println("Successfully Generated xml file in :- " + destination);
            } else {
                result = new StreamResult(new File(destination + "/coverage.xml"));
                System.out.println(
                        "Successfully Generated xml file in :- " + destination + "coverage.xml");
            }
            transformer.transform(source, result);

        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }
}
