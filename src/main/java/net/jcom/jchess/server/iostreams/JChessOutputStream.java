package net.jcom.jchess.server.iostreams;

import net.jcom.jchess.server.generated.JChessMessage;
import org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class JChessOutputStream {
    private final OutputStream outputStream;
    private final Marshaller marshaller;
    private final Object sendLock = new Object();

    public JChessOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;

        try {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = sf.newSchema(this.getClass().getResource("/xsd/j-chess-xsd/jChessMessage.xsd"));
            JAXBContext jaxbContext = JAXBContext.newInstance(JChessMessage.class);
            this.marshaller = jaxbContext.createMarshaller();
            this.marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
            this.marshaller.setSchema(schema);
            this.marshaller.setEventHandler((event) -> false);
        } catch (SAXException | JAXBException var5) {
            throw new RuntimeException(var5);
        }
    }

    private void writeString(String text) throws IOException {
        byte[] header = (new BigInteger(Integer.toString(text.length()))).toByteArray();
        byte[] headerToSend = new byte[4];
        System.arraycopy(header, 0, headerToSend, 4 - header.length, header.length);
        IOUtils.write(headerToSend, this.outputStream);
        IOUtils.write(text, this.outputStream, StandardCharsets.UTF_8);
    }

    public boolean write(JChessMessage jChessMessage) throws IOException {
        synchronized (this.sendLock) {
            try {
                this.writeString(this.jChessToXML(jChessMessage));
                this.flush();
                return true;
            } catch (JAXBException var5) {
                return false;
            }
        }
    }

    @Deprecated
    public String jChessToXML(JChessMessage jChessMessage) throws JAXBException {
        return jChessToXml(jChessMessage);
    }

    public String jChessToXml(JChessMessage jChessMessage) throws JAXBException {
        jChessMessage.setSchemaVersion(JChessInputStream.CURRENT_SCHEMA_VERSION);

        StringWriter stringWriter = new StringWriter();
        this.marshaller.marshal(jChessMessage, stringWriter);
        return stringWriter.toString();
    }

    public void flush() throws IOException {
        this.outputStream.flush();
    }

    public void close() throws IOException {
        this.outputStream.close();
    }
}
