package com.example.mssqll.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;

@Converter
public class ListToStringConverter implements AttributeConverter<List<String>, String> {

    private static final String DELIMITER = ",";

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        return (attribute != null && !attribute.isEmpty()) ? String.join(DELIMITER, attribute) : null;
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        return (dbData != null && !dbData.isEmpty()) ? Arrays.asList(dbData.split(DELIMITER)) : null;
    }
}
