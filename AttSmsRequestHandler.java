package com.twonet.sp.phc.sme;

import com.twonet.sp.phc.PhcResultCodes;
import com.twonet.sp.phc.messaging.MessageRouter;
import com.twonet.sp.phc.model.GatewayDevice;
import com.twonet.sp.phc.model.PhcRequest;
import com.twonet.sp.phc.model.enumeration.RequestStatus;
import com.twonet.sp.phc.mups.util.DateTimeConverter;
import com.twonet.sp.phc.service.GatewayDeviceService;
import com.twonet.sp.phc.service.PhcRequestService;
import com.twonet.sp.phc.smshandler.SmsWsOperations;
import com.twonet.sp.phc.smshandler.response.SMSResponse;
import com.twonet.sp.phc.xml.phc.*;
import com.twonet.sp.phc.xml.sme.SMEInterface;
import com.twonet.sp.phc.xml.sme.SMSPING;
import com.twonet.sp.phc.xml.sme.SMSRequest;
import com.twonet.sp.util.messagingutil.MessageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.StringReader;

public class AttSmsRequestHandler extends AbstractMessageHandler {
    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger
            .getLogger(AttSmsRequestHandler.class);

    @Autowired
    @Qualifier("smeMarshaller")
    private Unmarshaller unmarshaller;

    @Autowired
    PhcRequestService phcRequestService;

    @Autowired
    private MessageRouter messageRouter;


    @Autowired
    @Qualifier("attSmsWsOperations")
    SmsWsOperations ws;

    public void handleMessage(byte[] message) {
        try {
            System.out.println("Sending a message to AT&T" + new String(message));
            handleMessage(new String(message));
        } catch (Exception e) {
            LOG.error("Error handling AT&T SMS request: " + new String(message), e);
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    public void handleMessage(String message) {
        try {
            SMEInterface smei = (SMEInterface) unmarshaller
                    .unmarshal(new StreamSource(new StringReader(message)));
            SMSRequest request = smei.getSMSRequest();
            SMSPING ping = request.getSMSPING();

            // txnId maps to a transaction id in the PhcRequest table so status
            // can be updated
            String txnId = ping.getRequestID();
            String min = ping.getMOBILEMIN();
            String msg = ping.getPAYLOAD();

            PhcRequest phcRequest = phcRequestService.getPhcRequestByTransactionId(txnId);
            PhcResponse phcResponse = formatPhcSmsResponse(min, txnId, phcRequest);

            //Call ws return bool
            SMSResponse response = ws.sendSMS(min, msg, 0);
            phcRequest = formatPhcSmsReqPayload(phcRequest, response.getStatus(), response);
            phcResponse = formatPhcSmsRespPayload(phcResponse, response.getStatus(), min);


            try {
                /**
                 * Need to make sure that we pass along the HubId and the Source
                 */
                phcRequestService.update(phcRequest);
                messageRouter.sendPhcResponse(phcResponse);
            } catch(Exception ex) {
                LOG.error("Error updating or sending response, " + ex);
            }

        } catch (Exception e) {
            LOG.error("Error handling sme request: " + message, e);
        }
    }
}
