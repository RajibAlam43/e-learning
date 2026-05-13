package com.gii.api.service.payment.bkash;

import java.io.Serializable;

public record Message(
    String Type,
    String MessageId,
    String Token,
    String TopicArn,
    String Message,
    String SubscribeURL,
    String Timestamp,
    String SignatureVersion,
    String Signature,
    String SigningCertURL,
    String Subject,
    String UnsubscribeURL)
    implements Serializable {}
