package com.aspirephile.shared.parse;

import com.aspirephile.shared.debug.Logger;
import com.aspirephile.shared.debug.NullPointerAsserter;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseImageView;

public class ParseImageViewUtils {
    Logger l;
    NullPointerAsserter asserter;

    public ParseImageViewUtils(Logger logger) {
        assert logger != null;
        if (logger != null)
            l = logger;
        else
            l = new Logger("<NullLogger>");
        asserter = new NullPointerAsserter(l);
    }

    public void loadParseImage(ParseImageView parseImageView, ParseFile imageFile) {
        if (asserter.assertPointer(imageFile))
            l.d("Loading parse image view: " + parseImageView + " with -> photo file: " + imageFile);
        else
            l.w("Photo file for parse image view: " + parseImageView + " is null!");
        if (asserter.assertPointer(imageFile)) {
            parseImageView.setParseFile(imageFile);
            parseImageView.loadInBackground(new GetDataCallback() {
                @Override
                public void done(byte[] data, ParseException e) {
                    int size = 0;
                    if (data != null)
                        size = data.length;

                    if (e != null)
                        e.printStackTrace();
                    else if (size > 0)
                        l.d("Parse image view loaded " + size + " bytes successfully");
                    else
                        l.e("Parse image view loaded 0 bytes!");
                }
            });
        }
    }
}
