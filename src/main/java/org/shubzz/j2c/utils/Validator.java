package org.shubzz.j2c.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class Validator {

    public static boolean validateJacocoFilePath(String jacocoFile) {
        Path of = Path.of(jacocoFile);
        if (!Files.exists(of, LinkOption.NOFOLLOW_LINKS)) {
            System.out.println("Jacoco file does not exist at this location");
            return false;
        }
        if (!Files.isReadable(of)) {
            System.out.println("Jacoco file have no read access.");
            return false;
        }
        if (!validateJacocoXmlUsingDTD(jacocoFile)) {
            System.out.println("Invalid Jacoco.xml file.");
            return false;
        }
        return true;
    }

    private static boolean validateJacocoXmlUsingDTD(String jacocoFile) {
        String jacocoTarget = null;
        try {
            ClassLoader classLoader = Validator.class.getClassLoader();
            InputStream jp = classLoader.getResourceAsStream("report.dtd");

            jacocoTarget = copyFile(jacocoFile, jp);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(true);
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException exception) throws SAXException {
                    throw new SAXException();
                }

                @Override
                public void fatalError(SAXParseException exception) throws SAXException {
                    throw new SAXException();
                }

                @Override
                public void error(SAXParseException exception) throws SAXException {
                    throw new SAXException();
                }
            });
            builder.setEntityResolver((publicId, systemId) -> {
                if ((systemId != null && !systemId.contains("report.dtd")) || (publicId != null
                        && !publicId.contains("-//JACOCO//DTD Report"))) {
                    System.out.println("PUBLIC ID : " + publicId + " SYSTEM ID : " + systemId);
                    throw new IOException();
                } else {
                    return null;
                }
            });
            builder.parse(new InputSource(jacocoTarget));
            return true;
        } catch (ParserConfigurationException | IOException | SAXException se) {
            System.out.println("Error Occurred :- " + se.getMessage());
            return false;
        } finally {
            try {
                if (jacocoTarget != null && !jacocoFile.trim().isBlank()) {
                    try (Stream<Path> walk = Files.walk(Paths.get(jacocoTarget).getParent())) {
                        walk.sorted(Comparator.reverseOrder())
                                .map(Path::toFile)
                                .forEach(File::delete);
                    }
                }
            } catch (IOException e) {
                System.out.println();
            }
        }
    }


    private static String copyFile(String jacocoFile, InputStream dtdFile) {
        String randomString = Long.toHexString(Double.doubleToLongBits(Math.random()));

        Path jacocoSrc = Paths.get(jacocoFile);
        Path targetDir = Paths.get(System.getProperty("java.io.tmpdir") + "/" + randomString);
        Path jacocoTarget = targetDir.resolve(jacocoSrc.getFileName());
        Path dtdTarget = targetDir.resolve("report.dtd");
        try {
            Files.createDirectory(targetDir);
            Files.copy(jacocoSrc, jacocoTarget);
            Files.copy(dtdFile, dtdTarget);
        } catch (IOException e) {
            System.out.println("TE500 - Some Error Occurred.");
        }
        return jacocoTarget.toString();
    }
}
