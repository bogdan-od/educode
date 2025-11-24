package com.educode.educodeApi.enums;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum RoleScope implements CodeEnum<Integer> {
    GLOBAL_AND_NODE(1),
    NODE_AND_GROUP(2),
    GLOBAL(3, Set.of(GLOBAL_AND_NODE)),
    NODE(4, Set.of(GLOBAL_AND_NODE, NODE_AND_GROUP)),
    GROUP(5, Set.of(NODE_AND_GROUP));

    private final Integer code;
    private final Set<RoleScope> children;

    RoleScope(Integer code) {
        this.code = code;
        this.children = Set.of(this);
    }

    RoleScope(Integer code, Set<RoleScope> children) {
        this.code = code;
        this.children = children.isEmpty()
                ? Set.of(this)
                : Collections.unmodifiableSet(Stream.concat(children.stream(), Set.of(this).stream()).collect(Collectors.toSet()));
    }

    @Override
    public Integer getCode() {
        return code;
    }

    public Set<RoleScope> getChildren() {
        return children;
    }
}

