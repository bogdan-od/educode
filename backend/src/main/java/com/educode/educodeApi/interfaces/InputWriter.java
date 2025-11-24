package com.educode.educodeApi.interfaces;

import java.io.IOException;
import java.io.OutputStream;

public interface InputWriter {
    void write(OutputStream stream) throws IOException;
}
