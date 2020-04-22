package com.example.opencvtest;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.content.Context;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Array;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import static android.os.Environment.getExternalStoragePublicDirectory;


public class MainActivity extends AppCompatActivity {

    public static final int ImageGalleryReq = 20;

    String pathToFile,location1,location2;
    ImageView ImgPic,testView;
    Bitmap testBitmap,img;
    InputStream inputStream;
    Button buttonProc, buttonGal,b1;
    TextToSpeech t1;
    public Context mContext;
    Color ballColour;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImgPic =  findViewById(R.id.ImgView);
        testView=findViewById(R.id.imageView1);
        b1= findViewById(R.id.button2);
        buttonGal=findViewById(R.id.buttonGal);
        buttonProc=findViewById(R.id.button);
        t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener(){
            @Override
            public void onInit(int status){
                if(status==TextToSpeech.SUCCESS){
                    int result = t1.setLanguage(Locale.UK);
                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result== TextToSpeech.LANG_NOT_SUPPORTED){
                        Log.e("TTS","Language not supported");
                    }else {
                        b1.setEnabled(true);
                    }
                }else{
                    Log.e("TTS","Initialization failed");
                }
            }

        });
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String toSpeak= "Text to speech is working";
                Toast.makeText(getApplicationContext(),toSpeak,Toast.LENGTH_SHORT).show();
                t1.speak(toSpeak,TextToSpeech.QUEUE_FLUSH,null);
            }
        });
        if (OpenCVLoader.initDebug()){
            Toast.makeText(getApplicationContext(),"OpenCV Loaded Successfully",Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(getApplicationContext(),"opencv load FAIL",Toast.LENGTH_LONG).show();
        }
        buttonGal.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                onLoadImage();
            }
        });
        buttonProc.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onClick(View v) {
                convertGray();
            }
        });
    }
    public void onLoadImage() {
        Intent loadImg = new Intent(Intent.ACTION_PICK);
        File picDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String picDirPath = picDir.getPath();
        Uri data = Uri.parse(picDirPath);
        loadImg.setDataAndType(data, "image/*");
        startActivityForResult(loadImg, ImageGalleryReq);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if (resultCode == RESULT_OK) {
            if (requestCode == ImageGalleryReq) {
                assert data != null;
                Uri imageUri = data.getData();
                try {
                    assert imageUri != null;
                    inputStream = getContentResolver().openInputStream(imageUri);
                    img = BitmapFactory.decodeStream(inputStream);
                    ImgPic.setImageBitmap(img);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Unable to open img", Toast.LENGTH_LONG).show();
                }
            }
            if (requestCode == 1) {
                Bitmap bitmap1 = BitmapFactory.decodeFile(pathToFile);
                ImgPic.setImageBitmap(bitmap1);
            }
        }else {
            Toast.makeText(getApplicationContext(),"Error-result", Toast.LENGTH_LONG).show();
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void convertGray() {
        if (img != null) {
            Mat mat = new Mat(img.getWidth(), img.getHeight(),CvType.CV_8UC1);
            Mat greyMat = new Mat(img.getWidth(), img.getHeight(),CvType.CV_8UC1);
            Mat cannyMat = new Mat(img.getWidth(), img.getHeight(),CvType.CV_8UC1);
            Bitmap testBitmap = img.copy(img.getConfig(),true);
            Utils.bitmapToMat(img, mat);
            Imgproc.cvtColor(mat, greyMat, Imgproc.COLOR_BGR2GRAY);
            Imgproc.GaussianBlur(greyMat, cannyMat, new Size(9, 9), 0);
            Imgproc.adaptiveThreshold(cannyMat, cannyMat, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 5, 4);
            double dp = 1d;
            double minDist = 500;
            int minRadius = 100, maxRadius = 800;
            double param1 = 70, param2 = 72;
            Mat circles = new Mat(img.getWidth(),
                    img.getHeight(), CvType.CV_8UC1);
            Imgproc.HoughCircles(cannyMat, circles,
                    Imgproc.CV_HOUGH_GRADIENT, dp, minDist, param1,param2, minRadius, maxRadius);
            int numberOfCircles = (circles.rows() == 0) ? 0 : circles.cols();
            for (int i=0; i<numberOfCircles; i++) {
                double[] circleCoordinates = circles.get(0, i);
                int x = (int) circleCoordinates[0], y = (int) circleCoordinates[1];
                Point centre = new Point(x, y);
                int radius = (int) circleCoordinates[2];
                Imgproc.circle(mat, centre, radius, new Scalar(0,255,0),10);
                Imgproc.rectangle(mat, new Point(x - 5, y - 5),new Point(x + 5, y + 5),new Scalar(0, 128, 255), 5);
                ballColour =  img.getColor(x,y);
                if (centre.y > img.getHeight()) {
                    location1 = "top";
                }if  (centre.y < img.getHeight()){
                        location1 = "bottom";
                }if (centre.x > img.getWidth()){
                    location2 = "right";
                }if (centre.x < img.getWidth()){
                    location2 = "left";
                }
            }
            Utils.matToBitmap(mat,img);
            Utils.matToBitmap(cannyMat,testBitmap);
            ImgPic.setImageBitmap(img);
            testView.setImageBitmap(testBitmap);
            String toSpeak= "Image processing finished, there are"+numberOfCircles+"balls detected, The colour of the balls are"+ballColour+"the balls are located at the"+location1+location2;
            Toast.makeText(getApplicationContext(),toSpeak,Toast.LENGTH_SHORT).show();
            t1.speak(toSpeak,TextToSpeech.QUEUE_FLUSH,null);
        }
    }
}






