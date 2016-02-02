package org.wso2.carbon.axis2.runtime.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.axis2.runtime.bridge.ResponseStatus;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.CarbonMessageProcessor;
import org.wso2.carbon.messaging.DefaultCarbonMessage;
import org.wso2.carbon.messaging.TransportSender;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Created by jayanga on 1/26/16.
 */
public class Axis2CarbonMessageProcessor implements CarbonMessageProcessor {
    private static final Logger logger = LoggerFactory.getLogger(Axis2CarbonMessageProcessor.class);
    private CarbonAxis2MessageBridge carbonAxis2MessageBridge = new CarbonAxis2MessageBridge();

    @Override
    public boolean receive(CarbonMessage carbonMessage, CarbonCallback carbonCallback) throws Exception {

        StringBuilder stringBuilder = new StringBuilder();
        while (true) {
            ByteBuffer byteBuffer = carbonMessage.getMessageBody();

            Charset charset = Charset.defaultCharset();
            CharsetDecoder charsetDecoder = charset.newDecoder();
            CharBuffer charBuffer = charsetDecoder.decode(byteBuffer);
            stringBuilder.append(charBuffer.toString());

            if (carbonMessage.isEomAdded() && carbonMessage.isEmpty()) {
                break;
            }
        }

        String requestUri = (String) carbonMessage.getProperty("TO");
        String contentType = carbonMessage.getHeader("Content-Type");

        InputStream inputStream = new ByteArrayInputStream(stringBuilder.toString().getBytes(StandardCharsets.UTF_8));
        OutputStream outputStream = new ByteArrayOutputStream();
        ResponseStatus responseStatus = carbonAxis2MessageBridge.processMessage(inputStream, outputStream,
                contentType, null, requestUri);

        if (ResponseStatus.READY == responseStatus) {
            ByteBuffer responseContent = ByteBuffer.wrap(((ByteArrayOutputStream) outputStream).toByteArray());

            DefaultCarbonMessage defaultCarbonMessage = new DefaultCarbonMessage();
            defaultCarbonMessage.addMessageBody(responseContent);
            defaultCarbonMessage.setHeader("Content-Length", Integer.toString(responseContent.limit()));
            defaultCarbonMessage.setEomAdded(true);
            carbonCallback.done(defaultCarbonMessage);
            return true;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Received response is not in the state of READY");
            }
            return false;
        }
    }

    @Override
    public void setTransportSender(TransportSender transportSender) {

    }

    @Override
    public String getId() {
        return Axis2CarbonMessageProcessor.class.getName();
    }
}
