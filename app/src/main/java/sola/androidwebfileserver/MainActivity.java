package sola.androidwebfileserver;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.nanohttpd.webserver.SimpleWebServer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int DEFAULT_PORT = 8080;

    // INSTANCE OF ANDROID WEB FILE SERVER
    private SimpleWebServer androidWebFileServer;
    private BroadcastReceiver broadcastReceiverNetworkState;
    private static boolean isStarted = false;

    // VIEWS
    private CoordinatorLayout coordinatorLayout;
    private TextView textViewIpAccess;
    private EditText editTextPort;
    private TextView textViewMessage;
    private FloatingActionButton floatingActionButtonOnOff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // INIT VIEWS
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
        textViewIpAccess = (TextView) findViewById(R.id.textViewIpAccess);
        setIpAccess();
        editTextPort = (EditText) findViewById(R.id.editTextPort);
        textViewMessage = (TextView) findViewById(R.id.textViewMessage);
        floatingActionButtonOnOff = (FloatingActionButton) findViewById(R.id.floatingActionButtonOnOff);
        floatingActionButtonOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isConnected()) {
                    if (!isStarted && startAndroidWebFileServer()) {
                        isStarted = true;
                        textViewMessage.setVisibility(View.VISIBLE);
                        floatingActionButtonOnOff.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorGreen)));
                        editTextPort.setEnabled(false);
                    } else if (stopAndroidWebFileServer()) {
                        isStarted = false;
                        textViewMessage.setVisibility(View.INVISIBLE);
                        floatingActionButtonOnOff.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorRed)));
                        editTextPort.setEnabled(true);
                    }
                } else {
                    Snackbar.make(coordinatorLayout, getString(R.string.network_message), Snackbar.LENGTH_LONG).show();
                }
            }
        });

        // INIT BROADCAST RECEIVER TO LISTEN NETWORK STATE CHANGED
        initBroadcastReceiverNetworkStateChanged();
    }

    private boolean startAndroidWebFileServer() {
        if (!isStarted) {
            int port = getPortFromEditTextPort();
            try {
                if (port == 0) {
                    throw new Exception();
                }
                String host = null; // bind to all interfaces by default
                List<File> rootDirs = new ArrayList<File>();
                rootDirs.add(new File(".").getAbsoluteFile());
                boolean quiet = false;
                String cors = null;
                androidWebFileServer = new SimpleWebServer(host, port, rootDirs, quiet, cors);
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

    private void setIpAccess() {
        textViewIpAccess.setText(getIpAccess());
    }

    private void initBroadcastReceiverNetworkStateChanged() {
        final IntentFilter filters = new IntentFilter();
        filters.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filters.addAction("android.net.wifi.STATE_CHANGE");
        filters.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        broadcastReceiverNetworkState = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setIpAccess();
            }
        };
        super.registerReceiver(broadcastReceiverNetworkState, filters);
    }

    private String getIpAccess() {
        String formatedIpAddress = Utils.getIPAddress(true);
        if ("".equals(formatedIpAddress)) {
            formatedIpAddress = "0:0:0:0";
        }
        return "http://" + formatedIpAddress + ":";
    }

    private int getPortFromEditTextPort() {
        String valueEditText = editTextPort.getText().toString();
        return valueEditText.length() > 0 ? Integer.parseInt(valueEditText) : DEFAULT_PORT;
    }

    public boolean isConnected() {
        return "".equals(Utils.getIPAddress(true));
    }

    public boolean onKeyDown(int keyCode, KeyEvent evt) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isStarted) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.warning)
                        .setMessage(R.string.dialog_exit_message)
                        .setPositiveButton(getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finish();
                            }
                        })
                        .setNegativeButton(getResources().getString(android.R.string.cancel), null)
                        .show();
            } else {
                finish();
            }
            return true;
        }
        return false;
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
