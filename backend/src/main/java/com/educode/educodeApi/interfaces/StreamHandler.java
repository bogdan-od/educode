package com.educode.educodeApi.interfaces;

import java.io.IOException;
import java.io.InputStream;

public interface StreamHandler {
    void handle(InputStream stream) throws IOException;
}
