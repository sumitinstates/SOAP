package com.twonet.sp.phc.sme;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.w3c.dom.NodeList;

@Component
public class HeaderHandler implements SOAPHandler<SOAPMessageContext> 
{
	public static String messageID;
	
	public String getMessageID() {
		return messageID;
	}

	private static final Logger LOG = Logger.getLogger(HeaderHandler.class);

	@Override
	public boolean handleMessage(SOAPMessageContext context) 
	{
		
		     Boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
		     
			    if (outbound) 
				{
				      LOG.debug("SOAP message departing");
				} 
				else 
				{
				      LOG.debug("SOAP message incoming");
				      SOAPMessage soapMsg = context.getMessage();
			        
					try {
						
						printFormattedXML(soapMsg);
						
						SOAPEnvelope soapEnv = soapMsg.getSOAPPart().getEnvelope();
						SOAPHeader soapHeader = soapEnv.getHeader();
						
						NodeList messageIDNode = soapHeader.getElementsByTagNameNS("*", "MessageID");
						messageID = messageIDNode.item(0).getChildNodes().item(0).getNodeValue();
						
						LOG.debug("MessageID value is :" + getMessageID());
						
					} 
					catch (SOAPException e) 
					{
						LOG.error("SOAPException occured", e);
						e.printStackTrace();
					} catch (Exception e) {
						LOG.error("exception occured", e);
						e.printStackTrace();
					}		   
				}        
			         return true;       
	}
	
	public static void printFormattedXML(SOAPMessage message) throws Exception
	{
		LOG.debug("Inside printformatttedxml method");
	    ByteArrayOutputStream bout = new ByteArrayOutputStream();
	    message.writeTo(bout);
	    String xml = bout.toString();

	    Source xmlInput = new StreamSource(new StringReader(xml));
	    StringWriter stringWriter = new StringWriter();
	    StreamResult xmlOutput = new StreamResult(stringWriter);
	    TransformerFactory transformerFactory = TransformerFactory.newInstance();
	    transformerFactory.setAttribute("indent-number", 5);
	    Transformer transformer = transformerFactory.newTransformer();
	    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	    transformer.transform(xmlInput, xmlOutput);
	    String xmlString = xmlOutput.getWriter().toString();

	    LOG.debug(xmlString);
	}

	@Override
	public boolean handleFault(SOAPMessageContext context) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void close(MessageContext context) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<QName> getHeaders() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString() {
		return messageID;
	}

	
}
