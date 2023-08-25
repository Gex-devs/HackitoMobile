package gex.home.hackito;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter BA;
    private Handler handler;
    private StringBuffer mOutStringBuffer;
    private StringBuffer mInStringBuffer;

    private BluetoothChatService bcs;
    Button BluetoothConnectBtn, ShellUpBtn;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        setupButtonViews();


        mOutStringBuffer = new StringBuffer("");
        mInStringBuffer = new StringBuffer("");



        handler = new Handler(){
            @SuppressLint("HandlerLeak")
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case Constants.MESSAGE_STATE_CHANGE:
                        switch (msg.arg1) {
                            case BluetoothChatService.STATE_CONNECTED:
                                Log.d("status","connected");
                                // send the protocol version to the server
                                send("3," + Constants.PROTOCOL_VERSION + "," + Constants.CLIENT_NAME + "\n");
                                break;
                            case BluetoothChatService.STATE_CONNECTING:
                                Log.d("status","connecting");
                                break;
                            case BluetoothChatService.STATE_LISTEN:
                            case BluetoothChatService.STATE_NONE:
                                Log.d("status","not connected");
                                disconnect();
                                break;
                        }
                        break;
                    case Constants.MESSAGE_WRITE:
                        byte[] writeBuf = (byte[]) msg.obj;
                        // construct a string from the buffer
                        String writeMessage = new String(writeBuf);
                        break;
                    case Constants.MESSAGE_READ:
                        byte[] readBuf = (byte[]) msg.obj;
                        // construct a string from the valid bytes in the buffer
                        String readData = new String(readBuf, 0, msg.arg1);
                        // message received
                        parseData(readData);
                        break;
                    case Constants.MESSAGE_DEVICE_NAME:
                        // save the connected device's name
                        if (null != this) {
                            Toast.makeText(getApplicationContext(), "Connected to "
                                    + "raspi", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case Constants.MESSAGE_TOAST:
                        if (null != this) {
                            Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),
                                    Toast.LENGTH_SHORT).show();
                        }
                        break;
                }

            }
        };





    }

    private void setupButtonViews() {

        // Assign Views
        BluetoothConnectBtn = findViewById(R.id.blu_connect);
        ShellUpBtn = findViewById(R.id.Shell_Connect);


        //setup listeners
        BluetoothConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bcs = new BluetoothChatService(MainActivity.this,handler);
                String raspberryPiMacAddress = "B8:27:EB:2F:30:FC"; // Replace with actual MAC address
                BA = BluetoothAdapter.getDefaultAdapter();
                BluetoothDevice raspberryPiDevice = BA.getRemoteDevice(raspberryPiMacAddress);
                bcs.connect(raspberryPiDevice,1,true);

                if (bcs.getState() != BluetoothChatService.STATE_CONNECTED) {
                    Toast.makeText(MainActivity.this, "cant send message - not connected", Toast.LENGTH_SHORT).show();
                }
            }
        });

        ShellUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = "1,0,0,0.5,0.5\n";
                bcs.write(message.getBytes());

                // Reset out string buffer to zero and clear the edit text field
                mOutStringBuffer.setLength(0);
            }
        });

    }

    public void send(String message) {
        // Check that we're actually connected before trying anything
        if (bcs.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, "cant send message - not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            bcs.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
        }
    }
    private void parseData(String data) {
        //msg(data);

        // add the message to the buffer
        mInStringBuffer.append(data);

        // debug - log data and buffer
        //Log.d("data", data);
        //Log.d("mInStringBuffer", mInStringBuffer.toString());
        //msg(data.toString());

        // find any complete messages
        String[] messages = mInStringBuffer.toString().split("\\n");
        int noOfMessages = messages.length;
        // does the last message end in a \n, if not its incomplete and should be ignored
        if (!mInStringBuffer.toString().endsWith("\n")) {
            noOfMessages = noOfMessages - 1;
        }

        // clean the data buffer of any processed messages
        if (mInStringBuffer.lastIndexOf("\n") > -1)
            mInStringBuffer.delete(0, mInStringBuffer.lastIndexOf("\n") + 1);

        // process messages
        for (int messageNo = 0; messageNo < noOfMessages; messageNo++) {
            processMessage(messages[messageNo]);
        }

    }
    private void processMessage(String message) {
        // Debug
        // msg(message);
        String parameters[] = message.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
        boolean invalid = false;

        // Check the message
        if (parameters.length > 0) {
            switch (parameters[0]) {
                case "4":
                case "5":
                    System.out.println("Ignored, Warning");
                    break;
                default:
                    invalid = true;
            }
        }

        if (invalid) {
            System.out.println("Invalid Message");
        }
    }
    private void disconnect() {
        if (bcs != null) {
            bcs.stop();
        };

        finish();
    }
}