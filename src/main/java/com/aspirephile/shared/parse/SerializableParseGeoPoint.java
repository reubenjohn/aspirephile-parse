package com.aspirephile.shared.parse;

import com.parse.ParseException;
import com.parse.ParseGeoPoint;

import java.io.Serializable;

public class SerializableParseGeoPoint implements Serializable {

    private static final long serialVersionUID = 1L;

    private double latitude = 0.0D;
    private double longitude = 0.0D;

    public SerializableParseGeoPoint(ParseGeoPoint parseGeoPoint) throws ParseException {

        this.latitude = parseGeoPoint.getLatitude();
        this.longitude = parseGeoPoint.getLongitude();

    }

    public ParseGeoPoint getResurrectParseGeoPoint() throws IllegalAccessException {
        return new ParseGeoPoint(getLatitude(), getLongitude());
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

}