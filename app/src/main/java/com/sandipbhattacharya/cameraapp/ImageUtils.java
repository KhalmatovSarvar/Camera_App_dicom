package com.sandipbhattacharya.cameraapp;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;

public class ImageUtils {

    public static byte[] bitmapToJpegByteArray(Bitmap bitmap, int quality) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }
}
