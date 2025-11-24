package com.educode.educodeApi.lazyinit;

import com.educode.educodeApi.models.Node;
import org.hibernate.Hibernate;

public enum NodeInclude implements LazyInclude<Node> {
    TREE_NODE {
        @Override
        public void initialize(Node entity) {
            Hibernate.initialize(entity.getTreeNode());
        }
    },
    TREE_NODE_PARENT {
        @Override
        public void initialize(Node entity) {
            Hibernate.initialize(entity.getTreeNode());
            Hibernate.initialize(entity.getTreeNode().getParent());
        }
    }
}
