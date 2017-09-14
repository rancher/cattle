package io.github.ibuildthecloud.gdapi.json;

import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SerializedString;

public class EscapeForwardSlash extends CharacterEscapes {

    private static final long serialVersionUID = -8247873729957370584L;

    private final int[] _asciiEscapes;
    private final SerializedString escaped = new SerializedString("\\/");

    public EscapeForwardSlash() {
        _asciiEscapes = standardAsciiEscapesForJSON();
        _asciiEscapes['/'] = CharacterEscapes.ESCAPE_CUSTOM;
    }

    @Override
    public int[] getEscapeCodesForAscii() {
        return _asciiEscapes;
    }

    @Override
    public SerializableString getEscapeSequence(int i) {
        if (i == '/')
            return escaped;
        return null;
    }

}