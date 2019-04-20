package com.doggzam.doggzam;

import android.graphics.Bitmap;

import java.util.Map;

public interface MainActivityInteraction {
    void setInfoText(String text);
    void setDetailsText(Map results);
    void setMainImageBitmap(Bitmap bitmap);
}
