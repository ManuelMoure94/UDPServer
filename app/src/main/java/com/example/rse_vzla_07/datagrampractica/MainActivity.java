package com.example.rse_vzla_07.datagrampractica;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Date;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {

    EditText editTextAddress, editTextPort, editTextMsg;
    Button buttonConnect;
    TextView textViewState, textViewRx, infoIp, infoPort, textViewStateReceived, textViewPrompt;

    static final int UdpServerPORT = 4445;

    UdpClientHandler udpClientHandler;
    UdpClientThread udpClientThread;
    UdpServerThread udpServerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        infoIp = findViewById(R.id.infoIp);
        infoPort = findViewById(R.id.infoPort);
        editTextAddress = findViewById(R.id.address);
        editTextPort = findViewById(R.id.port);
        editTextMsg = findViewById(R.id.msg);
        buttonConnect = findViewById(R.id.connect);
        textViewState = findViewById(R.id.state);
        textViewRx = findViewById(R.id.received);
        textViewStateReceived = findViewById(R.id.stateReceived);
        textViewPrompt = findViewById(R.id.prompt);

        infoIp.setText(getIpAddress());
        infoPort.setText(String.valueOf(UdpServerPORT));

        buttonConnect.setOnClickListener(buttonConnectOnClickListener);

        udpClientHandler = new UdpClientHandler(this);
    }

    View.OnClickListener buttonConnectOnClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    udpClientThread = new UdpClientThread(
                            editTextAddress.getText().toString(),
                            Integer.parseInt(editTextPort.getText().toString()),
                            udpClientHandler);

                    udpClientThread.start();

                    buttonConnect.setEnabled(false);
                }
            };

    private void updateState(String state) {
        textViewState.setText(state);
    }

    private void updateRxMsg(String rxMsg) {
        textViewRx.append(rxMsg + "\n");
    }

    private void clientEnd() {
        udpClientThread = null;
        textViewState.setText("clientEnd()");
        buttonConnect.setEnabled(true);
    }

    private String getMessage() {

        if (TextUtils.isEmpty(editTextMsg.getText().toString())) {
            return "Hola, Te envio este mensaje de saludo";
        } else {
            return editTextMsg.getText().toString();
        }
    }

    @Override
    protected void onStart() {
        udpServerThread = new UdpServerThread(UdpServerPORT);
        udpServerThread.start();
        super.onStart();
    }

    @Override
    protected void onStop() {

        if (udpServerThread != null) {
            udpServerThread.setRunning(false);
            udpServerThread = null;
        }
        super.onStop();
    }

    private void updateStateReceived(final String state) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewStateReceived.setText(state);
            }
        });
    }

    private void updatePrompt(final String prompt) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewPrompt.append(prompt);
            }
        });
    }

    public static class UdpClientHandler extends Handler {

        public static final int UPDATE_STATE = 0;
        public static final int UPDATE_MSG = 1;
        public static final int UPDATE_END = 2;
        private MainActivity parent;

        public UdpClientHandler(MainActivity parent) {
            super();
            this.parent = parent;
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {

                case UPDATE_STATE:
                    parent.updateState((String) msg.obj);
                    break;

                case UPDATE_MSG:
                    parent.updateRxMsg((String) msg.obj);
                    break;

                case UPDATE_END:
                    parent.clientEnd();
                    break;

                default:
                    super.handleMessage(msg);
            }
        }

        public String getMessage() {
            return parent.getMessage();
        }
    }

    private class UdpServerThread extends Thread {

        int serverPort;
        DatagramSocket socket;

        boolean running;

        public UdpServerThread(int serverPort) {
            super();
            this.serverPort = serverPort;
        }

        public void setRunning(boolean running) {
            this.running = running;
        }

        @Override
        public void run() {

            running = true;

            try {
                updateStateReceived("Starting UDP Server");
                socket = new DatagramSocket(serverPort);

                updateStateReceived("UDP Server is running");

                while (running) {
                    byte[] buf = new byte[1024];

                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);

                    InetAddress address = packet.getAddress();
                    int port = packet.getPort();

                    String msg = new String(buf, 0, packet.getLength());
                    updatePrompt(msg + "\n");

                    String dString = new Date().toString() + "\n" + "Your Address "
                            + address.toString() + ":" + String.valueOf(port);
                    buf = dString.getBytes();
                    packet = new DatagramPacket(buf, buf.length, address, port);
                    socket.send(packet);
                }
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    socket.close();
                }
            }
        }
    }

    private String getIpAddress() {

        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterface = NetworkInterface
                    .getNetworkInterfaces();

            while (enumNetworkInterface.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterface.nextElement();
                Enumeration<InetAddress> enumeration = networkInterface.getInetAddresses();

                while (enumeration.hasMoreElements()) {
                    InetAddress inetAddress = enumeration.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += "SiteLocalAddress: "
                                + inetAddress.getHostAddress() + "\n";
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }
        return ip;
    }
}
