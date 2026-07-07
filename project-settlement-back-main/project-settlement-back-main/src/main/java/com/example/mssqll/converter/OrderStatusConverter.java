package com.example.mssqll.converter;

import com.example.mssqll.models.OrderStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class OrderStatusConverter implements AttributeConverter<OrderStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(OrderStatus attribute) {
        return attribute == null ? null : attribute.ordinal();
    }

    @Override
    public OrderStatus convertToEntityAttribute(Integer dbData) {
        if (dbData == null) return null;
        OrderStatus[] values = OrderStatus.values();
        return (dbData >= 0 && dbData < values.length) ? values[dbData] : null;
    }
}
