package com.educode.educodeApi.DTO.treenode.invitation;

import java.util.List;

public record PaginatedInvitationDTO(
        List<InvitationViewDTO> invitations,
        boolean hasNext
) {}
