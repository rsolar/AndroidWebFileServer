package sola.androidwebfileserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.nanohttpd.webserver.MyWebFileServer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int DEFAULT_PORT = 8080;

    // Instance of android web file server
    private MyWebFileServer androidWebFileServer;
    private BroadcastReceiver broadcastReceiverNetworkState;
    private static boolean isStarted = false;

    // Views
    private CoordinatorLayout coordinatorLayout;
    private TextView textViewIpAccess;
    private EditText editTextPort;
    private TextView textViewMessage;
    private FloatingActionButton floatingActionButtonOnOff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init views
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
        textViewIpAccess = (TextView) findViewById(R.id.textViewIpAccess);
        editTextPort = (EditText) findViewById(R.id.editTextPort);
        textViewMessage = (TextView) findViewById(R.id.textViewMessage);
        floatingActionButtonOnOff = (FloatingActionButton) findViewById(R.id.floatingActionButtonOnOff);
        floatingActionButtonOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isConnected()) {
                    if (!isStarted && startAndroidWebFileServer()) {
                        isStarted = true;
                        textViewMessage.setVisibility(View.VISIBLE);
                        floatingActionButtonOnOff.setBackgroundTintList(ContextCompat.getColorStateList(MainActivity.this, R.color.colorGreen));
                        editTextPort.setEnabled(false);
                    } else if (stopAndroidWebFileServer()) {
                        isStarted = false;
                        textViewMessage.setVisibility(View.INVISIBLE);
                        floatingActionButtonOnOff.setBackgroundTintList(ContextCompat.getColorStateList(MainActivity.this, R.color.colorRed));
                        editTextPort.setEnabled(true);
                    }
                } else {
                    Snackbar.make(coordinatorLayout, getString(R.string.network_message), Snackbar.LENGTH_LONG).show();
                }
            }
        });
        setIpAccess();

        // Init broadcast receiver to listen network state changed
        initBroadcastReceiverNetworkStateChanged();
    }

    private boolean startAndroidWebFileServer() {
        if (!isStarted) {
            int port = getPortFromEditTextPort();
            try {
                if (port == 0) {
                    throw new Exception();
                }
                String host = null; // Bind to all interfaces by default
                List<File> rootDirs = new ArrayList<File>();
                rootDirs.add(new File(".").getAbsoluteFile());
                boolean quiet = false;
                String cors = null;
                androidWebFileServer = new MyWebFileServer(host, port, rootDirs, quiet, cors);
                androidWebFileServer.start();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                Snackbar.make(coordinatorLayout, "The Port " + port + " doesn't work, please choose another port between 1000 and 9999.", Snackbar.LENGTH_LONG).show();
            }
        }
        return false;
    }

    private boolean stopAndroidWebFileServer() {
        if (isStarted && androidWebFileServer != null) {
            androidWebFileServer.stop();
            return true;
        }
        return false;
    }

    private void initBroadcastReceiverNetworkStateChanged() {
        final IntentFilter filters = new IntentFilter();
        filters.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filters.addAction("android.net.wifi.STATE_CHANGE");
        filters.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        broadcastReceiverNetworkState = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!isConnected() && isStarted) {
                    isStarted = false;
                    textViewMessage.setVisibility(View.INVISIBLE);
                    floatingActionButtonOnOff.setBackgroundTintList(ContextCompat.getColorStateList(MainActivity.this, R.color.colorRed));
                    editTextPort.setEnabled(true);
                    Snackbar.make(coordinatorLayout, getString(R.string.network_message), Snackbar.LENGTH_LONG).show();
                }
                setIpAccess();
            }
        };
        super.registerReceiver(broadcastReceiverNetworkState, filters);
    }

    private void setIpAccess() {
        textViewIpAccess.setText("http://" + getIpAddress() + ":");
    }

    private String getIpAddress() {
        String formatedIpAddress = Utils.getIPAddress(true);
        if ("".equals(formatedIpAddress)) {
            formatedIpAddress = "0:0:0:0";
        }
        return formatedIpAddress;
    }

    private int getPortFromEditTextPort() {
        String valueEditText = editTextPort.getText().toString();
        return valueEditText.length() > 0 ? Integer.parseInt(valueEditText) : DEFAULT_PORT;
    }

    private boolean isConnected() {
        return !"".equals(Utils.getIPAddress(true));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAndroidWebFileServer();
        isStarted = false;
        if (broadcastReceiverNetworkState != null) {
            unregisterReceiver(broadcastReceiverNetworkState);
        }
    }
}
