package com.example.findthesensor;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Handler;

import static java.lang.Thread.sleep;

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

    // 블루투스
    /*
    UUID SPP_UUID = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //블루투스 Adapter를 가져온다
    BluetoothAdapter mBlueToothAdapter = BluetoothAdapter.getDefaultAdapter();
    */

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
                //onReceive();
                try {
                    makeFile();
                    fileIndex++;
                    Thread.sleep(1000); // 1초간 Thread를 잠재운다
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    ;
                }
                Log.i("경과된 시간 : ", Integer.toString(second));
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
                    Log.d("tag", "TYPE_PROXIMITY: "+ event.values[0]);
                    PRO_value = event.values[0];
                    break;
                case Sensor.TYPE_LIGHT:
                    //Log.d("tag", "TYPE_LIGHT: "+event.values[0]);
                    LIG_value = event.values[0];
                    break;
                case Sensor.TYPE_ORIENTATION:
                    Log.d("tag", "TYPE_ORIENTATION: X"+event.values[0]+"Y " + event.values[1] + "Z " + event.values[2]);
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
        Log.d("Directory Path : ", dirPath);
        File folder = new File(dirPath);

        if( !folder.exists() ) {
            folder.mkdirs();
        }

        try{

            ArrayList<Float> ACCValue = new ArrayList<Float>();
            ACCValue.add(ACC_value[0]);
            ACCValue.add(ACC_value[1]);
            ACCValue.add(ACC_value[2]);
            obj.put("TYPE_ACCELEROMETER", new JSONArray(ACCValue));

            ArrayList<Float> LIGValue = new ArrayList<Float>();
            LIGValue.add(LIG_value);
            obj.put("TYPE_LIGHT", new JSONArray(LIGValue));

            ArrayList<Float> GYPValue = new ArrayList<Float>();
            GYPValue.add(GYP_value[0]);
            GYPValue.add(GYP_value[1]);
            GYPValue.add(GYP_value[2]);
            obj.put("TYPE_GYROSCOPE", new JSONArray(GYPValue));

            ArrayList<Float> PROVaule = new ArrayList<Float>();
            PROVaule.add(PRO_value);
            obj.put("TYPE_PROXIMITY", new JSONArray(PROVaule));

            ArrayList<Float> ORIValue = new ArrayList<Float>();
            ORIValue.add(ORI_value[0]);
            ORIValue.add(ORI_value[1]);
            ORIValue.add(ORI_value[2]);
            obj.put("TYPE_ORIENTATION", new JSONArray(ORIValue));

        }
        catch (JSONException e) {
            e.printStackTrace();
        }


        try {
            //임의의 장소에 저장.
            File savefile = new File(dirPath+"/Sensor"+fileIndex+".txt");
            FileWriter file = new FileWriter(savefile);
            file.write(obj.toString());
            file.flush();
            file.close();

        } catch (IOException e) {
            e.printStackTrace();
        }


        if (folder.listFiles().length > 0 )
            for ( File f : folder.listFiles() ) {
                String str = f.getName();

                // 파일 내용 읽어오기
                String loadPath = dirPath+"/"+str;
                try {
                    FileInputStream fis = new FileInputStream(loadPath);
                    BufferedReader bufferReader = new BufferedReader(new InputStreamReader(fis));

                    String content="", temp="";
                    while( (temp = bufferReader.readLine()) != null ) {
                        content += temp;
                    }
                    Log.d("MeSSAGE : ", content);
                } catch (Exception e) {}
            }

    }


    /*
        Start Bluetooth Communication



    public void onReceive() {

        if(mBlueToothAdapter == null){
            // 만약 블루투스 adapter가 없으면, 블루투스를 지원하지 않는 기기이거나 블루투스 기능을 끈 기기이다.
        }else{
            // 블루투스 adapter가 있으면, 블루투스 adater에서 페어링된 장치 목록을 불러올 수 있다.
            Set pairDevices = mBlueToothAdapter.getBondedDevices();

            //페어링된 장치가 있으면
            if(pairDevices.size()>0){

            }else{
                Toast.makeText(getApplicationContext(), "no Device", Toast.LENGTH_SHORT).show();
            }
        }

        //브로드캐스트리시버를 이용하여 블루투스 장치가 연결이 되고, 끊기는 이벤트를 받아 올 수 있다.
        BroadcastReceiver bluetoothReceiver =  new BroadcastReceiver(){
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                //연결된 장치를 intent를 통하여 가져온다.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                //장치가 연결이 되었으면
                if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                    Log.d("TEST", device.getName().toString() +" Device Is Connected!");
                    sayHelloToDevice(device.getName().toString());
                    //장치의 연결이 끊기면
                }else if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)){
                    Log.d("TEST", device.getName().toString() +" Device Is DISConnected!");

                }
            }
        };

        //MUST unregisterReceiver(bluetoothReceiver) in onDestroy()
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        registerReceiver(bluetoothReceiver, filter);
        filter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(bluetoothReceiver, filter);

    }


    private void sayHelloToDevice(String deviceName) {

        UUID SPP_UUID = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        Set<BluetoothDevice> pairedDevices = mBlueToothAdapter.getBondedDevices();
        BluetoothDevice targetDevice = null;


        for(BluetoothDevice pairedDevice : pairedDevices)
            if(pairedDevice.getName().equals(deviceName)) {
                targetDevice = pairedDevice;
                break;
            }


        // If the device was not found, toast an error and return
        if(targetDevice == null) {
            Log.d("Device not found","BAD");
            return;
        }

        // Create a connection to the device with the SPP UUID
        BluetoothSocket btSocket = null;
        try {
            btSocket = targetDevice.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
        } catch (IOException e) {
            Log.d("Unable to open message","BAD");
            return;
        }

        // Connect to the device
        try {
            btSocket.connect();
        } catch (IOException e) {
            Log.d("Unable to Connect","BAD");
            return;
        }

        try {
            OutputStreamWriter writer = new OutputStreamWriter(btSocket.getOutputStream());
            writer.write("Hello World!\r\n");
            Log.d("SUCCESS","GOODJOB"+writer.toString());
            writer.flush();
        } catch (IOException e) {
            Log.d("Unable to send message","BAD");
        }

        try {
            btSocket.close();
            Toast.makeText(this, "Message successfully sent to the device", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Unable to close the connection to the device", Toast.LENGTH_SHORT).show();
        }
    }
*/

} // end of class
