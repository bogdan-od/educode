package com.educode.educodeApi.enums.converters;

import com.educode.educodeApi.enums.TaskType;
import jakarta.persistence.Converter;

@Converter
public class TaskTypeConverter extends GenericCodeEnumConverter<TaskType, Integer> {
    public TaskTypeConverter() {
        super(TaskType.class);
    }
}
