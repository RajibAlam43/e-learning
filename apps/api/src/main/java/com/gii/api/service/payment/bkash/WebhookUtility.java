package com.gii.api.service.payment.bkash;

import java.io.InputStream;
import java.net.URL;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * Created by alam.ashraful on 6/3/2018.
 */
public class WebhookUtility {
    public static boolean isMessageSignatureValid(Message msg) {
        try {
            URL url = new URL(msg.SigningCertURL());
            InputStream inStream = url.openStream();
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);
            inStream.close();

            Signature sig = Signature.getInstance("SHA1withRSA");
            sig.initVerify(cert.getPublicKey());
            sig.update(getMessageBytesToSign(msg));
            return sig.verify(Base64.getDecoder().decode(msg.Signature()));
        } catch (Exception e) {
            throw new SecurityException("Verify method failed.", e);
        }
    }

    private static byte[] getMessageBytesToSign(Message msg) {
        byte[] bytesToSign = null;
        if (msg.Type().equals("Notification"))
            bytesToSign = buildNotificationStringToSign(msg).getBytes();
        else if (msg.Type().equals("SubscriptionConfirmation") || msg.Type().equals("UnsubscribeConfirmation"))
            bytesToSign = buildSubscriptionStringToSign(msg).getBytes();
        return bytesToSign;
    }

    private static String buildNotificationStringToSign(Message msg) {
        String stringToSign = null;
        stringToSign = "Message\n";
        stringToSign += msg.Message() + "\n";
        stringToSign += "MessageId\n";
        stringToSign += msg.MessageId() + "\n";
        if (msg.Subject() != null) {
            stringToSign += "Subject\n";
            stringToSign += msg.Subject() + "\n";
        }
        stringToSign += "Timestamp\n";
        stringToSign += msg.Timestamp() + "\n";
        stringToSign += "TopicArn\n";
        stringToSign += msg.TopicArn() + "\n";
        stringToSign += "Type\n";
        stringToSign += msg.Type() + "\n";
        return stringToSign;
    }

    private static String buildSubscriptionStringToSign(Message msg) {
        String stringToSign = null;
        stringToSign = "Message\n";
        stringToSign += msg.Message() + "\n";
        stringToSign += "MessageId\n";
        stringToSign += msg.MessageId() + "\n";
        stringToSign += "SubscribeURL\n";
        stringToSign += msg.SubscribeURL() + "\n";
        stringToSign += "Timestamp\n";
        stringToSign += msg.Timestamp() + "\n";
        stringToSign += "Token\n";
        stringToSign += msg.Token() + "\n";
        stringToSign += "TopicArn\n";
        stringToSign += msg.TopicArn() + "\n";
        stringToSign += "Type\n";
        stringToSign += msg.Type() + "\n";
        return stringToSign;
    }
}
