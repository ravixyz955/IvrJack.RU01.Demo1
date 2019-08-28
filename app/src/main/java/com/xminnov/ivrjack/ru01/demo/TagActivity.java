package com.xminnov.ivrjack.ru01.demo;

import android.app.TabActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.xminnov.ivrjack.ru01.IvrJackService;

/**
 * Created by ChenRong1 on 2015/9/24 0024.
 */
public class TagActivity extends TabActivity {

    private Tab1 tab1;
    private Tab2 tab2;
    private Tab3 tab3;
    private Tab4 tab4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TabHost tabHost = getTabHost();

        LayoutInflater.from(this).inflate(R.layout.tag, tabHost.getTabContentView(), true);

        tabHost.addTab(tabHost.newTabSpec("tab1").setIndicator("Read").setContent(R.id.tab1));
        tabHost.addTab(tabHost.newTabSpec("tab2").setIndicator("Write").setContent(R.id.tab2));
        tabHost.addTab(tabHost.newTabSpec("tab3").setIndicator("Lock").setContent(R.id.tab3));
        tabHost.addTab(tabHost.newTabSpec("tab4").setIndicator("Kill").setContent(R.id.tab4));

        tab1 = new Tab1();
        tab2 = new Tab2();
        tab3 = new Tab3();
        tab4 = new Tab4();

