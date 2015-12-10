package com.example.jying.androidannotations.support;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;

import com.example.jying.androidannotations.R;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by jying on 5/21/2015.
 */
public class FileManager {

    public static File getOutputJPGFile(Context context, String filename) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Log.d("FileManager", "SD card not mounted");
        }

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), context.getResources().getString(R.string.app_name));

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("FileManager", "Failed to create directory!");
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        return new File(mediaStorageDir.getPath() + File.separator + filename + ".jpg");
    }

    public static File writeImageDataToFile(Context context, String filename, byte[] data) {
        File outFile = FileManager.getOutputJPGFile(context, filename);
        try {
            FileOutputStream fos = new FileOutputStream(outFile);
            fos.write(data);
            fos.close();
        } catch (Exception e) {
            Log.d("FileManager", "Unable to write picture data to file.");
        }
        MediaScannerConnection.scanFile(context, new String[]{outFile.getPath()}, new String[]{"image/jpeg"}, null);
        return outFile;
    }
}
