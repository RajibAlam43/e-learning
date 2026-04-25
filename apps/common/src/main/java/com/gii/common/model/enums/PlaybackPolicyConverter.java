package com.gii.common.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PlaybackPolicyConverter implements AttributeConverter<PlaybackPolicy, String> {

    @Override
    public String convertToDatabaseColumn(PlaybackPolicy attribute) {
        if (attribute == null) {
            return null;
        }

        return switch (attribute) {
            case public_ -> "public";
            case signed -> "signed";
        };
    }

    @Override
    public PlaybackPolicy convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        return switch (dbData) {
            case "public" -> PlaybackPolicy.public_;
            case "signed" -> PlaybackPolicy.signed;
            default -> throw new IllegalArgumentException("Unknown playback policy: " + dbData);
        };
    }
}