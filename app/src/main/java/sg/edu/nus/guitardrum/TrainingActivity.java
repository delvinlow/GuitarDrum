package sg.edu.nus.guitardrum;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;


public class TrainingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);

        final ArrayList<String> labels = new ArrayList<String>(Arrays.asList("front", "back", "up", "down", "left", "right", "nothing"));

        final Button trainingButton = (Button)findViewById(R.id.button_choose_text_file);
        trainingButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                String data= "";
                InputStream inputStream = TrainingActivity.this.getResources().openRawResource(R.raw.results);

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                // Do reading, usually loop until end of file reading
                StringBuilder sb = new StringBuilder();
                ArrayList<Double> features;
                try{
                    String mLine = reader.readLine();
                    System.out.println("First line " + mLine);
                    int counter = 0;
                    int xyz_counter = 0; // 0 to 2 corresponding to x, y or z
                    double sampling_rate = 0.0;
                    String label = "Unknown";

                    while (mLine != null) {

                        sb.append(mLine);
                        // process line
                        mLine = reader.readLine();

                        counter += 1;
                        counter = counter % 5;
                        if (mLine == null){ // End of file
                            break;
                        }
                        //File should have a series of 5 lines: fs, label, x, y, z
                        // Add newline every 1st line
                        if (counter == 0){
                            data = data.concat("\n");
                            sampling_rate = Double.valueOf(mLine);
                        } else if (counter == 1){ // Add label every 2nd line
                            label = mLine.toLowerCase();
                            data = data.concat(String.valueOf(labels.indexOf(label)) + " ");
                        } else {
                            // Call features extractor every 3rd to 5th line
                            // Read in data from text file, convert to Double and extract FEATURES
                            String[] readings = mLine.split(" ");
                            double[] numbers = new double[readings.length];
                            for (int i = 0; i < readings.length; ++i) {
                                double number = Double.parseDouble(readings[i]);
                                numbers[i] = number;
                            }
                            xyz_counter = xyz_counter % 3;

                            // Call features extractor every 3rd to 5th line
                            FeaturesExtractor fe = new FeaturesExtractor(numbers, sampling_rate);
                            features = fe.calculateFeatuers();
                            for (int j=1; j<= features.size(); j++){
                                data = data.concat(String.valueOf(j + (xyz_counter * features.size()) + ":" + String.valueOf(features.get(j-1)) + " "));
                            }
                            data = data.trim();
                            xyz_counter += 1;
                        }
                    }
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(TrainingActivity.this, "The text file format is incorrect", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println(data);

                writeToFile(data, TrainingActivity.this);
            }

        });
    }

    private void writeToFile(String data,Context context) {
        try {
            String outputfile = "training.txt";
            // Get the directory for the user's public Downloads directory.
            File file = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), outputfile);

            FileOutputStream   fOut        = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(data);
            myOutWriter.close();
            fOut.close();
            Toast.makeText(context, "Success! Saved into Downloads/" + outputfile , Toast.LENGTH_SHORT).show();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }


    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

}