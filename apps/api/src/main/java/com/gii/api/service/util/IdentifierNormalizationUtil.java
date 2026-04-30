package com.gii.api.service.util;

import com.gii.common.enums.VerificationChannel;
import lombok.experimental.UtilityClass;

import java.util.Locale;

@UtilityClass
public class IdentifierNormalizationUtil {

    public static String normalizeIdentifier(VerificationChannel channel, String identifier) {
        String value = identifier == null ? "" : identifier.trim();
        return switch (channel) {
            case EMAIL -> value.toLowerCase(Locale.ROOT);
            case PHONE -> value.replaceAll("[^0-9]", "");
        };
    }
}
