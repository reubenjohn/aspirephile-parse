package com.aspirephile.shared.parse;

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

public class SerializableParseObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private HashMap<String, Object> values = new HashMap<>();
    private HashMap<String, SerializableParseFile> serializedParseFiles = new HashMap<>();
    private HashMap<String, SerializableParseGeoPoint> serializedParseGeoPoints = new HashMap<>();
    private String className;
    private Date createdAt;
    private String objectId;
    private Date updatedAt;

    public SerializableParseObject(ParseObject parseObject) throws ParseException {

        for (String key : parseObject.keySet()) {
            Class classType = parseObject.get(key).getClass();
            if (classType == Boolean.class ||
                    classType == byte[].class ||
                    classType == Date.class ||
                    classType == Double.class ||
                    classType == Integer.class ||
                    classType == Long.class ||
                    classType == Number.class ||
                    classType == String.class) {
                values.put(key, parseObject.get(key));
            } else if (classType == ParseFile.class) {
                // In the case of ParseFile, the url to the file will be retained as a String, since ParseFile is not serializable
                SerializableParseFile serializableParseFile = new SerializableParseFile(((ParseFile) parseObject.get(key)));
                serializedParseFiles.put(key, serializableParseFile);
            } else if (classType == ParseGeoPoint.class) {
                // In the case of a ParseGeoPoint, the doubles values for lat, long will be retained in a double[], since ParseGeoPoint is not serializable
                SerializableParseGeoPoint serializableParseGeoPoint = new SerializableParseGeoPoint(((ParseGeoPoint) parseObject.get(key)));
                serializedParseGeoPoints.put(key, serializableParseGeoPoint);
            }
        }

        this.className = parseObject.getClassName();
        this.createdAt = parseObject.getCreatedAt();
        this.objectId = parseObject.getObjectId();
        this.updatedAt = parseObject.getUpdatedAt();
    }

    public ParseObject getDeserialized() {
        ParseObject parseObject = ParseObject.createWithoutData(getClassName(), getObjectId());
        for (String key : values.keySet()) {
            parseObject.put(key, values.get(key));
        }
        for (String key : serializedParseFiles.keySet()) {
            SerializableParseFile serializableParseFile = serializedParseFiles.get(key);
            try {
                parseObject.put(key, serializableParseFile.getResurrectParseFile());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        for (String key : serializedParseGeoPoints.keySet()) {
            SerializableParseGeoPoint serializedParseGeoPoint = this.serializedParseGeoPoints.get(key);
            try {
                parseObject.put(key, serializedParseGeoPoint.getResurrectParseGeoPoint());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return parseObject;
    }

    public boolean getBoolean(String key) {
        if (values.containsKey(key)) {
            return (Boolean) values.get(key);
        } else {
            return false;
        }
    }

    public byte[] getBytes(String key) {
        if (values.containsKey(key)) {
            return (byte[]) values.get(key);
        } else {
            return null;
        }
    }

    public String getClassName() {
        return className;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getDate(String key) {
        if (values.containsKey(key)) {
            return (Date) values.get(key);
        } else {
            return null;
        }
    }

    public double getDouble(String key) {
        if (values.containsKey(key)) {
            return (Double) values.get(key);
        } else {
            return 0;
        }
    }

    public int getInt(String key) {
        if (values.containsKey(key)) {
            return (Integer) values.get(key);
        } else {
            return 0;
        }
    }

    public long getLong(String key) {
        if (values.containsKey(key)) {
            return (Long) values.get(key);
        } else {
            return 0;
        }
    }

    public Number getNumber(String key) {
        if (values.containsKey(key)) {
            return (Number) values.get(key);
        } else {
            return null;
        }
    }

    public String getObjectId() {
        return objectId;
    }

    //Note: only the url to the file is returned, not an actual ParseFile
    public SerializableParseFile getParseFile(String key) {
        if (serializedParseFiles.containsKey(key)) {
            return serializedParseFiles.get(key);
        } else {
            return null;
        }
    }

    // Note only the lat, long values are returned, not the actual ParseGeoPoint
    // [0] latitude
    // [1] longitude
    public SerializableParseGeoPoint getParseGeoPointArray(String key) {
        if (serializedParseGeoPoints.containsKey(key)) {
            return serializedParseGeoPoints.get(key);
        } else {
            return null;
        }
    }

    public String getString(String key) {
        if (values.containsKey(key)) {
            return (String) values.get(key);
        } else {
            return null;
        }
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }


}