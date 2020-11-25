package com.example.t1.endgame;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    int stcount = 0;
    int plug, status, scale;
    double ratio; // 각자 값
    TextView textView01; // 사용량 표시
    TextView textView02; // 시작값 표시
    TextView textView03; // 결과값 표시
    TextView textView04; // 신호강도 표시
    TextView textView05; // LTE신호강도 표시
    private SignalStrength signalStrength;
    private TelephonyManager telephonyManager;
    private final static String LTE_TAG             = "LTE_Tag";
    private final static String LTE_SIGNAL_STRENGTH = "getLteSignalStrength";
    public int cycle = 0;
    public String value = "";
    String LTEsignal = "";
    BTSend btSend;
    IPSend ipSend;
    FileSend fileSend;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView01 = (TextView) findViewById(R.id.howmany); // 사용량 표시
        textView02 = (TextView) findViewById(R.id.start); // 시작값 표시
        textView03 = (TextView) findViewById(R.id.result); // 결과값 표시
        textView04 = (TextView) findViewById(R.id.signal); // 신호강도 표시
        textView05 = (TextView) findViewById(R.id.LTEsignal); // LTE신호강도 표시
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        final RadioGroup rg = (RadioGroup) findViewById(R.id.cycle);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int rid) {
                if (rid== R.id.one) {
                    cycle = 5000;      // 주기 0.01초 되도록 설정
                }
                else if (rid == R.id.five) {
                    cycle = 10000;     //주기 0.1초
                }
                else if (rid == R.id.ten) {
                    cycle = 60000;     //주기 0.5초
                }
            }
        });

        final RadioGroup rg2 = (RadioGroup) findViewById(R.id.Send);
        rg2.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int rid2) {
                if (rid2 ==R.id.bt) value = "BT";
                else if (rid2 == R.id.wifi) value = "IP";
            }
        });

        btSend = new BTSend(this);
        btSend.initialize();
        fileSend = new FileSend(this ,btSend);
        Button sendbtn = (Button) findViewById(R.id.sendbtn);
        sendbtn.setOnClickListener(new View.OnClickListener() { //누르면
            public void onClick(View v) {  // 시행
//                SendThread st = new SendThread(cycle , btSend); // 샌드 스레드를 선언
//                st.start(); // 시행
            }
        });

        Button btsendbtn = (Button) findViewById(R.id.btsendbtn);
        btsendbtn.setOnClickListener(new View.OnClickListener() { //누르면
            public void onClick(View v) {
                btSend.startThread();
            }
        });

        Button Allsendbtn = (Button) findViewById(R.id.ALLsendbtn);
        Allsendbtn.setOnClickListener(new View.OnClickListener() { //누르면
            public void onClick(View v) {
                Log.i("dsem_log", "c:" +cycle);
                Log.i("dsem_log", "value:" + value);
                fileSend.ReadData();
            }
        });
    }

    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(); // 인텐트 필터 선언
        filter.addAction(Intent.ACTION_BATTERY_CHANGED); // 필터가 읽어들일 액션 = 배터리 변경
        filter.addAction(Intent.ACTION_BATTERY_LOW); // 필터가 읽어들일 액션 = 배터리 부족
        filter.addAction(Intent.ACTION_BATTERY_OKAY); // 필터가 읽어들일 액션 = 배터리 부족 상태 해제
        filter.addAction(Intent.ACTION_POWER_CONNECTED); // 필터가 읽어들일 액션 = 배터리 전원 연결
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED); // 필터가 읽어들일 액션 = 배터리 전원 연결 해제
        registerReceiver(mBRBattery, filter); // 읽어들일 리시버 선언
        IntentFilter rssiFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION); // rssi 신호 읽어들일 인텐트 필터 선언 와이파이
        this.registerReceiver(mBRBattery, rssiFilter); // 읽어들일 리시버 선언

    }

    public void onPause() {
        super.onPause();
        unregisterReceiver(mBRBattery); // 중지시
    }

    BroadcastReceiver mBRBattery = new BroadcastReceiver() { // 브로드캐스트 리시버 선언

        int Count = 0; // 변경 횟수 확인

        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction(); // 인텐트로부터 변화 가져옴
            Count++; // 변화 횟수 증가
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                onBatteryChanged(intent); // 배터리 상태가 변화 했을 경우 실행
            }
            if (action.equals(Intent.ACTION_BATTERY_LOW)) {
                Toast.makeText(context, "배터리 위험 수준", Toast.LENGTH_LONG).show();
            }
            if (action.equals(Intent.ACTION_BATTERY_OKAY)) {
                Toast.makeText(context, "배터리 양호", Toast.LENGTH_LONG).show();
            }
            if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
                Toast.makeText(context, "전원 연결됨", Toast.LENGTH_LONG).show();
            }
            if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
                Toast.makeText(context, "전원 분리됨", Toast.LENGTH_LONG).show();
            }
        }

        public void onBatteryChanged(Intent intent) { // 배터리 상태가 변화 했을 경우 실행

            String sPlug = ""; //
            String sStatus = ""; //
            WifiManager wifiMan = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE); // 와이파이 매니저 선언
            wifiMan.startScan(); // 스캔 시작
            int linkspeed = wifiMan.getConnectionInfo().getLinkSpeed(); // 신호 세기 체크
            int newRssi = wifiMan.getConnectionInfo().getRssi(); // rssi 신호 체크
            int level = wifiMan.calculateSignalLevel(newRssi, 10); // rssi 값에 따른 레벨값 체크
            int percentage = (int) ((level / 10.0) * 100); // 신호 감도 값 rssi 레벨값/*10
            if (intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false) == false) {
                textView01.setText("배터리 없음");
                return;
            }
            telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

            final PhoneStateListener mListener = new PhoneStateListener()
            {
                @Override
                public void onSignalStrengthsChanged(SignalStrength sStrength)
                {

                    signalStrength = sStrength;
                    try  {
                        Method[] methods = android.telephony.SignalStrength.class.getMethods();

                        for (Method mthd : methods) {
                            if (mthd.getName().equals(LTE_SIGNAL_STRENGTH))
                            {
                                int LTEsignalStrength = (Integer) mthd.invoke(signalStrength, new Object[] {});
                                Log.i(LTE_TAG, "signalStrength = " + LTEsignalStrength);
                                LTEsignal = String.valueOf(LTEsignalStrength);
                                textView05.setText("LTE signalStrength : "+LTEsignal);
                                return;
                            }
                        }
                    }
                    catch (Exception e) {
                        Log.e(LTE_TAG, "Exception: " + e.toString());
                    }

                }
            };
            telephonyManager.listen(mListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
            plug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
            scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0); // 배터리 용량 값/100
            ratio = (double)level * 100.0 / scale; // 배터리 퍼센테이지


            switch (plug) { // 배터리 충전 종류 확인
                case BatteryManager.BATTERY_PLUGGED_AC:
                    sPlug = "AC ";
                    break;
                case BatteryManager.BATTERY_PLUGGED_USB:
                    sPlug = "USB ";
                    break;
                default:
                    sPlug = "Inner Battery";
                    break;
            }

            switch (status) { // 배터리 충전 상태 확인
                case BatteryManager.BATTERY_STATUS_CHARGING:
                    sStatus = "충전중";
                    break;
                case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                    sStatus = "충전중 아님";
                    break;
                case BatteryManager.BATTERY_STATUS_DISCHARGING:
                    sStatus = "In use";
                    break;
                case BatteryManager.BATTERY_STATUS_FULL:
                    sStatus = "Charging complete";
                    break;
                default:
                case BatteryManager.BATTERY_STATUS_UNKNOWN:
                    sStatus = "상태를 알 수 없습니다.";
                    break;
            }
            long now = System.currentTimeMillis();
            Date date = new Date(now);
            SimpleDateFormat sdfNow = new SimpleDateFormat("yyyy/MM/dd/HH:mm:ss");
            String formatDate = sdfNow.format(date);
            String stats = String.format("power value :%d", (int)ratio * 33);
            String signal = String.format("wireless signal strength :%d\nWiFi signal sensitivity :%d", linkspeed, percentage);
            textView02.setText(stats);
            textView04.setText(signal);
            String str = String.format("reading recovery :%d\nCharge type: %s\ncondition:%s\nResidual power source:%.1f%%",
                    Count, sPlug, sStatus, ratio);
            textView01.setText(str);
            String logresult = (formatDate+ " " + (int)ratio * 33 + " " + linkspeed + " "+ percentage + " "  + LTEsignal +  " "+ sStatus+ " "+ ratio + " " + value + " " + cycle/1000 + "초");
            writeLog(logresult);


            if (stats.length() != 0 && stcount == 0) {
                ClipData clip = ClipData.newPlainText("text", stats);
                ClipboardManager cm = (ClipboardManager)
                        getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(clip);
                pasteText();
                stcount++;
            }


        }
    };

    void pasteText() {
        ClipboardManager cm = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm.hasPrimaryClip() == false) {
            return;
        }
        if (cm.getPrimaryClipDescription().hasMimeType(
                ClipDescription.MIMETYPE_TEXT_PLAIN) == false) {
            return;
        }

        ClipData clip = cm.getPrimaryClip();
        ClipData.Item item = clip.getItemAt(0);
        textView03.setText("Initial " + item.getText());
    }

    void writeLog(String a ) {
        String mRoot = Environment.getExternalStorageDirectory().getAbsolutePath(); //  mroot 에 루트 경로를 준다
        String mDir = mRoot+"/logdata";
        File file = new File(mDir);
        if( !file.exists() ) {
            file.mkdirs();
            Toast.makeText(this, "logdata created", Toast.LENGTH_SHORT).show();
        }

        File logfile = new File(mDir+"/ALLlogfile.txt");
        try{
            BufferedWriter bfw = new BufferedWriter(new FileWriter(logfile,true));
            bfw.write(a);
            bfw.write("\n");
            bfw.flush();
            bfw.close();
            Toast.makeText(this, "Save Success", Toast.LENGTH_SHORT).show();
        } catch(IOException e){

        }
    }

    public void showPairedDevicesListDialog() {
        Set<BluetoothDevice> devices = btSend.mBluetoothAdapter.getBondedDevices();
        final BluetoothDevice[] pairedDevices = devices.toArray(new BluetoothDevice[0]);

        if (pairedDevices.length == 0) {
            showQuitDialog("No devices have been paired.\n"
                    + "You must pair it with another device.");
            return;
        }

        String[] items;
        items = new String[pairedDevices.length];
        for (int i = 0; i < pairedDevices.length; i++) {
            items[i] = pairedDevices[i].getName();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select device");
        builder.setCancelable(true);

        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                btSend.connectTaskExecute(pairedDevices[which]);
                //ConnectTask task = new ConnectTask(pairedDevices[which]);
                //task.execute();

            }
        });
        builder.create().show();
    }

    public void showErrorDialog(String message ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quit");
        builder.setCancelable(false);
        builder.setMessage(message);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

//                if (isConnectionError) {
//                    isConnectionError = false;
                finish();
//                }
            }
        });
        builder.create().show();
    }

    public void showQuitDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quit");
        builder.setCancelable(false);
        builder.setMessage(message);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        builder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == BTSend.REQUEST_BLUETOOTH_ENABLE) {
            if (resultCode == RESULT_OK) {
                //BlueTooth is now Enabled
                showPairedDevicesListDialog();
            }
            if (resultCode == RESULT_CANCELED) {
                showQuitDialog("You need to enable bluetooth");
            }
        }
    }

}
