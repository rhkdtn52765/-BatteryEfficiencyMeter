package com.example.t1.endgame;


import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class FileSend {

    private MainActivity ma;
    public static Socket socket;
    public String ip;
    public int port;
    public static boolean Enable = false;
    public int cycle;
    public String value;
    public String mDir;
    BTSend btSend;
    IPSend ipSend;
    public boolean Sending = true;
    private CountDownTimer cmt;

    public FileSend() {

    }

    public FileSend(MainActivity mainActivity, BTSend btsend) {
        this.ma = mainActivity;
        this.ip = "203.234.62.144";
        this.port = 30001;
        this.mDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/lifelog";
        this.cycle = 0;
        this.value = "";
        this.btSend = btsend;
        this.cmt = null;
    }

    public void ReadData() {
        ipSend = new IPSend();
        cycle = ma.cycle;
        value = ma.value;
        if(cmt != null) {
            cmt.cancel();
        }
        cmt= new CountDownTimer(30000000, cycle) {
            @Override
            public void onTick(long millisUntilFinished) {// 총 시간과 주기
                FileSendThread fst = new FileSendThread();
                fst.start();
            }

            @Override
            public void onFinish() {

            }
        }.start();  // 타이머 시작

    }

    class FileSendThread extends Thread {
        public void run() {
            Looper.prepare();
            String str = (mDir);
            File directory = new File(str);
            File[] files = directory.listFiles();
            List<String> filesNameList = new ArrayList<>();
            Log.i("dsem_log", "길이" + files.length);
            for (int i = 0; i < files.length; i++) {
                filesNameList.add(files[i].getName());
            }
            ipSend.connect();
            try {
                for (int j = 0; j < filesNameList.size(); j++) {
                    File file = new File(mDir + "/" + filesNameList.get(j).toString());
                    Scanner scan = new Scanner(file);
                    ipSend.sendMessage("[file_name]");
                    ipSend.sendMessage("logfile.txt");
                    while (scan.hasNextLine()) {

                        String msg = scan.nextLine();
                        if (ma.value.equals("IP")) {
                            ipSend.sendMessage(msg);
                        } else if (ma.value.equals("BT")) {
                            btSend.sendMessage(msg);
                        }
                    }
                    ipSend.sendMessage("[file_close]");
                }
                ipSend.disconnect();
            } catch (Exception e) {
                Log.e("dsem_log", e.getMessage());
                Log.i("dsem_log", "err1");
            }
            Looper.loop();
        }
    }

}

