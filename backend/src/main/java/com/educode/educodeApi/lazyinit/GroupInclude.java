package com.educode.educodeApi.lazyinit;

import com.educode.educodeApi.models.Group;
import org.hibernate.Hibernate;

public enum GroupInclude implements LazyInclude<Group> {
    HOMEWORKS {
        @Override
        public void initialize(Group entity) {
            Hibernate.initialize(entity.getHomeworks());
        }
    },
    TREE_NODE {
        @Override
        public void initialize(Group entity) {
            Hibernate.initialize(entity.getTreeNode());
        }
    },
    TREE_NODE_PARENT {
        @Override
        public void initialize(Group entity) {
            Hibernate.initialize(entity.getTreeNode());
            Hibernate.initialize(entity.getTreeNode().getParent());
        }
    }
}
