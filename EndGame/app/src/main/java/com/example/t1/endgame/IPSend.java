package com.example.t1.endgame;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;


public class IPSend {

    private MainActivity ma;
    private FileSend fs;
    public Socket socket;
    public String ip;
    public int port;
    public static boolean Enable = false;
    public int cycle;
    public String value = "";
    public String mDir;

    private PrintWriter out;

    public IPSend() {
        socket = null;
        this.ip = "203.234.62.144";
        this.port = 30001;
        this.mDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/lifelog";
        this.cycle = 0;
        this.value = "";

    }

    public IPSend(MainActivity mainActivity, FileSend fileSend) {
        this.ma = mainActivity;
        this.fs = fileSend;
        socket = null;
        this.ip = "203.234.62.144";
        this.port = 30001;
        this.mDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/lifelog";
        this.cycle = 0;
        this.value = "";

    }
    public void connect() {
        try {
            Log.i("dsem_log", "ip" + ip);
            Log.i("dsem_log", "port" + port);
            socket = new Socket(ip, port);
            this.out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "MS949")), true); // 문자 인코딩 부문 ms949를 사용한다
        } catch (IOException e) {
            Log.e("dsem_log", e.getMessage());
        }
    }
    public void disconnect() {
        try {
            this.out.println("[socket_close]");
        } catch (Exception e) {
            Log.e("dsem_log", e.getMessage());
        }
    }
    public void sendMessage(String msg) {
        try {
            this.out.println(msg);
            this.out.flush();
        }  catch (Exception e) {
            Log.e("dsem_log", e.getMessage());
        }
    }
}
