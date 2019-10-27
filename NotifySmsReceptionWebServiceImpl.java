
package com.twonet.sp.phc.sme;

import java.util.List;
import java.util.UUID;

import javax.jws.HandlerChain;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.soap.Addressing;

import org.apache.log4j.Logger;
import org.csapi.schema.parlayx.sms.notification.v2_2.local.NotifySmsReceptionResponse;
import org.csapi.schema.parlayx.sms.notification.v2_2.local.SmsMessage;
import org.csapi.wsdl.parlayx.sms.notification.v2_2._interface.smsnotification.notifysmsreception.NotifySmsReception;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.oxm.XmlMappingException;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import com.twonet.sp.phc.messaging.MessageRouter;
import com.twonet.sp.phc.mups.util.DateTimeConverter;
import com.twonet.sp.phc.xml.phc.InboundSMSMessage;
import com.twonet.sp.phc.xml.phc.InboundSMSMessageList;
import com.twonet.sp.phc.xml.phc.InboundSMSResponseWrapper;
import com.twonet.sp.phc.xml.phc.InboundSMSTextMessage;
import com.twonet.sp.phc.xml.phc.ObjectFactory;

@Addressing(enabled = true, required = true)
@HandlerChain(file = "classpath:handler-chain.xml")
@WebService(endpointInterface = "org.csapi.wsdl.parlayx.sms.notification.v2_2._interface.smsnotification.notifysmsreception.NotifySmsReception")
public class NotifySmsReceptionWebServiceImpl extends SpringBeanAutowiringSupport implements NotifySmsReception
{

	private static final Logger LOG = Logger.getLogger(NotifySmsReceptionWebServiceImpl.class);
	
	@Autowired
	private DateTimeConverter dtc;
	 
	@Autowired
    private MessageRouter messageRouter;
    
    @Autowired
    private HeaderHandler headerHandler;
	
	@Override
	@WebMethod
	public String notifySmsReception(String correlator, List<SmsMessage> message) 
	{
        LOG.debug("Inside NotifySmsReceptionWebServiceImpl Web method");
		
		NotifySmsReceptionResponse notifySmsReceptionResponse = new NotifySmsReceptionResponse();
			
		//parsing message to be sent to cic
		try 
		{
		    ObjectFactory factory = new ObjectFactory();
		    InboundSMSMessage inboundSMSMessage = factory.createInboundSMSMessage();
		    InboundSMSMessageList inboundSMSMessageList = factory.createInboundSMSMessageList();
		    InboundSMSTextMessage inboundSMSTextMessage = factory.createInboundSMSTextMessage();
		    InboundSMSResponseWrapper inboundSMSResponseWrapper = factory.createInboundSMSResponseWrapper();
		
		    UUID uuid = UUID.randomUUID();
		    
		  for(int i=0; i<message.size(); i++) 
		  { 
			
			SmsMessage smsMessage = message.get(i);
		    inboundSMSTextMessage.setMessage(smsMessage.getMessage());
		    inboundSMSMessage.setDateTime(dtc.getXMLGregorianCalendarNow());
		    inboundSMSMessage.setDestinationAddress(smsMessage.getSmsServiceActivationNumber());
		    inboundSMSMessage.setMessageId(headerHandler.getMessageID());
		    inboundSMSMessage.setSenderAddress(smsMessage.getSenderAddress());
		    inboundSMSMessage.setInboundSMSTextMessage(inboundSMSTextMessage);
		    inboundSMSMessage.setResourceURL(null);
		    inboundSMSMessage.setResponseMessageType("ATT");
		    inboundSMSMessageList.getInboundSMSMessage().add(inboundSMSMessage);
		    inboundSMSMessageList.setNumberOfMessagesInThisBatch(null);
		    inboundSMSMessageList.setTotalNumberOfPendingMessages(null);
		    inboundSMSMessageList.setResourceURL(null);
		    inboundSMSResponseWrapper.setInboundSMSMessageList(inboundSMSMessageList);
		    inboundSMSResponseWrapper.setTransactionid(uuid.toString());
		
		    LOG.info("sending message to cic");
            messageRouter.sendPhcMessageToCIC(inboundSMSResponseWrapper);
    
               
		  } 
		  notifySmsReceptionResponse.setResult("Message Received from ATT"); 
		  return notifySmsReceptionResponse.getResult();
		}
		catch(NullPointerException e) {
			LOG.error("NullPointer Exception " + e.getMessage());
			notifySmsReceptionResponse.setResult("Message NOT Received from ATT");
			return notifySmsReceptionResponse.getResult();
		}
		catch(XmlMappingException e) 
		{
			LOG.error("XML exception occured while parsing message");
			notifySmsReceptionResponse.setResult("Message NOT Received from ATT");
			return notifySmsReceptionResponse.getResult();
		}
		catch(Exception e)
		{
			LOG.error("Exception occured while parsing message",e);
			notifySmsReceptionResponse.setResult("Message NOT Received from ATT");
			return notifySmsReceptionResponse.getResult();
		}     
	}

}
