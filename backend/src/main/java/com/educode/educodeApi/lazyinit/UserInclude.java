package com.educode.educodeApi.lazyinit;

import com.educode.educodeApi.models.Puzzle;
import com.educode.educodeApi.models.User;
import org.hibernate.Hibernate;

public enum UserInclude implements LazyInclude<User> {
    ACCESS_TOKENS {
        @Override
        public void initialize(User user) {
            Hibernate.initialize(user.getAccessTokens());
        }
    },
    DECISIONS {
        @Override
        public void initialize(User user) {
            Hibernate.initialize(user.getDecisions());
        }
    },
    PUZZLES {
        @Override
        public void initialize(User user) {
            Hibernate.initialize(user.getPuzzles());
        }
    },
    REFRESH_TOKENS {
        @Override
        public void initialize(User user) {
            Hibernate.initialize(user.getRefreshTokens());
        }
    },
    SESSIONS {
        @Override
        public void initialize(User user) {
            Hibernate.initialize(user.getSessions());
        }
    },
    PUZZLE_PUZZLEDATA {
        @Override
        public void initialize(User user) {
            for (Puzzle puzzle : user.getPuzzles()) {
                Hibernate.initialize(puzzle.getPuzzleData());
            }
        }
    },
    CHECKERS {
        @Override
        public void initialize(User user) {
            Hibernate.initialize(user.getCheckers());
        }
    },
    NOTIFICATIONS {
        @Override
        public void initialize(User user) {
            Hibernate.initialize(user.getNotifications());
        }
    },
    TREE_NODE_MEMBERS {
        @Override
        public void initialize(User user) {
            Hibernate.initialize(user.getTreeNodeMembers());
        }
    },
}
