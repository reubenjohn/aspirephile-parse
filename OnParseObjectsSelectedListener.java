package com.aspirephile.shared.parse;


import com.parse.ParseObject;

import java.util.ArrayList;

public interface OnParseObjectsSelectedListener<E> {
    void onParseObjectsSelectedListener(ArrayList<E> selectedObjectIds);
}
