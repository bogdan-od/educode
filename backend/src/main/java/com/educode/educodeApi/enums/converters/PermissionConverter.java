package com.educode.educodeApi.enums.converters;

import com.educode.educodeApi.enums.PermissionType;
import jakarta.persistence.Converter;

@Converter
public class PermissionConverter extends GenericCodeEnumConverter<PermissionType, Integer> {
    public PermissionConverter() {
        super(PermissionType.class);
    }
}
