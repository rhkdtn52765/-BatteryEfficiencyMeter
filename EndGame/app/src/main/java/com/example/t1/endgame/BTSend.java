package com.example.t1.endgame;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class BTSend {
    private MainActivity ma;
    public static final int REQUEST_BLUETOOTH_ENABLE = 100;
    private TextView mConnectionStatus;
    private EditText mInputEditText;
    ConnectedTask mConnectedTask = null;
    public BluetoothAdapter mBluetoothAdapter;
    private String mConnectedDeviceName = null;
    static boolean isConnectionError = false;
    private static final String TAG = "BluetoothClient";
    public BTSendThread btsendThread;

    public BTSend() {
    }

    public BTSend(MainActivity mainActivity) {
        this.ma = mainActivity;

    }

    public boolean initialize() {

        this.mConnectionStatus = (TextView) ma.findViewById(R.id.connection_status_textview);
        this.mInputEditText = (EditText) ma.findViewById(R.id.input_string_edittext);
        Log.d(TAG, "Initalizing Bluetooth adapter...");
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            ma.showErrorDialog("This device is not implement Bluetooth.");
            return false;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            ma.startActivityForResult(intent, REQUEST_BLUETOOTH_ENABLE);

        } else {
            Log.d(TAG, "Initialisation successful.");
            ma.showPairedDevicesListDialog();

        }
        return true;
    }

    public void startThread() {
        btsendThread = new BTSendThread();
        btsendThread.start();
    }

    public void connectTaskExecute(BluetoothDevice pairedDevice) {
        ConnectTask task = new ConnectTask(pairedDevice);
        task.execute();
    }

    private class ConnectTask extends AsyncTask<Void, Void, Boolean> {

        private BluetoothSocket mBluetoothSocket = null;
        private BluetoothDevice mBluetoothDevice = null;

        ConnectTask(BluetoothDevice bluetoothDevice) {
            mBluetoothDevice = bluetoothDevice;
            mConnectedDeviceName = bluetoothDevice.getName();

            //SPP
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

            try {
                mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                Log.d(TAG, "create socket for " + mConnectedDeviceName);

            } catch (IOException e) {
                Log.e(TAG, "socket create failed " + e.getMessage());
            }

            mConnectionStatus.setText("connecting...");
        }


        @Override
        protected Boolean doInBackground(Void... params) {

            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mBluetoothSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mBluetoothSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " +
                            " socket during connection failure", e2);
                }

                return false;
            }

            return true;
        }


        @Override
        protected void onPostExecute(Boolean isSucess) {

            if (isSucess) {
                connected(mBluetoothSocket);
            } else {

                isConnectionError = true;
                Log.d(TAG, "Unable to connect device");
                ma.showErrorDialog("Unable to connect device");
                isConnectionError = false;
            }
        }
    }

    public void connected(BluetoothSocket socket) {
        mConnectedTask = new ConnectedTask(socket);
        mConnectedTask.execute();
    }


    public class ConnectedTask extends AsyncTask<Void, String, Boolean> {

        private InputStream mInputStream = null;
        private OutputStream mOutputStream = null;
        private BluetoothSocket mBluetoothSocket = null;

        ConnectedTask(BluetoothSocket socket) {

            mBluetoothSocket = socket;
            try {
                mInputStream = mBluetoothSocket.getInputStream();
                mOutputStream = mBluetoothSocket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "socket not created", e);
            }

            Log.d(TAG, "connected to " + mConnectedDeviceName);
            mConnectionStatus.setText("connected to " + mConnectedDeviceName);
        }


        @Override
        protected Boolean doInBackground(Void... params) {

            byte[] readBuffer = new byte[1024];
            int readBufferPosition = 0;


            while (true) {

                if (isCancelled()) return false;

                try {

                    int bytesAvailable = mInputStream.available();

                    if (bytesAvailable > 0) {

                        byte[] packetBytes = new byte[bytesAvailable];

                        mInputStream.read(packetBytes);

                        for (int i = 0; i < bytesAvailable; i++) {

                            byte b = packetBytes[i];
                            if (b == '\n') {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0,
                                        encodedBytes.length);
                                String recvMessage = new String(encodedBytes, "UTF-8");

                                readBufferPosition = 0;

                                Log.d(TAG, "recv message: " + recvMessage);
                                publishProgress(recvMessage);
                            } else {
                                readBuffer[readBufferPosition++] = b;
                            }
                        }
                    }
                } catch (IOException e) {

                    Log.e(TAG, "disconnected", e);
                    return false;
                }
            }

        }


        @Override
        protected void onPostExecute(Boolean isSucess) {
            super.onPostExecute(isSucess);

            if (!isSucess) {


                closeSocket();
                Log.d(TAG, "Device connection was lost");
                isConnectionError = true;
                ma.showErrorDialog("Device connection was lost");
                isConnectionError = false;
            }
        }

        @Override
        protected void onCancelled(Boolean aBoolean) {
            super.onCancelled(aBoolean);

            closeSocket();
        }

        void closeSocket() {

            try {

                mBluetoothSocket.close();
                Log.d(TAG, "close socket()");

            } catch (IOException e2) {

                Log.e(TAG, "unable to close() " +
                        " socket during connection failure", e2);
            }
        }

        void write(String msg) {

            msg += "\n";

            try {
                mOutputStream.write(msg.getBytes());
                mOutputStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "Exception during send", e);
            }

            mInputEditText.setText("senser data"); // 글자 입력창 비워주려고 사용
        }
    }

    void sendMessage(String msg) {
        Log.i("dsem_log", "샌드메세지 실행");
        Log.i("dsem_log", "mConnectedTask"+mConnectedTask);
        Log.i("dsem_log", "mConnectedTask"+msg);
        if (mConnectedTask != null) {
            mConnectedTask.write(msg);
            Log.d(TAG, "send message: " + msg);
        }
    }

    class BTSendThread extends Thread  {
        public void run() {
            Looper.prepare();
            String mRoot = Environment.getExternalStorageDirectory().getAbsolutePath(); //  mroot 에 루트 경로를 준다
            String str = (mRoot + "/lifelog");
            File directory = new File(str);
            File[] files = directory.listFiles();
            List<String> filesNameList = new ArrayList<>();
            Log.i("dsem_log", "길이" + files.length);
            for (int i = 0; i < files.length; i++) {
                filesNameList.add(files[i].getName());
            }
            Log.i("dsem_log", "filenameload");
            try {
                Log.i("dsem_log", "try 진입");
                for (int j = 0; j < filesNameList.size(); j++) {

                    File file = new File(mRoot + "/lifelog/" + filesNameList.get(j).toString());
                    Scanner scan = new Scanner(file);

                    while (scan.hasNextLine()) {
                        String sendMessage = scan.nextLine();
                        sendMessage(sendMessage);
                    }
                }
            } catch (Exception e) {
                Log.e("dsem_log", e.getMessage());
            }
            Looper.loop();
        }
    }

}
