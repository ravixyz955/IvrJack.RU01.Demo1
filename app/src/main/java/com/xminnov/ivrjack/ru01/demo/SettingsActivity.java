package com.xminnov.ivrjack.ru01.demo;

import android.app.TabActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.xminnov.ivrjack.ru01.IvrJackService;

/**
 * Created by ChenRong1 on 2015/9/24 0024.
 */
public class SettingsActivity extends TabActivity {

    private Tab1 tab1;
    private Tab2 tab2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TabHost tabHost = getTabHost();

        LayoutInflater.from(this).inflate(R.layout.settings, tabHost.getTabContentView(), true);

        tabHost.addTab(tabHost.newTabSpec("tab1").setIndicator("Battery & Buzzer").setContent(R.id.tab1));
        tabHost.addTab(tabHost.newTabSpec("tab2").setIndicator("EPC + TID").setContent(R.id.tab2));

        tab1 = new Tab1();
        tab2 = new Tab2();

        tab1.init();
        tab2.init();
    }

    private void updateUi(Object source, boolean editing) {
        tab1.buttonRead.setEnabled(!editing);
        tab1.buttonBeep.setEnabled(!editing);
        tab2.buttonRead.setEnabled(!editing);
    }

    class Tab1 {

        private TextView viewStatus;
        private TextView viewBattery;
        private ToggleButton buttonBeep;
        private Button buttonRead;

        public void init() {
            View layout = findViewById(R.id.tab1);
            viewStatus = (TextView) layout.findViewById(R.id.status);
            viewBattery = (TextView) layout.findViewById(R.id.battery);
            buttonBeep = (ToggleButton) layout.findViewById(R.id.beep);
            buttonRead = (Button) layout.findViewById(R.id.read);

            viewStatus.setText("");
            viewBattery.setText("");
            buttonRead.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    viewStatus.setText("Reading...");
                    updateUi(this, true);
                    new Thread(new ReadTask()).start();
                }
            });
            buttonBeep.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    viewStatus.setText("Setting...");
                    updateUi(this, true);
                    new Thread(new BeepTask()).start();
                }
            });
        }

        class ReadTask implements Runnable {

            @Override
            public void run() {
                final IvrJackService.BatteryBuzzerStatus status = new IvrJackService.BatteryBuzzerStatus();
                final int ret = MainActivity.service.getBatteryBuzzerStatus(status);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (ret == 0) {
                            viewStatus.setText("Success");
                            viewBattery.setText(String.format("%02d%%", status.battery));
                            buttonBeep.setChecked(status.buzzer == 0x01);
                        } else {
                            viewStatus.setText("Failed: " + ret);
                        }
                        updateUi(Tab1.this, false);
                    }
                });
            }
        }

        class BeepTask implements Runnable {

            @Override
            public void run() {
                final int ret = MainActivity.service.setBuzzerStatus((byte) (buttonBeep.isChecked() ? 1 : 0));
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (ret == 0) {
                            viewStatus.setText("Success");
                        } else {
                            viewStatus.setText("Failed: " + ret);
                            buttonBeep.setChecked(!buttonBeep.isChecked());
                        }
                        updateUi(Tab1.this, false);
                    }
                });
            }
        }

    }

    class Tab2 {

        private TextView viewStatus;
        private EditText viewPassword;
        private TextView viewEpc;
        private TextView viewTid;
        private Button buttonRead;

        public void init() {
            View layout = findViewById(R.id.tab2);
            viewStatus = (TextView) layout.findViewById(R.id.status);
            viewPassword = (EditText) layout.findViewById(R.id.accpwd);
            viewEpc = (TextView) layout.findViewById(R.id.epc);
            viewTid = (TextView) layout.findViewById(R.id.tid);
            buttonRead = (Button) layout.findViewById(R.id.read);

            viewStatus.setText("");
            viewEpc.setText("");
            viewTid.setText("");
            buttonRead.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        ReadTask task = new ReadTask();
                        task.password = getAccpwd();
                        viewStatus.setText("Running...");
                        viewEpc.setText("");
                        viewTid.setText("");
                        updateUi(Tab2.this, true);
                        new Thread(task).start();
                    } catch (NumberFormatException e) {
                        Toast.makeText(SettingsActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        public int getAccpwd() {
            String str = viewPassword.getText().toString();
            if (str.length() != 8) {
                throw new NumberFormatException("Acc Pwd must be 4 bytes!");
            }
            for (char c : str.toLowerCase().toCharArray()) {
                if ((c < '0' || c > '9') && (c < 'a' || c > 'z')) {
                    throw new NumberFormatException("Acc Pwd format is wrong!");
                }
            }
            return Integer.parseInt(str, 16);
        }

        class ReadTask implements Runnable {

            int password;

            @Override
            public void run() {
                final IvrJackService.TagEpcAndTid result = new IvrJackService.TagEpcAndTid();
                final int ret = MainActivity.service.readEpcAndTid(password, result);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (ret == 0) {
                            viewStatus.setText("Success");

                            StringBuilder builder = new StringBuilder();
                            for (int i = 0; i < result.epc.length; i++) {
                                builder.append(String.format("%02X", result.epc[i]));
                                if ((i + 1) % 4 == 0) builder.append(" ");
                            }
                            viewEpc.setText(builder.toString());

                            builder.setLength(0);
                            for (int i = 0; i < result.tid.length; i++) {
                                builder.append(String.format("%02X", result.tid[i]));
                            }
                            viewTid.setText(builder.toString());

                        } else {
                            viewStatus.setText("Failed: " + ret);
                        }
                        updateUi(Tab2.this, false);
                    }
                });
            }
        }
    }
}