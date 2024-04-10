package com.example.earsypay;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;

public class CameraHelper {
    public static final int REQUEST_IMAGE_CAPTURE = 1;

    public static void dispatchTakePictureIntent(Activity activity) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    public static Bitmap handleActivityResult(Intent data) {
        return (Bitmap) data.getExtras().get("data");
    }
}