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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import com.google.gson.Gson;
import com.microsoft.projectoxford.emotion.contract.*;
import com.microsoft.projectoxford.emotion.rest.EmotionServiceException;
import com.microsoft.projectoxford.face.*;
import com.microsoft.projectoxford.face.contract.*;
import com.microsoft.projectoxford.emotion.EmotionServiceClient;
import com.microsoft.projectoxford.emotion.EmotionServiceRestClient;
import com.microsoft.projectoxford.face.contract.FaceRectangle;
import android.graphics.drawable.BitmapDrawable;

/*
    This is the main activity of the android application.
    When the application starts it creates the custom surface view.
    Double Tap action results in capturing an image bitmap from the surface
    view and then face detection, emotion recognition methods are called
 */
public class MainActivity extends AppCompatActivity {
    Preview preview;  //instance of Preview class
    Camera camera;  //An instance of Camera class which can be used to call inbuilt camera of the mobile
    ImageView image;  //Instance of ImageView class
    private ProgressDialog detectionProgressDialog;  //This is used to show process dialog on the screen
    private FaceServiceClient faceServiceClient =
            new FaceServiceRestClient("PLACE YOUR FACE API KEY HERE");  //Face Detection class instance initialized using Face API key
    private EmotionServiceClient emotionServiceClient =
            new EmotionServiceRestClient("PLACE YOUR EMOTION API KEY HERE");  //Emotion Recognition class instance initialized using Emotion API key
    TextView textView, textView2;  //Instances of TextView class
    TextToSpeech talk;  //An instance  of TextToSpeech class
    static int tracker = 1;  //A counter of that keeps track of tap action

    /*
    This class is called when the application is being created.
    It initializes the global variables.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);  //setting the view of main activity to the application
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);  //This disables the screen to turn off when the application is opened

        //Initializing the global variables and adding to the application main view
        preview = new Preview(this, (SurfaceView) findViewById(R.id.surfaceView));
        ((RelativeLayout) findViewById(R.id.activity_main)).addView(preview);
        preview.setKeepScreenOn(true);
        image = (ImageView) findViewById(R.id.imageView);
        textView = (TextView) findViewById(R.id.textView);
        textView2 = (TextView) findViewById(R.id.textView2);
        detectionProgressDialog = new ProgressDialog(this);
        talk = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    talk.setLanguage(Locale.ENGLISH);
                }
            }
        });

        //Setting the action listener to the entire application screen
        View app = findViewById(R.id.activity_main);
        app.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    camera.startPreview();  //Starting the mobile camera
                    preview.setCamera(camera);  //Setting the camera to surface view
                    if (tracker == 1) {
                        tracker++;
                    } else {
                        tracker = 1;
                        int x = preview.bitmaps.size();  //Extracting the size of bitmap arraylist
                        Bitmap b = preview.bitmaps.get(x - 1);  //Taking the last bitmap in the array
                        image.setImageBitmap(b);  //Setting the bitmap to image view
                        detectAndFrame(b);  //Calling face detection method by passing the bitmap
                    }
                } catch (Exception e) {
                    Log.d("EXCEPTION", e.toString());
                }
            }
        });
    }

    /*
    This class is called when the application starts.
     */
    @Override
    protected void onStart() {
        super.onStart();
        int numCams = Camera.getNumberOfCameras();
        if (numCams > 0) {
            try {
                camera = Camera.open(0);  //opening the camera. '0' is for default inbuilt camera
                camera.startPreview();  //Starting the mobile camera
                preview.setCamera(camera);  //Setting the camera to surface view
            } catch (RuntimeException ex) {
                Log.d("EXCEPTION", ex.toString());
            }
        }
    }

    /*
    This method is called when the application pauses/stops
     */
    @Override
    protected void onPause() {
        if (camera != null) {
            camera.stopPreview();
            preview.setCamera(null);
            camera.release();
            camera = null;
        }
        super.onPause();
    }

    //Method is start camera again
    private void resetCam() {
        camera.startPreview();
        preview.setCamera(camera);
    }

    /*
    This method is for face detection.
    It takes bitmap as input and extracts faces for that bitmap
     */
    private void detectAndFrame(final Bitmap imageBitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        //Attributes that are returned when a face is detected. Here we use 'age' and 'gender'
        final FaceServiceClient.FaceAttributeType[] types = new FaceServiceClient.FaceAttributeType[]{
                FaceServiceClient.FaceAttributeType.Age,
                FaceServiceClient.FaceAttributeType.Gender,
        };
        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(outputStream.toByteArray());
        //The following task takes the compressed bitmap and extracts faces using Face API
        AsyncTask<InputStream, String, Face[]> detectTask =
                new AsyncTask<InputStream, String, Face[]>() {
                    @Override
                    protected Face[] doInBackground(InputStream... params) {
                        try {
                            publishProgress("Detecting...");
                            //Following method call returns faces along with required parameters
                            Face[] result = faceServiceClient.detect(
                                    params[0],
                                    true,         // returnFaceId
                                    false,        // returnFaceLandmarks
                                    types           // returnFaceAttributes: a string like "age, gender"
                            );
                            if (result == null) {
                                publishProgress("Detection Finished. Nothing detected");
                                return null;
                            }
                            publishProgress(
                                    String.format("Detection Finished. %d face(s) detected",
                                            result.length));
                            return result;
                        } catch (Exception e) {
                            publishProgress("Detection failed");
                            return null;
                        }
                    }

                    @Override
                    protected void onPreExecute() {
                        detectionProgressDialog.show();
                    }

                    @Override
                    protected void onProgressUpdate(String... progress) {
                        detectionProgressDialog.setMessage(progress[0]);
                    }

                    //This methos is executed after the detection is complete
                    @Override
                    protected void onPostExecute(Face[] result) {
                        detectionProgressDialog.dismiss();
                        if (result == null) return;
                        //Setting the faces to the images
                        image.setImageBitmap(drawFaceRectanglesOnBitmap(imageBitmap, result));
                        //Setting the detected faces on the image bitmap
                        textView2.setText("  Gender: "+ result[0].faceAttributes.gender+"   Age: "+ result[0].faceAttributes.age);
                        //This method is called for recognizing emotions by passing image bitmap and faces
                        doRecognize(imageBitmap, result);
                    }
                };
        detectTask.execute(inputStream);  //executing the async task
    }

