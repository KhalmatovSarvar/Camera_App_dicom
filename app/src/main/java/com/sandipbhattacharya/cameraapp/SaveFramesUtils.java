package com.sandipbhattacharya.cameraapp;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class SaveFramesUtils {
    public interface SaveFramesCallback {
        void onSaveComplete();
    }
    public static void saveFramesAsync(final List<Bitmap> frameList, final SaveFramesCallback callback) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                Log.d("FRAMELIST", "doInBackground");
                saveFrames(frameList);
                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                super.onPostExecute(unused);
                Log.d("FRAMELIST", "onPostExecute");
                callback.onSaveComplete();
            }
        }.execute();
    }

    private static void saveFrames(List<Bitmap> frameList) {
        Log.d("FRAMELIST", "saveFrames: frameList size: " + frameList.size());
        File folder;
        if (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) != null) {
            // Save in DOWNLOADS folder
            folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DICOM");
        }else {
            // Save in DCIM folder as fallback
            folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "DICOM");
        }
        if (!folder.exists()) {
            folder.mkdirs();
        }

        for (int i = 0; i < frameList.size(); i++) {
            Log.d("FRAMELIST", "frame list size SAVE: "+frameList.size());
            Bitmap bitmap = frameList.get(i);
            @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "IMG_" + timeStamp + "_" + i + ".jpeg";
            File file = new File(folder, fileName);
            saveBitmapAsJpeg(bitmap, file);
        }
    }

    private static void saveBitmapAsJpeg(Bitmap bitmap, File file) {
        Log.d("FRAMELISTFILE", "saveBitmapAsJpeg: "+file.getAbsolutePath());
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            byte[] jpegByteArray = ImageUtils.bitmapToJpegByteArray(bitmap, 80); // Adjust quality if needed
            fileOutputStream.write(jpegByteArray);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}