package com.gii.api.service.util;

import com.gii.common.enums.VerificationChannel;
import java.util.Locale;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import lombok.experimental.UtilityClass;

@UtilityClass
public class IdentifierNormalizationUtil {

  public static String normalizeIdentifier(VerificationChannel channel, String identifier) {
    String value = identifier == null ? "" : identifier.trim();
    return switch (channel) {
      case EMAIL -> value.toLowerCase(Locale.ROOT);
      case PHONE -> normalizeBdPhone(value);
    };
  }

  public String normalizeBdPhone(String rawPhoneNumber) {
    PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

    try {
      Phonenumber.PhoneNumber parsed = phoneUtil.parse(rawPhoneNumber, "BD");

      if (parsed.getCountryCode() != 880) {
        throw new IllegalArgumentException("Only Bangladesh numbers are supported");
      }

      if (!phoneUtil.isValidNumberForRegion(parsed, "BD")) {
        throw new IllegalArgumentException("Invalid Bangladesh phone number");
      }

      return phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
    } catch (NumberParseException e) {
      throw new IllegalArgumentException("Invalid Bangladesh phone number", e);
    }
  }
}
