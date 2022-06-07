package org.shubzz.j2c;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.shubzz.j2c.core.XMLMain;
import org.shubzz.j2c.utils.Validator;
import org.xml.sax.SAXException;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "Jacoco2Cobertura",
        mixinStandardHelpOptions = true,
        version = "Jacoco2Cobertura 1.0.0",
        description = "Convert Jacoco xml file to Cobertura xml.")
public class Jacoco2Cobertura implements Runnable {


    @Option(
            required = true,
            names = {"-j", "--jacoco"},
            description = "Path to the jacoco xml file.",
            arity = "1..*")
    private String jacocoFile;

    @Option(
            names = {"-d", "--destination"},
            description = "path to save Cobertura xml file.",
            defaultValue = ".")
    private String destination;

    @Option(
            required = true,
            names = {"-s", "--source"},
            description =
                    "Path to java source root. If program have more than one src directory pass all with comma seperated",
            arity = "1..*",
            split = ",")
    private String[] source_roots;

    public Jacoco2Cobertura() {
    }

    public static void main(String[] args) {
        new CommandLine(new Jacoco2Cobertura()).execute(args);
    }

    @Override
    public void run() {
        if (Validator.validateJacocoFilePath(jacocoFile)) {
            System.out.println("XML file Validated");
            XMLMain xmlMain = new XMLMain(jacocoFile, destination, source_roots);
            try {
                xmlMain.init();
                xmlMain.createCoberturaCoverageRootTag();
            } catch (TransformerException | ParserConfigurationException | IOException |
                     SAXException e) {
                System.out.println(
                        "Some Error Occurred. Please try again if problem persist please raise an issue in Github");
                throw new RuntimeException(e);
            }
        }
    }
}
