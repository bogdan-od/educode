package com.educode.educodeApi.enums;

public enum TreeNodeInvitationRestriction implements CodeEnum<Integer> {
    NONE(0),
    NODE_MEMBERS(1);

    private final Integer code;

    TreeNodeInvitationRestriction(Integer code) {
        this.code = code;
    }

    @Override
    public Integer getCode() {
        return code;
    }
}
