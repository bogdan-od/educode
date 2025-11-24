package com.educode.educodeApi.lazyinit;

import com.educode.educodeApi.models.RefreshToken;
import org.hibernate.Hibernate;

public enum RefreshTokenInclude implements LazyInclude<RefreshToken> {
    SESSION {
        @Override
        public void initialize(RefreshToken refreshToken) {
            Hibernate.initialize(refreshToken.getSession());
        }
    }
}
