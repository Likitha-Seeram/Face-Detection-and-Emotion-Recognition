# Face-Detection-and-Emotion-Recognition
This project collects images from surface view upon a double tap on the screen. Face detection and Emotion Recognition processes are done on these images using Microsoft Cognitive services.

What does the app do?
1) Collects images from the camera view in the application upon double tapping the screen
2) Detects faces in the image
3) Recognizes the emotion in the image
4) Gives a speech notification of the emotion recognized

Steps to use this application:
1) Download the project and open it using Android Studio
2) Register for Microsoft Cognitive Services - Face API and Emotion API. Take the keys and place them in MainActivity class
3) Make sure you have all the required SDK downloaded in your IDE. (This app is compatible from Android sdk level 16 - 23)
4) Make a gradle build and run the app by connecting your android device
5) Make sure you are viewing a face in camera view and then double tap on the screen to take a picture
6) The image at the moment is captured and then face is detected and then emotion is recognized

Limitation:
Though the app detects many faces in the image, this app approach is limited to recognizing only one emotion in an image. Hence use it for detecting only one face in an image.
