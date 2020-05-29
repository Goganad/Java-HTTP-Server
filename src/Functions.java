import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

public class Functions {

    private final String folderPath;

    private final Request request;
    private Response response = new Response(501);

    public Response getResponse() {
        return response;
    }

    public Functions(Request request, String folderPath) {
        this.request = request;
        this.folderPath = folderPath;
        parseRequest();
    }

    public String getDirectoryStructureXML(String relativePath) throws IOException, ParserConfigurationException, TransformerException {
        String filePathString = folderPath + request.getRelativePath();
        Path filePath = Paths.get(filePathString);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document document = builder.newDocument();

        Element rootElement = document.createElement("root");
        document.appendChild(rootElement);

        Attr pathAttribute = document.createAttribute("path");
        pathAttribute.setValue(relativePath);
        rootElement.setAttributeNode(pathAttribute);

        Files.list(filePath)
                .forEach(path -> {
                    if (Files.isRegularFile(path)) {
                        Element element = document.createElement("file");
                        element.appendChild(document.createTextNode(String.valueOf(path.getFileName())));
                        rootElement.appendChild(element);
                    } else if (Files.isDirectory(path)) {
                        Element element = document.createElement("dir");
                        element.appendChild(document.createTextNode(String.valueOf(path.getFileName())));
                        rootElement.appendChild(element);
                    }
                });


        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));

        return writer.getBuffer().toString();
    }

    private String extractFileExtension(String fileName) {
        String fileExtension = "";
        if (fileName.contains(".")) {
            int index = fileName.lastIndexOf(".");
            if (index < fileName.length() -1) {
                fileExtension = fileName.substring(index + 1);
            }
        }
        return fileExtension;
    }

    private void handleHEAD(Path filePath){
        if (Files.exists(filePath)) {
            // Send file
            if (Files.isRegularFile(filePath)) {
                response = new Response(500);

                try {
                    String fileName = filePath.getFileName().toString();
                    String fileExtension = extractFileExtension(fileName);

                    response = new Response(200);
                    response.addHeader("Content-Length", String.valueOf(Files.size(filePath)));
                    response.addHeader("Content-Type", HTTPContentType.getContentTypeFor(fileExtension));
                    response.addHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (Files.isDirectory(filePath)) {
                // Send directory XML
                response = new Response(500);

                try {
                    String xmlString = getDirectoryStructureXML(request.getRelativePath());
                    response = new Response(200);
                    response.addHeader("Content-Length", String.valueOf(xmlString.getBytes().length));
                    response.addHeader("Content-Type", HTTPContentType.getContentTypeFor("txt"));
                } catch (IOException | ParserConfigurationException | TransformerException e) {
                    e.printStackTrace();
                }
            }
        } else {
            response = new Response(404);
            response.setData("File does not exist".getBytes(), "txt");
        }
    }

    private void handleGET(Path filePath){
        if (Files.exists(filePath)) {
            // Send file
            if (Files.isRegularFile(filePath)) {
                response = new Response(500);
                try {
                    byte[] bytes = Files.readAllBytes(filePath);
                    String fileName = filePath.getFileName().toString();
                    String fileExtension = extractFileExtension(fileName);

                    response = new Response(200);
                    response.setData(bytes, fileExtension);
                    response.addHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (Files.isDirectory(filePath)) {
                // Send directory XML
                response = new Response(500);

                try {
                    String xmlString = getDirectoryStructureXML(request.getRelativePath());
                    response = new Response(200);
                    response.setData(xmlString.getBytes(), "txt");
                } catch (IOException | ParserConfigurationException | TransformerException e) {
                    e.printStackTrace();
                }
            }
        } else {
            response = new Response(404);
            response.setData("File does not exist".getBytes(), "txt");
        }
    }

    private void handlePUT(Path filePath, String filePathString){
        response = new Response(500);
        if (request.getRelativePath().equals("/")) {
            response = new Response(403);
            response.setData("Unable to modify root".getBytes(), "txt");
            return;
        }

        byte[] dataToWrite = null;
        dataToWrite = request.getData();

        if (dataToWrite != null) {
            try {
                if (!Files.exists(filePath.getParent())) {
                    Files.createDirectories(filePath.getParent());
                }
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                }
                Files.createFile(filePath);
                try (FileOutputStream stream = new FileOutputStream(filePathString)) {
                    stream.write(dataToWrite);
                }
                response = new Response(201);
                response.setData("Created new file".getBytes(), "txt");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleDELETE(Path filePath){
        response = new Response(500);
        if (request.getRelativePath().equals("/")) {
            response = new Response(403);
            response.setData("Unable to remove root folder".getBytes(), "txt");
            return;
        }

        if (Files.exists(filePath)) {
            try {
                Files.walk(filePath)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                response = new Response(200);
                response.setData("Successfully deleted".getBytes(), "txt");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            response = new Response(404);
        }

    }

    synchronized public void parseRequest() {
        String method = request.getRequestMethod();
        String filePathString = folderPath + request.getRelativePath();
        Path filePath = Paths.get(filePathString);

        switch (method) {
            case "HEAD": {
                handleHEAD(filePath);
                break;
            }
            case "GET": {
                handleGET(filePath);
                break;
            }
            case "PUT": {
                handlePUT(filePath, filePathString);
                break;
            }
            case "DELETE": {
                handleDELETE(filePath);
                break;
            }
            default: {
                break;
            }
        }
        response.addHeader("Connection", "Closed");
        response.addHeader("Server", "Java-HTTP-Storage");
    }



}

