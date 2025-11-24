package com.educode.educodeApi.lazyinit;

import com.educode.educodeApi.models.Checker;
import org.hibernate.Hibernate;

public enum CheckerInclude implements LazyInclude<Checker> {
    PUZZLES {
        @Override
        public void initialize(Checker checker) {
            Hibernate.initialize(checker.getPuzzles());
        }
    }
}
