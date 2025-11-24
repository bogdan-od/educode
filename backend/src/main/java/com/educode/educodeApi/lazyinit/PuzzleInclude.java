package com.educode.educodeApi.lazyinit;

import com.educode.educodeApi.models.Puzzle;
import org.hibernate.Hibernate;

public enum PuzzleInclude implements LazyInclude<Puzzle> {
    PUZZLE_DATA {
        @Override
        public void initialize(Puzzle puzzle) {
            Hibernate.initialize(puzzle.getPuzzleData());
        }
    },
    DECISIONS {
        @Override
        public void initialize(Puzzle puzzle) {
            Hibernate.initialize(puzzle.getDecisions());
        }
    },
    CHECKER {
        @Override
        public void initialize(Puzzle puzzle) {
            Hibernate.initialize(puzzle.getChecker());
        }
    },
    TREE_NODES {
        @Override
        public void initialize(Puzzle puzzle) {
            Hibernate.initialize(puzzle.getTreeNodes());
        }
    }
}
