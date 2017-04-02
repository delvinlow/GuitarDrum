package sg.edu.nus.guitardrum;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.skyfishjy.library.RippleBackground;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import weka.classifiers.Classifier;

/*
            Make sure to Sync Project With Gradle Files (Tools -> Android -> Sync Project With Gradle Files) and rebuild the app
            for usage of RippleBackground refer to: https://github.com/skyfishjy/android-ripple-background
*/

public class GuitarActivity extends AppCompatActivity implements SensorEventListener {
    /** Called when the activity is first created. */
    //variables for checking sampling rate
    private final static long sampling_interval = 10000000; //ns
    private final static long sampling_interval_error_margin = 2000000;//20%
    private double last_x;
    private double last_y;
    private double last_z;
    private long lastTimeStamp;
    //Buffer variables
    private boolean bufferisReady = false;
    private double[][] buffer;
    private double[][] nextBuffer;
    private final static int bufferLen = 100;
    private final static int bufferOverlap = 25;
    //    private final static int bufferLen = 64;
//    private final static int bufferOverlap = 32;
    private int bufferIndex;

    private SensorManager     mSensorManager;
    private Sensor            mAccelerometer;
    private FeaturesExtractor featureExtractor_x;
    private FeaturesExtractor featureExtractor_y;
    private FeaturesExtractor featureExtractor_z;
//    private J48       tree;
    private Classifier cModel;
//    Classifier cModel;
    TextView tv_label;
    final ArrayList<String> labels = new ArrayList<String>(Arrays.asList("front", "back", "up", "down", "left", "right", "standing"));

