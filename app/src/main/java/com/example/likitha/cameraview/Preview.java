/*
References:
Microsoft Cognitive Services
1) Face API
   https://www.microsoft.com/cognitive-services/en-us/face-api
   https://docs.microsoft.com/en-us/azure/cognitive-services/Face/Tutorials/FaceAPIinJavaForAndroidTutorial
2) Emotion API
   https://www.microsoft.com/cognitive-services/en-us/emotion-api
3) Android Developers site
   https://developer.android.com/reference/android/hardware/Camera.html
   https://developer.android.com/reference/android/media/MediaMetadataRetriever.html?
   https://developer.android.com/reference/android/app/Activity.html
4) GitHub
   https://github.com/Microsoft/Cognitive-face-android
   https://github.com/Microsoft/Cognitive-emotion-android
   https://github.com/josnidhin/Android-Camera-Example
   https://github.com/josnidhin/Android-Camera-Example/blob/master/src/com/example/cam/CamTestActivity.java
5) Stack Overflow
   http://stackoverflow.com/questions/3401579/get-filename-and-path-from-uri-from-mediastore
   http://stackoverflow.com/questions/29546381/open-camera-directly-in-the-activity-without-clicking-on-button-and-without-inte
   http://stackoverflow.com/questions/10749198/nullpointerexception-while-setting-image-in-imageview-from-bitmap
   http://stackoverflow.com/questions/20097698/getting-image-data-continuously-from-camera-surfaceview-or-surfaceholder
6) Microsoft COC
   https://www.microsoft.com/cognitive-services/en-us/legal/DeveloperCodeofConductforCognitiveServices20161121
 */

package com.example.likitha.cameraview;

/**
 * Created by Likitha on 4/17/2017.
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.BitmapFactory;

//This class is used for creating a custom surface view widget which acts as a camera
class Preview extends ViewGroup implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private final String TAG = "Preview";
    SurfaceView mSurfaceView;
    SurfaceHolder mHolder;
    Size mPreviewSize;
    List<Size> mSupportedPreviewSizes;
    Camera mCamera;
    ArrayList<Bitmap> bitmaps = new ArrayList<Bitmap>(); //Array list that saves all the bitmaps

    Preview(Context context, SurfaceView sv) {
        super(context);
        mSurfaceView = sv;
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    //Method for setting camera to surface view
    public void setCamera(Camera camera) {
        mCamera = camera;
        if (mCamera != null) {
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            requestLayout();
            // get Camera parameters
            Camera.Parameters params = mCamera.getParameters();
            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                // set the focus mode
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                // set Camera parameters
                mCamera.setParameters(params);
            }
        }
        mCamera.setOneShotPreviewCallback(this);  //To take a screen shot of the surface view
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);
        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);
            final int width = r - l;
            final int height = b - t;
            int previewWidth = width;
            int previewHeight = height;
            if (mPreviewSize != null) {
                previewWidth = mPreviewSize.width;
                previewHeight = mPreviewSize.height;
            }
            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2,
                        width, (height + scaledChildHeight) / 2);
            }
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        //When the surface view is created, camera instance is taken and is set to it
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }

    //To stop the camera preview when surface destroys
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;
        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        int targetHeight = h;
        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }
        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    //This method is called when the surface changes. It sets the camera preview again
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            requestLayout();
            mCamera.setParameters(parameters);
            mCamera.startPreview();
        }
    }

    /*
    This method is called when a screen capture of surface view happens.
    Here we compress the frame and decode it into bitmap
     */
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, previewSize.width, previewSize.height, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 80, baos);
        byte[] jdata = baos.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(jdata, 0, jdata.length);
        bitmaps.add(bitmap);  //Adding the bitmap to array list
    }
}