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
 * Created by Likitha on 4/19/2017.
 */

/*
Thic class keep track of Emotions.
Every emotion result has 2 attributes associated with it - Name and value
 */
public class Emotions {
    public String name;
    public double value;

    public Emotions(String name, double value) {
        this.name = name;
        this.value = value;
    }
}