    SoundSynthesizer synthesizer_E, synthesizer_A, synthesizer_D, synthesizer_G,
            synthesizer_B, synthesizer_E_thick;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guitar);

        tv_label = (TextView) findViewById(R.id.textView5);

        // For playing guitar sounds when acceleration detected
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // For Buffers
        buffer = new double[3][bufferLen];
        nextBuffer = new double[3][bufferLen];
        bufferIndex = 0;

        featureExtractor_x = null;
        featureExtractor_y = null;
        featureExtractor_z = null;

        synthesizer_E = new SoundSynthesizer("E");
        synthesizer_A = new SoundSynthesizer("A");
        synthesizer_D = new SoundSynthesizer("D");
        synthesizer_G = new SoundSynthesizer("G");
        synthesizer_B = new SoundSynthesizer("B");
        synthesizer_E_thick = new SoundSynthesizer("E");

        // deserialize model
        try{
            ObjectInputStream ois = new ObjectInputStream(
                    new FileInputStream(Environment.getExternalStorageDirectory()+"/Download/classifier.model"));
            cModel = (Classifier) ois.readObject();
            ois.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Downloads/classifier.model not found. Run TrainingActivity First.", Toast.LENGTH_SHORT).show();
        } catch (ClassNotFoundException e){

        }


        FloatingActionButton fab1 = (FloatingActionButton)findViewById(R.id.string_button_1);
        FloatingActionButton fab2 = (FloatingActionButton)findViewById(R.id.string_button_2);
        FloatingActionButton fab3 = (FloatingActionButton)findViewById(R.id.string_button_3);
        FloatingActionButton fab4 = (FloatingActionButton)findViewById(R.id.string_button_4);
        FloatingActionButton fab5 = (FloatingActionButton)findViewById(R.id.string_button_5);
        FloatingActionButton fab6 = (FloatingActionButton)findViewById(R.id.string_button_6);

        fab1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startAllAnimation(1);
                if (synthesizer_E.isRunning){
                    synthesizer_E.stop();
                } else {
                    synthesizer_E.play();
                }
            }
        });
        fab2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startAllAnimation(2);
                if (synthesizer_A.isRunning){
                    synthesizer_A.stop();
                } else {
                    synthesizer_A.play();
                }
            }
        });
        fab3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startAllAnimation(3);
                if (synthesizer_D.isRunning){
                    synthesizer_D.stop();
                } else {
                    synthesizer_D.play();
                }
            }
        });
        fab4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startAllAnimation(4);
                if (synthesizer_G.isRunning){
                    synthesizer_G.stop();
                } else {
                    synthesizer_G.play();
                }
            }
        });
        fab5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startAllAnimation(5);
                if (synthesizer_B.isRunning){
                    synthesizer_B.stop();
                } else {
                    synthesizer_B.play();
                }
            }
        });
        fab6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startAllAnimation(6);
                if (synthesizer_E_thick.isRunning){
                    synthesizer_E_thick.stop();
                } else {
                    synthesizer_E_thick.play();
                }
            }
        });
    }

    public void startAllAnimation(int index) {
        final RelativeLayout relativeLayout = (RelativeLayout)findViewById(R.id.activity_guitar);
        startRippleAnimation(index);
        startColorTransition(index);
    }

    private void startColorTransition(int index) {
        final RelativeLayout relativeLayout = (RelativeLayout)findViewById(R.id.activity_guitar);

        int color = Color.TRANSPARENT;
        Drawable background = relativeLayout.getBackground();
        if (background instanceof ColorDrawable) color = ((ColorDrawable) background).getColor();
        int colorStart = color;
        int colorEnd = Color.TRANSPARENT;
        switch (index) {
            case 1:
                colorEnd =  ContextCompat.getColor(getApplicationContext(), R.color.colorGuitarButton1);
                break;
            case 2:
                colorEnd =  ContextCompat.getColor(getApplicationContext(), R.color.colorGuitarButton2);
                break;
            case 3:
                colorEnd =  ContextCompat.getColor(getApplicationContext(), R.color.colorGuitarButton3);
                break;
            case 4:
                colorEnd =  ContextCompat.getColor(getApplicationContext(), R.color.colorGuitarButton4);
                break;
            case 5:
                colorEnd =  ContextCompat.getColor(getApplicationContext(), R.color.colorGuitarButton5);
                break;
            case 6:
                colorEnd =  ContextCompat.getColor(getApplicationContext(), R.color.colorGuitarButton6);
                break;
        }

        ValueAnimator colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), colorStart, colorEnd);

        colorAnim.setEvaluator(new ArgbEvaluator());
        colorAnim.setDuration(1500);

        colorAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                relativeLayout.setBackgroundColor((int) animation.getAnimatedValue());
            }
        });
        colorAnim.start();
    }

    public void startRippleAnimation(int index) {
        final RippleBackground rippleBackground;
        switch (index) {
            case 1:
                rippleBackground=(RippleBackground)findViewById(R.id.ripple_bg_1);
                break;
            case 2:
                rippleBackground=(RippleBackground)findViewById(R.id.ripple_bg_2);
                break;
            case 3:
                rippleBackground=(RippleBackground)findViewById(R.id.ripple_bg_3);
                break;
            case 4:
                rippleBackground=(RippleBackground)findViewById(R.id.ripple_bg_4);
                break;
            case 5:
                rippleBackground=(RippleBackground)findViewById(R.id.ripple_bg_5);
                break;
            case 6:
                rippleBackground=(RippleBackground)findViewById(R.id.ripple_bg_6);
                break;
            default:
                rippleBackground=(RippleBackground)findViewById(R.id.ripple_bg_1);
        }
        playRippleAnimation(rippleBackground);
    }

    public void playRippleAnimation(final RippleBackground rb) {
        rb.startRippleAnimation();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                rb.stopRippleAnimation();
            }
        }, 1500);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        makeBuffer(event);
        if (bufferisReady) {
            doSomeCalculations(buffer);
            bufferisReady = false;
            //copy nextBuffer into buffer
            for (int i = 0; i < bufferOverlap; i++) {
                for (int j = 0; j < 3; j++) {
                    buffer[j][i] = nextBuffer[j][i];
                }
            }
        }
    }


    private void makeBuffer(SensorEvent event){
        int sensor = event.sensor.getType();
        float[] values = event.values;



        if(sensor == Sensor.TYPE_ACCELEROMETER) {
            if(lastTimeStamp == 0){
                lastTimeStamp = event.timestamp;
            }
            else {
                long timeInterval = event.timestamp - lastTimeStamp;
                if (timeInterval > 5) {
                    buffer[0][bufferIndex] = values[0];
                    buffer[1][bufferIndex] = values[1];
                    buffer[2][bufferIndex] = values[2];
                    last_x = values[0];
                    last_y = values[1];
                    last_z = values[2];
                    lastTimeStamp = event.timestamp;
                    bufferIndex += 1;
                    //buffer is full
                    if (bufferIndex == bufferLen) {
                        bufferisReady = true;
                        //copy values into new buffer
                        for (int i = 0; i < bufferOverlap; i++) {
                            for (int j = 0; j < 3; j++) {
                                nextBuffer[j][i] = buffer[j][i + bufferOverlap];
                            }
                        }
                        bufferIndex = bufferOverlap;
                    }
                }

            }
        }
    }

    private void doSomeCalculations(double[][] buffer){
        List<Double> combined_features = new ArrayList<Double>();
        try {
            Log.v("Buffer is ", Arrays.toString(buffer[0]));
            featureExtractor_x = new FeaturesExtractor(buffer[0], 48);
            featureExtractor_y = new FeaturesExtractor(buffer[1], 48);
            featureExtractor_z = new FeaturesExtractor(buffer[2], 48);
        } catch (Exception e) {
            Toast.makeText(this, "FeaturesExtractor cannot launch", Toast.LENGTH_SHORT).show();
        }

        try {
            combined_features.clear();
            combined_features.addAll(featureExtractor_x.calculateFeatuers());
            combined_features.addAll(featureExtractor_y.calculateFeatuers());
            combined_features.addAll(featureExtractor_z.calculateFeatuers());
            System.out.println(Arrays.toString(combined_features.toArray()));
//            d.calculateFeatuersMean();
        } catch (Exception e) {
            Toast.makeText(this, "Features problem", Toast.LENGTH_SHORT).show();
        }
        classify(combined_features);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        synthesizer_E.stop();
        synthesizer_A.stop();
        synthesizer_D.stop();
        synthesizer_G.stop();
        synthesizer_B.stop();
        synthesizer_E_thick.stop();
    }

    private void classify(List<Double> list){
        // Call machine learning to classify new buffer features
//
//            Double[] doubleArray = {7.455246499373751,60.0,0.29087540648495147,370.59837721885856,
//                    39.150463030765216,12.734256465601685,60.0,0.18091646650363757,385.34431007080315,
//                    31.89150908203104, 5.351423635325828,53.0,0.0017771684478375433,319.40381718791707,
//                    31.948380719894285}; //up. Expected: 5.0
////            Double[] doubleArray = {4.600646720617296,36.0,-0.12234045583418018,292.38929804443194,
////                    24.408694383140627,7.724959309821786,0.0,-0.8577749609120245,475.6558379766474,
////                    2.8731960701758026,14.837271391181428,66.0,0.5127651790434414,348.79690502685065,
////                    43.45089525554437}; // back. Expected: 0.0
//            List<Double> list = Arrays.asList(doubleArray);

            ActionClassifier ac = new ActionClassifier();
            double result = ac.classify(cModel, list);
            int x = (int) result;
            String action_label = labels.get(x);
//            Toast.makeText(this, "Class is " + action_label, Toast.LENGTH_SHORT).show();
            tv_label.setText(action_label);



            // Modulate the sound based on result
            modulateSoundBasedOnAction(action_label);

    }
    public void modulateSoundBasedOnAction(String action_label){
        if (action_label.equalsIgnoreCase("front")){
//            synthesizer_E.makeFaster();
        } else if (action_label.equalsIgnoreCase("back")){
//            synthesizer_E.makeSlower();
        } else if (action_label.equalsIgnoreCase("left")) {
//            synthesizer_E.makePrevious();
        } else if (action_label.equalsIgnoreCase("right")) {
//            synthesizer_E.makeNext();
        } else if (action_label.equalsIgnoreCase("up")) {
            //increase volume
//            synthesizer_E.makeLouder();
        } else if (action_label.equalsIgnoreCase("down")) {
            // decrease volume
//            synthesizer_E.makeSofter();
        } else{
            // Do nothing
        }
    }


}
