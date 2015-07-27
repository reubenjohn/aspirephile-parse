package com.aspirephile.shared.parse;

import com.parse.ParseException;
import com.parse.ParseFile;

import java.io.Serializable;

public class SerializableParseFile implements Serializable {

    private static final long serialVersionUID = 1L;
    private final byte[] data;

    private String name;
    private String url;

    public SerializableParseFile(ParseFile parseFile) throws ParseException {

        this.name = parseFile.getName();
        this.url = parseFile.getUrl();
        this.data = parseFile.getData();
    }

    public ParseFile getResurrectParseFile() throws IllegalAccessException {
        return new ParseFile(getName(), getData());// TODO Set private state data of ParseFile too
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }


    public byte[] getData() {
        return data;
    }

}