package com.educode.educodeApi.lazyinit;

import com.educode.educodeApi.models.AccessToken;
import org.hibernate.Hibernate;

public enum AccessTokenInclude implements LazyInclude<AccessToken> {
    SESSION {
        @Override
        public void initialize(AccessToken accessToken) {
            Hibernate.initialize(accessToken.getSession());
        }
    }
}