    /*
    This method is called to draw the detected faces on the image bitmap
     */
    private static Bitmap drawFaceRectanglesOnBitmap(Bitmap originalBitmap, Face[] faces) {
        Bitmap bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        //Creating the canvas and paint class instances which are used for drawing
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        int stokeWidth = 5;
        paint.setStrokeWidth(stokeWidth);
        //Extracting face rectangles from the faces
        if (faces != null) {
            for (Face face : faces) {
                FaceRectangle faceRectangle = face.faceRectangle;
                //Drawing the face rectangles on the image bitmap
                canvas.drawRect(
                        faceRectangle.left,
                        faceRectangle.top,
                        faceRectangle.left + faceRectangle.width,
                        faceRectangle.top + faceRectangle.height,
                        paint);
            }
        }
        return bitmap;  //returns the bitmap on which face rectangles are drawn
    }

    /*
    This method is for emotion recognition.
    It takes bitmap and faces and recognizes emotion by calling Emotion API
     */
    private void doRecognize(final Bitmap b, final Face[] faces) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(outputStream.toByteArray());
        AsyncTask<String, String, List<RecognizeResult>> recognizeTask =
                new AsyncTask<String, String, List<RecognizeResult>>() {
                    @Override
                    protected List<RecognizeResult> doInBackground(String... args) {
                        try {
                            return processWithFaceRectangles(b, faces); //Calling the method that returns the emotion list
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                    @Override
                    protected void onPreExecute() {
                        detectionProgressDialog.show();
                    }

                    //This method is executed after emotion is detected
                    @Override
                    protected void onPostExecute(List<RecognizeResult> result) {
                        super.onPostExecute(result);
                        Integer count = 0;
                        // Covert bitmap to a mutable bitmap by copying it
                        //To draw on the bitmap
                        Bitmap bitmapCopy = b.copy(Bitmap.Config.ARGB_8888, true);
                        Canvas faceCanvas = new Canvas(bitmapCopy);
                        faceCanvas.drawBitmap(b, 0, 0, null);
                        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(7);
                        paint.setColor(Color.GREEN);

                        //Initializing emotion class instance to store the emotion list
                        Emotions data[] = new Emotions[8];
                        Emotions req = new Emotions(null, 0);

                        //Extracting emotions from the result
                        for (RecognizeResult r : result) {
                            data[0] = new Emotions("Anger", r.scores.anger);
                            data[1] = new Emotions("Contempt", r.scores.contempt);
                            data[2] = new Emotions("Disgust", r.scores.disgust);
                            data[3] = new Emotions("Fear", r.scores.fear);
                            data[4] = new Emotions("Happiness", r.scores.happiness);
                            data[5] = new Emotions("Neutral", r.scores.neutral);
                            data[6] = new Emotions("Sadness", r.scores.sadness);
                            data[7] = new Emotions("Surprise", r.scores.surprise);

                            //The following code checks which emotion has the highest value
                            req = data[0];
                            for (int i = 0; i < 7; i++) {
                                if (req.value > data[i + 1].value) {
                                    continue;
                                } else {
                                    req = data[i + 1];
                                }
                            }

                            //Displaying emotion on the screen
                            textView.setText("\t Emotion:  " + req.name);

                            //Emotion text is converted to speech
                            talk.speak(req.name, TextToSpeech.QUEUE_FLUSH, null);

                            //Drawing again on the bitmap after emotion recognition
                            faceCanvas.drawRect(r.faceRectangle.left,
                                    r.faceRectangle.top,
                                    r.faceRectangle.left + r.faceRectangle.width,
                                    r.faceRectangle.top + r.faceRectangle.height,
                                    paint);
                            count++;
                        }
                        image.setImageDrawable(new BitmapDrawable(getResources(), bitmapCopy));
                    }
                };
        recognizeTask.execute();  //executing the task
    }

    /*
    This method calls the Emotion API and returns the emotion list for the bitmap
     */
    private List<RecognizeResult> processWithFaceRectangles(Bitmap mBitmap, Face[] faces) throws EmotionServiceException, com.microsoft.projectoxford.face.rest.ClientException, IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());
        com.microsoft.projectoxford.emotion.contract.FaceRectangle[] faceRectangles = null;
        //Extracting face rectangles from faces object
        if (faces != null) {
            faceRectangles = new com.microsoft.projectoxford.emotion.contract.FaceRectangle[faces.length];
            for (int i = 0; i < faceRectangles.length; i++) {
                com.microsoft.projectoxford.face.contract.FaceRectangle rect = faces[i].faceRectangle;
                faceRectangles[i] = new com.microsoft.projectoxford.emotion.contract.FaceRectangle(rect.left, rect.top, rect.width, rect.height);
            }
        }
        List<RecognizeResult> result = null;
        if (faceRectangles != null) {
            inputStream.reset();
            result = this.emotionServiceClient.recognizeImage(inputStream, faceRectangles);  //Calling the Emotion API
        }
        return result;  //Returning the emotion list result
    }
}
