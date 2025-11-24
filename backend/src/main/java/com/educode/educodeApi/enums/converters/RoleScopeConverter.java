package com.educode.educodeApi.enums.converters;

import com.educode.educodeApi.enums.RoleScope;
import jakarta.persistence.Converter;

@Converter
public class RoleScopeConverter extends GenericCodeEnumConverter<RoleScope, Integer> {
    protected RoleScopeConverter() {
        super(RoleScope.class);
    }
}
