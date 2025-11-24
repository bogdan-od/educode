package com.educode.educodeApi.mappers;

import com.educode.educodeApi.DTO.auth.SessionDTO;
import com.educode.educodeApi.models.Session;
import org.springframework.stereotype.Component;

@Component
public class SessionMapper {
    public SessionDTO toDTO(Session session, boolean isCurrentSession) {
        return new SessionDTO(
            session.getId(),
            session.getDeviceName(),
            session.getIpAddress(),
            session.getDeviceType(),
            session.getCreatedAt(),
            isCurrentSession
        );
    }

    public SessionDTO toDTO(Session session, String currentAccessToken) {
        return toDTO(session, currentAccessToken != null && currentAccessToken.equals(session.getAccessToken().getToken()));
    }

    public SessionDTO toDTO(Session session) {
        return toDTO(session, false);
    }
}
