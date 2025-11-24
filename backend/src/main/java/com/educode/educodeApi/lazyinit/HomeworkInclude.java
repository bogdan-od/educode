package com.educode.educodeApi.lazyinit;

import com.educode.educodeApi.models.Homework;
import org.hibernate.Hibernate;

public enum HomeworkInclude implements LazyInclude<Homework> {
    DECISIONS {
        @Override
        public void initialize(Homework entity) {
            Hibernate.initialize(entity.getDecisions());
        }
    },
    GROUP {
        @Override
        public void initialize(Homework entity) {
            Hibernate.initialize(entity.getGroup());
        }
    },
    PUZZLE {
        @Override
        public void initialize(Homework entity) {
            Hibernate.initialize(entity.getPuzzle());
        }
    }
}
