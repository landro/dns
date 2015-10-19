package saaj;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

public class HttpSOAPConnectionTest {


	private static final String ENDPOINT = "http://www.webservicex.com/globalweather.asmx";

	private SOAPConnectionFactory connectionFactory;
	private MessageFactory messageFactory;

	@Before
	public void setUp() throws SOAPException {
		connectionFactory = SOAPConnectionFactory.newInstance();
		messageFactory = MessageFactory.newInstance();
	}

	@Test
	public void test() throws SOAPException {

		SOAPMessage message = messageFactory.createMessage();
		message.getSOAPHeader().detachNode(); // delete SOAP header
		SOAPBody body = message.getSOAPBody();

		QName bodyName = new QName("http://www.webserviceX.NET", "GetWeather");
		SOAPBodyElement bodyElement = body.addBodyElement(bodyName);

		QName cityName = new QName("CityName");
		SOAPElement citySoapElement = bodyElement.addChildElement(cityName);
		citySoapElement.addTextNode("Bardufoss");

		QName countryName = new QName("CountryName");
		SOAPElement countrySoapElement = bodyElement.addChildElement(countryName);
		countrySoapElement.addTextNode("Norway");

		SOAPConnection connection = connectionFactory.createConnection();

		// Wireshare shows TCP connection is effectively closed by
		// HTTPSoapConnection.java L:359 !
		SOAPMessage response = connection.call(message, ENDPOINT);

		Document ownerDocument = response.getSOAPBody().getOwnerDocument();

	}

	private String getStringFromDoc(Document doc) {
		DOMImplementationLS domImplementation = (DOMImplementationLS) doc.getImplementation();
		LSSerializer lsSerializer = domImplementation.createLSSerializer();
		return lsSerializer.writeToString(doc);
	}


}
