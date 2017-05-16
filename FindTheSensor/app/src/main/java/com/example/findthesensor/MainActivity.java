package com.example.findthesensor;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements SensorEventListener {
//implements SensorEventListener

    // 센서의 값을 저장하기 위한 변수
    SensorManager sm;
    ArrayList<Sensor> existSensor = new ArrayList<>();
    int sizeOfList;
    int fileIndex =1;
    ArrayList<Float> valueList = new ArrayList<Float>();

    // Json으로 저장하기 위한 Object.
    JSONObject obj = new JSONObject();

    // Sensor값 변수
    Float[] GYP_value = new Float[3];
    Float[] ACC_value = new Float[3];
    Float[] MAG_value = new Float[3];
    Float[] ORI_value = new Float[3];
    Float PRO_value;
    Float LIG_value;

    // 파일을 저장하기 위한 Thread (1초에 한번씩)
    private class fileSaveThread extends Thread {
        private static final String TAG = "fileSaveThread";

        public fileSaveThread() {
            // 초기화 작업
        }

        public void run() {

            int second = 0;

            while (true) {
                second++;
                try {
                    makeFile();
                    fileIndex++;
                    Thread.sleep(1000); // 2초간 Thread를 잠재운다
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    ;
                }
                //Log.i("경과된 시간 : ", Integer.toString(second));
            }
        }
    }

    private class fileSendThread extends Thread {
        private static final String TAG = "fileSendThread";

        public fileSendThread() {
            // 초기화 작업
        }

        public void run() {

            int second = 0;

            while (true) {
                second++;
                try {
                    insert();
                    Thread.sleep(5000); // 5초간 Thread를 잠재운다
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //Log.i("파일 업로드 시간 : ", Integer.toString(second));
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tv = (TextView) findViewById(R.id.textView1);

        // 센서 장치 목록 확인하기

        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> list = sm.getSensorList(Sensor.TYPE_ALL);
        sizeOfList = list.size();
        String str = "<센서목록>\n센서 총개수: " + sizeOfList;


        for (int i = 0; i < list.size(); i++) {
            Sensor s = list.get(i);
            str += "\n\n" + i + ", 센서이름:" + s.getName()
                    + "\n" + "센서번호:" + s.getType();
            existSensor.add(sm.getDefaultSensor(s.getType()));
        }
        tv.setText(str);

        // 파일 저장 Thread 실행
        Thread fileSavet = new fileSaveThread();
        fileSavet.start();

        Thread fileSendt = new fileSendThread();
        fileSendt.start();

    } // End of OnCreate


    //센서에 대한 딜레이 설정. 이걸 해 줘야 리스너값을 받아오기 위함.
    @Override
    protected void onResume() {
        super.onResume();


        for(int i=0; i<sizeOfList; i++)
        {
            sm.registerListener(this, existSensor.get(i),  SensorManager.SENSOR_DELAY_NORMAL);
        }

    }


    @Override
    public void onSensorChanged(SensorEvent event) {

        //여기서 센서값이 변하는걸 체크한다. 현재 GYROSCOPE,ACCELEROMETER,LIGHT,PROXIMITY 값 등 일부의 값을 받아온다.
        // 나중에 step이라든지 추가하면 됨..

        switch (event.sensor.getType()) {
            case Sensor.TYPE_GYROSCOPE:
                //Log.d("tag", "TYPE_GYROSCOPE: X"+event.values[0]+"Y " + event.values[1] + "Z " + event.values[2]);
                GYP_value[0] = event.values[0];
                GYP_value[1] = event.values[1];
                GYP_value[2] = event.values[2];
                break;
            case Sensor.TYPE_ACCELEROMETER:
                //Log.d("tag", "TYPE_ACCELEROMETER: X"+event.values[0]+"Y " + event.values[1] + "Z " + event.values[2]);
                ACC_value[0] = event.values[0];
                ACC_value[1] = event.values[1];
                ACC_value[2] = event.values[2];
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                //Log.d("tag", "TYPE_MAGNETIC_FILED: X"+event.values[0]+" Y " + event.values[1] + " Z " + event.values[2]);
                MAG_value[0] = event.values[0];
                MAG_value[1] = event.values[1];
                MAG_value[2] = event.values[2];
                break;
            case Sensor.TYPE_PROXIMITY:
                //Log.d("tag", "TYPE_PROXIMITY: "+ event.values[0]);
                PRO_value = event.values[0];
                break;
            case Sensor.TYPE_LIGHT:
                //Log.d("tag", "TYPE_LIGHT: "+event.values[0]);
                LIG_value = event.values[0];
                break;
            case Sensor.TYPE_ORIENTATION:
                //Log.d("tag", "TYPE_ORIENTATION: X"+event.values[0]+"Y " + event.values[1] + "Z " + event.values[2]);
                ORI_value[0] = event.values[0];
                ORI_value[1] = event.values[1];
                ORI_value[2] = event.values[2];
                break;
//등등으로 쭉 나간다.
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //정확도 변경시 사용된다는것 같은데 정확한 용도는 잘 모르겠다. 사용되는건 한번도 못 본듯 하다.
    }



    ///이제부터 파일저장해야됨.
    public void makeFile(){

        String dirPath = getFilesDir().getAbsolutePath();
        String addPath = dirPath+"/example2";
        Log.d("Directory Path : ", addPath);
        File folder = new File(addPath);

        if( !folder.exists() ) {
            folder.mkdirs();
        }

        try{

            ArrayList<Float> ACCValue = new ArrayList<Float>();
            ACCValue.add(ACC_value[0]);
            ACCValue.add(ACC_value[1]);
            ACCValue.add(ACC_value[2]);
            //obj.put("TYPE_ACCELEROMETER", new JSONArray(ACCValue));

            ArrayList<Float> LIGValue = new ArrayList<Float>();
            LIGValue.add(LIG_value);
           // obj.put("TYPE_LIGHT", new JSONArray(LIGValue));

            ArrayList<Float> GYPValue = new ArrayList<Float>();
            GYPValue.add(GYP_value[0]);
            GYPValue.add(GYP_value[1]);
            GYPValue.add(GYP_value[2]);
            //obj.put("TYPE_GYROSCOPE", new JSONArray(GYPValue));

            ArrayList<Float> PROVaule = new ArrayList<Float>();
            PROVaule.add(PRO_value);
           // obj.put("TYPE_PROXIMITY", new JSONArray(PROVaule));

            ArrayList<Float> ORIValue = new ArrayList<Float>();
            ORIValue.add(ORI_value[0]);
            ORIValue.add(ORI_value[1]);
            ORIValue.add(ORI_value[2]);
           // obj.put("TYPE_ORIENTATION", new JSONArray(ORIValue));

            ArrayList<Float> exampleValue = new ArrayList<Float>();
            exampleValue.add(ACC_value[0]);
            exampleValue.add(ACC_value[1]);
            exampleValue.add(ACC_value[2]);
            obj.put("sensor", new JSONArray(exampleValue));

        }
        catch (JSONException e) {
            e.printStackTrace();
        }


        try {
            //임의의 장소에 저장.
            File savefile = new File(addPath+"/"+fileIndex+".txt");
            FileWriter file = new FileWriter(savefile);
            file.write(obj.toString());
            file.flush();
            file.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        //파일 읽어오는 부분//

    }

    //insert 시작

    public void insert(){

        try{

            String dirPath = getFilesDir().getAbsolutePath();
            String addPath = dirPath+"/example2";
            File folder = new File(addPath);

            String link= "http://13.124.80.88:8080/insert";

            if (folder.listFiles().length > 0 ) {
                Log.d("tag", "How Many Files there? " + folder.listFiles().length);
                for (File f : folder.listFiles()) {
                    String str = f.getName();

                    // 파일 내용 읽어오기
                    String loadPath = addPath + "/" + str;
                    try {
                        FileInputStream fis = new FileInputStream(loadPath);
                        BufferedReader bufferReader = new BufferedReader(new InputStreamReader(fis));

                        String content = "", temp = "";
                        while ((temp = bufferReader.readLine()) != null) {
                            content += temp;
                        }

                        Log.d("tag", "contents content " + content);

                        String data = content;

                        URL url = new URL(link);
                        URLConnection conn = url.openConnection();

                        conn.setDoOutput(true);
                        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());

                        wr.write(data);
                        wr.flush();

                        // 저장할때 필요함 삭제 ㄴㄴ여
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                        StringBuilder sb = new StringBuilder();
                        String line = null;

                        //파일삭제
                        f.delete();


                    } catch (Exception e) {
                        Log.d("tag","Exception@@@@@@"+e.getMessage());
                    }
                }
            }



        }
        catch(Exception e){

        }

    }


} // end of class