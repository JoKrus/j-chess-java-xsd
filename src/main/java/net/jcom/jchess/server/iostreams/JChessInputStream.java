package net.jcom.jchess.server.iostreams;

import net.jcom.jchess.server.exception.InvalidSchemaVersion;
import net.jcom.jchess.server.generated.JChessMessage;
import org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class JChessInputStream {
    public static final String CURRENT_SCHEMA_VERSION = new JChessMessage().getSchemaVersion();

    private final InputStream inputStream;
    private final Unmarshaller unmarshaller;

    public JChessInputStream(InputStream inputStream) {
        this.inputStream = inputStream;

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(JChessMessage.class);
            this.unmarshaller = jaxbContext.createUnmarshaller();
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            try {
                URL urlToXsd = this.getClass().getResource("/xsd/j-chess-xsd/jChessMessage.xsd");
                Schema schema = schemaFactory.newSchema(urlToXsd);
                this.unmarshaller.setSchema(schema);
                this.unmarshaller.setEventHandler((validationEvent) -> false);
            } catch (SAXException var6) {
                throw new RuntimeException(var6);
            }
        } catch (JAXBException var7) {
            throw new RuntimeException(var7);
        }
    }

    private String readMessage() throws IOException {
        int messageLength = this.readHeader();
        byte[] message = new byte[messageLength];
        IOUtils.readFully(this.inputStream, message);
        return new String(message, StandardCharsets.UTF_8);
    }

    private int readHeader() throws IOException {
        byte[] textLength = new byte[4];
        IOUtils.readFully(this.inputStream, textLength);
        return (new BigInteger(textLength)).intValue();
    }

    public JChessMessage readJChess() throws IOException, InvalidSchemaVersion {
        JChessMessage result;
        try {
            String xml = this.readMessage();
            result = this.XMLToJChess(xml);
        } catch (NullPointerException | JAXBException var3) {
            result = null;
        }

        return result;
    }

    @Deprecated
    public JChessMessage XMLToJChess(String xml) throws JAXBException, InvalidSchemaVersion {
        return xmlToJChess(xml);
    }

    public JChessMessage xmlToJChess(String xml) throws JAXBException, InvalidSchemaVersion {
        StringReader stringReader = new StringReader(xml);
        JChessMessage unmarshal = (JChessMessage) this.unmarshaller.unmarshal(stringReader);
        if (!CURRENT_SCHEMA_VERSION.equals(unmarshal.getSchemaVersion())) {
            throw new InvalidSchemaVersion(CURRENT_SCHEMA_VERSION, unmarshal.getSchemaVersion());
        }
        return unmarshal;
    }

    public void close() throws IOException {
        this.inputStream.close();
    }
}
