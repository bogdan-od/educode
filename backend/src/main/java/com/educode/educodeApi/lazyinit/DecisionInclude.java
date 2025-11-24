package com.educode.educodeApi.lazyinit;

import com.educode.educodeApi.models.Decision;
import org.hibernate.Hibernate;

public enum DecisionInclude implements LazyInclude<Decision> {
    HOMEWORK {
        @Override
        public void initialize(Decision entity) {
            Hibernate.initialize(entity.getHomework());
        }
    }
}
