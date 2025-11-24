package com.educode.educodeApi.lazyinit;

import com.educode.educodeApi.models.TreeNode;
import org.hibernate.Hibernate;

public enum TreeNodeInclude implements LazyInclude<TreeNode> {
    MEMBERS {
        @Override
        public void initialize(TreeNode entity) {
            Hibernate.initialize(entity.getMembers());
        }
    },
    PARENT {
        @Override
        public void initialize(TreeNode entity) {
            Hibernate.initialize(entity.getParent());
        }
    },
    CHILDREN {
        @Override
        public void initialize(TreeNode entity) {
            Hibernate.initialize(entity.getChildren());
        }
    }
}
