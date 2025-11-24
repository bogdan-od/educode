package com.educode.educodeApi.enums.converters;

import com.educode.educodeApi.enums.CodeEnum;
import jakarta.persistence.AttributeConverter;

public abstract class GenericCodeEnumConverter<E extends Enum<E> & CodeEnum<T>, T>
        implements AttributeConverter<E, T> {

    private final Class<E> enumClass;

    protected GenericCodeEnumConverter(Class<E> enumClass) {
        this.enumClass = enumClass;
    }

    @Override
    public T convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public E convertToEntityAttribute(T dbData) {
        return CodeEnum.fromCode(enumClass, dbData);
    }
}