        tab1.init();
        tab2.init();
        tab3.init();
        tab4.init();

    }

    private void updateUi(boolean editing) {
        tab1.buttonRead.setEnabled(!editing);
        tab2.buttonWrite.setEnabled(!editing);
        tab3.buttonLock.setEnabled(!editing);
        tab4.buttonKill.setEnabled(!editing);
    }

    private int convertPassword(String parm, String str) {
        if (str.length() != 8) {
            throw new NumberFormatException(parm + " must be 8 bytes!");
        }
        try {
            return Integer.parseInt(str, 16);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(parm + " error format!");
        }
    }

    private byte convertByte(String parm, String str) {
        try {
            return (byte) Integer.parseInt(str, 16);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(parm + " error format!");
        }
    }

    private byte[] convertBytes(String parm, String str, int expectLen) {
        str = str.replace(" ", "");
        if (str.length() % 2 != 0) {
            throw new NumberFormatException(parm + " error length!");
        }
        if (expectLen > 0 && str.length() != expectLen) {
            throw new NumberFormatException(parm + " error length!");
        }
        byte[] bytes = new byte[str.length() / 2];
        for (int i=0; i<str.length(); i+=2) {
            try {
                bytes[i/2] = (byte) Integer.parseInt(str.substring(i, i + 2), 16);
            } catch (NumberFormatException e) {
                throw new NumberFormatException(parm + " error format!");
            }
        }
        return bytes;
    }

    class Tab1 {

        private TextView viewStatus;
        private TextView viewEpc;
        private EditText viewPwd;
        private EditText viewAddress;
        private RadioGroup viewBlocks;
        private EditText viewLength;
        private EditText viewData;
        private Button buttonRead;

        public void init() {
            View v = findViewById(R.id.tab1);
            viewStatus = (TextView) v.findViewById(R.id.status);
            viewEpc = (TextView) v.findViewById(R.id.epc);
            viewPwd = (EditText) v.findViewById(R.id.accpwd);
            viewAddress = (EditText) v.findViewById(R.id.address);
            viewBlocks = (RadioGroup) v.findViewById(R.id.blockGroup);
            viewLength = (EditText) v.findViewById(R.id.length);
            viewData = (EditText) v.findViewById(R.id.data);
            buttonRead = (Button) v.findViewById(R.id.read);

            viewStatus.setText("");
            viewEpc.setText(getIntent().getStringExtra("epc"));
            buttonRead.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        ReadTask task = new ReadTask();
                        task.epc = convertBytes("epc", viewEpc.getText().toString(), 0);
                        task.accpwd = convertPassword("accpwd", viewPwd.getText().toString());
                        task.address = convertByte("address", viewAddress.getText().toString());
                        task.block = getBlock();
                        task.length = convertByte("length", viewLength.getText().toString());

                        viewStatus.setText("Reading...");
                        viewData.setText("");
                        updateUi(true);
                        new Thread(task).start();
                    } catch (NumberFormatException e) {
                        Toast.makeText(TagActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        private byte getBlock() {
            switch (viewBlocks.getCheckedRadioButtonId()) {
                case R.id.blockEpc:
                    return 0;
                case R.id.blockUser:
                    return 1;
                case R.id.blockRFU:
                    return 2;
                default:
                    return 3;
            }
        }

        class ReadTask implements Runnable {

            byte[] epc;
            int accpwd;
            byte address;
            byte block;
            byte length;

            @Override
            public void run() {
                DoneTask task = new DoneTask();
                int ret = MainActivity.service.selectTag(accpwd, epc);
                if (ret == 0) {
                    IvrJackService.TagBlock result = new IvrJackService.TagBlock();
                    ret = MainActivity.service.readTag(block, address, length, result);
                    if (ret == 0) {
                        task.data = result.data;
                        task.msg = "Read tag success";
                    } else {
                        task.msg = "Read tag failed: " + ret;
                    }
                } else {
                    task.msg = "Select tag failed: " + ret;
                }
                task.ret = ret;
                new Handler(Looper.getMainLooper()).post(task);
            }
        }

        class DoneTask implements Runnable {

            byte[] data;
            int ret;
            String msg;

            @Override
            public void run() {
                viewStatus.setText(msg);
                if (ret == 0) {
                    StringBuilder builder = new StringBuilder();
                    for (byte b : data) {
                        builder.append(String.format("%02X ", b));
                    }
                    viewData.setText(builder.toString());
                }
                updateUi(false);
            }
        }

    }

    class Tab2 {

        private TextView viewStatus;
        private TextView viewEpc;
        private EditText viewPwd;
        private EditText viewAddress;
        private RadioGroup viewBlocks;
        private EditText viewLength;
        private EditText viewData;
        private Button buttonWrite;

        public void init() {
            View v = findViewById(R.id.tab2);
            viewStatus = (TextView) v.findViewById(R.id.status);
            viewEpc = (TextView) v.findViewById(R.id.epc);
            viewPwd = (EditText) v.findViewById(R.id.accpwd);
            viewAddress = (EditText) v.findViewById(R.id.address);
            viewBlocks = (RadioGroup) v.findViewById(R.id.blockGroup);
            viewLength = (EditText) v.findViewById(R.id.length);
            viewData = (EditText) v.findViewById(R.id.data);
            buttonWrite = (Button) v.findViewById(R.id.write);

            viewStatus.setText("");
            viewEpc.setText(getIntent().getStringExtra("epc"));
            buttonWrite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        WriteTask task = new WriteTask();
                        task.epc = convertBytes("epc", viewEpc.getText().toString(), 0);
                        task.accpwd = convertPassword("accpwd", viewPwd.getText().toString());
                        task.address = convertByte("address", viewAddress.getText().toString());
                        task.block = getBlock();
                        task.length = convertByte("length", viewLength.getText().toString());
                        task.data = convertBytes("data", viewData.getText().toString(), task.length * 4);

                        viewStatus.setText("Writing...");
                        updateUi(true);
                        new Thread(task).start();
                    } catch (NumberFormatException e) {
                        Toast.makeText(TagActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        private byte getBlock() {
            switch (viewBlocks.getCheckedRadioButtonId()) {
                case R.id.blockEpc:
                    return 0;
                case R.id.blockUser:
                    return 1;
                default:
                    return 2;
            }
        }

        class WriteTask implements Runnable {

            byte[] epc;
            int accpwd;
            byte address;
            byte block;
            byte length;
            byte[] data;

            @Override
            public void run() {
                DoneTask task = new DoneTask();
                int ret = MainActivity.service.selectTag(accpwd, epc);
                if (ret == 0) {
                    ret = MainActivity.service.writeTag(block, address, length, data);
                    if (ret == 0) {
                        task.msg = "Write tag success";
                    } else {
                        task.msg = "Write tag failed: " + ret;
                    }
                } else {
                    task.msg = "Select tag failed: " + ret;
                }
                new Handler(Looper.getMainLooper()).post(task);
            }
        }

        class DoneTask implements Runnable {

            String msg;

            @Override
            public void run() {
                viewStatus.setText(msg);
                updateUi(false);
            }
        }

    }

    class Tab3 {

        private TextView viewStatus;
        private TextView viewEpc;
        private EditText viewPwd;
        private RadioGroup viewLockObjects;
        private RadioGroup viewLockActions;
        private Button buttonLock;

        public void init() {
            View v = findViewById(R.id.tab3);
            viewStatus = (TextView) v.findViewById(R.id.status);
            viewEpc = (TextView) v.findViewById(R.id.epc);
            viewPwd = (EditText) v.findViewById(R.id.accpwd);
            viewLockObjects = (RadioGroup) v.findViewById(R.id.lockObjectGroup);
            viewLockActions = (RadioGroup) v.findViewById(R.id.lockActionGroup);
            buttonLock = (Button) v.findViewById(R.id.lock);

            viewStatus.setText("");
            viewEpc.setText(getIntent().getStringExtra("epc"));
            buttonLock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        LockTask task = new LockTask();
                        task.epc = convertBytes("epc", viewEpc.getText().toString(), 0);
                        task.accpwd = convertPassword("accpwd", viewPwd.getText().toString());
                        task.lockObject = getLockObject();
                        task.lockAction = getLockAction();

                        viewStatus.setText("Locking...");
                        updateUi(true);
                        new Thread(task).start();
                    } catch (NumberFormatException e) {
                        Toast.makeText(TagActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        private byte getLockObject() {
            switch (viewLockObjects.getCheckedRadioButtonId()) {
                case R.id.objectEpc:
                    return 0;
                case R.id.objectUser:
                    return 1;
                case R.id.objectAPWD:
                    return 2;
                default:
                    return 3;
            }
        }

        private byte getLockAction() {
            switch (viewLockActions.getCheckedRadioButtonId()) {
                case R.id.actionUnlock:
                    return 0;
                case R.id.actionLock:
                    return 1;
                default:
                    return 2;
            }
        }

        class LockTask implements Runnable {

            byte[] epc;
            int accpwd;
            byte lockObject;
            byte lockAction;

            @Override
            public void run() {
                DoneTask task = new DoneTask();
                int ret = MainActivity.service.selectTag(accpwd, epc);
                if (ret == 0) {
                    ret = MainActivity.service.lockTag(accpwd, lockObject, lockAction);
                    if (ret == 0) {
                        task.msg = "Lock tag success";
                    } else {
                        task.msg = "Lock tag failed: " + ret;
                    }
                } else {
                    task.msg = "Select tag failed: " + ret;
                }
                new Handler(Looper.getMainLooper()).post(task);
            }
        }

        class DoneTask implements Runnable {

            String msg;

            @Override
            public void run() {
                viewStatus.setText(msg);
                updateUi(false);
            }
        }
    }

    class Tab4 {

        private TextView viewStatus;
        private TextView viewEpc;
        private EditText viewAccpwd;
        private EditText viewKillpwd;
        private Button buttonKill;

        public void init() {
            View v = findViewById(R.id.tab4);
            viewStatus = (TextView) v.findViewById(R.id.status);
            viewEpc = (TextView) v.findViewById(R.id.epc);
            viewAccpwd = (EditText) v.findViewById(R.id.accpwd);
            viewKillpwd = (EditText) v.findViewById(R.id.killpwd);
            buttonKill = (Button) v.findViewById(R.id.kill);

            viewStatus.setText("");
            viewEpc.setText(getIntent().getStringExtra("epc"));
            buttonKill.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    KillTask task = new KillTask();
                    task.epc = convertBytes("epc", viewEpc.getText().toString(), 0);
                    task.accpwd = convertPassword("accpwd", viewAccpwd.getText().toString());
                    task.killpwd = convertPassword("killpwd", viewKillpwd.getText().toString());

                    viewStatus.setText("Killing...");
                    updateUi(true);
                    new Thread(task).start();
                }
            });

        }

        class KillTask implements Runnable {

            byte[] epc;
            int accpwd;
            int killpwd;

            @Override
            public void run() {
                DoneTask task = new DoneTask();
                int ret = MainActivity.service.selectTag(accpwd, epc);
                if (ret == 0) {
                    ret = MainActivity.service.killTag(killpwd);
                    if (ret == 0) {
                        task.msg = "Kill tag success";
                    } else {
                        task.msg = "Kill tag failed: " + ret;
                    }
                } else {
                    task.msg = "Select tag failed: " + ret;
                }
                new Handler(Looper.getMainLooper()).post(task);
            }
        }

        class DoneTask implements Runnable {

            String msg;

            @Override
            public void run() {
                viewStatus.setText(msg);
                updateUi(false);
            }
        }
    }

}
