package com.educode.educodeApi.mappers;

import com.educode.educodeApi.DTO.treenode.invitation.InvitationDetailsDTO;
import com.educode.educodeApi.DTO.treenode.invitation.InvitationViewDTO;
import com.educode.educodeApi.DTO.treenode.invitation.SelfInvitationViewDTO;
import com.educode.educodeApi.models.TreeNodeInvitation;
import com.educode.educodeApi.utils.StdOptional;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class TreeNodeInvitationMapper {
    private final RoleMapper roleMapper;

    public TreeNodeInvitationMapper(RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    public InvitationViewDTO toViewDTO(TreeNodeInvitation invitation, StdOptional<Integer> usersCount) {
        if (invitation == null) {
            return null;
        }

        return new InvitationViewDTO(
                invitation.getId(),
                invitation.getCode(),
                invitation.getTreeNode().getId(),
                invitation.getRoles().stream().map(roleMapper::toMinimalDTO).collect(Collectors.toList()),
                invitation.getExpiresAt(),
                invitation.getActive(),
                invitation.getInvitationType().getDescription(),
                invitation.getAllowedTreeNode() != null ? invitation.getAllowedTreeNode().getId() : null,
                usersCount.orElseGet(() -> invitation.getUsers() != null ? invitation.getUsers().size() : 0),
                invitation.isCanLeaveOnJoin()
        );
    }

    public InvitationViewDTO toViewDTO(TreeNodeInvitation invitation) {
        return toViewDTO(invitation, StdOptional.empty());
    }

    public InvitationDetailsDTO toDetailsDTO(TreeNodeInvitation invitation) {
        if (invitation == null) {
            return null;
        }
        return new InvitationDetailsDTO(
                invitation.getTreeNode().getId(),
                invitation.getRoles().stream().map(roleMapper::toMinimalDTO).collect(Collectors.toList()),
                invitation.getInvitationType().getDescription(),
                invitation.isCanLeaveOnJoin()
        );
    }

    public SelfInvitationViewDTO toSelfViewDTO(TreeNodeInvitation invitation, String entityTitle) {
        if (invitation == null) {
            return null;
        }
        return new SelfInvitationViewDTO(
                invitation.getId(),
                invitation.getCode(),
                invitation.getInvitationType().getDescription(),
                invitation.getRoles().stream().map(roleMapper::toMinimalDTO).collect(Collectors.toList()),
                invitation.getExpiresAt(),
                invitation.getActive(),
                invitation.isCanLeaveOnJoin(),
                entityTitle
        );
    }
}
