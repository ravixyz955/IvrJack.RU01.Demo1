package com.xminnov.ivrjack.ru01.demo;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.xminnov.ivrjack.ru01.IvrJackAdapter;
import com.xminnov.ivrjack.ru01.IvrJackService;
import com.xminnov.ivrjack.ru01.IvrJackStatus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends ListActivity implements IvrJackAdapter {

    private static final String TAG = "ru01";

    public static IvrJackService service;

    private TextView viewStatus;
    private TextView viewTime;
    private ImageView viewProgress;
    private View layoutReader;
    private View viewPlugout;
    private Button buttonStart;
    private Button buttonClear;
    private ImageButton buttonSetting;
    private ProgressBar viewVolume;

    private EpcAdapter adapter;
    private MainHandler handler;

    private BroadcastReceiver volumeBroadcast;

    @Override
    public void onConnect(String deviceSn) {
        Log.i(TAG, "on connect");
        viewPlugout.setVisibility(View.GONE);
        layoutReader.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDisconnect() {
        Log.i(TAG, "on disconnect");
        layoutReader.setVisibility(View.GONE);
        viewPlugout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStatusChange(IvrJackStatus status) {
        Log.i(TAG, "on status change: " + status);
        switch (status) {
            case ijsDetecting:
                viewStatus.setText("Device detecting...");
                startAnimation();
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                viewVolume.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
                viewVolume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
                if (viewVolume.getMax() != viewVolume.getProgress()) {
                    new AlertDialog.Builder(this)
                            .setTitle("Please set the volume to maximum!")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).show();

                }
                break;
            case ijsRecognized:
                viewStatus.setText("Device connected.");
                stopAnimation();
                break;
            case ijsUnRecognized:
                viewStatus.setText("Device unrecognized.");
                stopAnimation();
                break;
            case ijsPlugout:
                viewStatus.setText("Device disconnected.");
                stopAnimation();
                break;
        }
    }

    @Override
    public void onInventory(byte[] epc) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < epc.length; i++) {
            builder.append(String.format("%02X", epc[i]));
            if ((i + 1) % 4 == 0) builder.append(" ");
        }
        adapter.addEpcRecord(builder.toString());
    }

    public void onStart(View v) {
        if (buttonStart.getText().toString().equals(">>>Start<<<")) {
            startAnimation();
            buttonStart.setEnabled(false);
            buttonSetting.setEnabled(false);
            new Thread(new StartTask()).start();
        } else {
            startAnimation();
            buttonStart.setEnabled(false);
            new Thread(new StopTask()).start();
        }
    }

    public void onClear(View v) {
        adapter.clearEpcRecords();
    }

    public void onSetting(View v) {
        if (buttonStart.getText().toString().equals(">>>Start<<<")) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewStatus = (TextView) findViewById(R.id.status);
        viewTime = (TextView) findViewById(R.id.time);
        viewProgress = (ImageView) findViewById(R.id.progress);
        layoutReader = findViewById(R.id.reader);
        viewPlugout = findViewById(R.id.plugout);
        viewVolume = (ProgressBar) findViewById(R.id.volume);

        buttonStart = (Button) findViewById(R.id.start);
        buttonClear = (Button) findViewById(R.id.clear);
        buttonSetting = (ImageButton) findViewById(R.id.settings);

        viewTime.setText(new SimpleDateFormat("yyyy-MM-dd E", Locale.US).format(new Date()));

        adapter = new EpcAdapter(this);
        setListAdapter(adapter);

        handler = new MainHandler();

        service = new IvrJackService(this, this);
        service.open();

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        viewVolume.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        viewVolume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        volumeBroadcast = new VolumnBroadcast();
        registerReceiver(volumeBroadcast, new IntentFilter("android.media.VOLUME_CHANGED_ACTION"));

    }

    @Override
    protected void onDestroy() {
        service.close();
        service = null;
        unregisterReceiver(volumeBroadcast);
        super.onDestroy();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if (buttonStart.getText().toString().equals(">>>Start<<<")) {
            EpcRecord epc = (EpcRecord) adapter.getItem(position);
            Intent intent = new Intent(this, TagActivity.class);
            intent.putExtra("epc", epc.epc);
            startActivity(intent);
        }
    }

    private void startAnimation() {
        viewProgress.setVisibility(View.VISIBLE);
        AnimationDrawable anim = (AnimationDrawable) viewProgress.getDrawable();
        anim.start();
    }

    private void stopAnimation() {
        viewProgress.setVisibility(View.INVISIBLE);
        AnimationDrawable anim = (AnimationDrawable) viewProgress.getDrawable();
        anim.stop();
    }

    static class EpcAdapter extends BaseAdapter {

        LayoutInflater inflater;
        List<EpcRecord> epcs;

        public EpcAdapter(Context context) {
            inflater = LayoutInflater.from(context);
            epcs = new ArrayList<EpcRecord>();
        }

        public void addEpcRecord(String epc) {
            EpcRecord epcRecord = null;
            for (EpcRecord er : epcs) {
                if (er.epc.equals(epc)) {
                    epcRecord = er;
                    break;
                }
            }
            if (epcRecord != null) {
                epcRecord.count++;
            } else {
                epcs.add(new EpcRecord(epcs.size() + 1, epc, 1));
            }
            notifyDataSetInvalidated();
        }

        public void clearEpcRecords() {
            epcs.clear();
            notifyDataSetInvalidated();
        }

        @Override
        public int getCount() {
            return epcs.size();
        }

        @Override
        public Object getItem(int position) {
            return epcs.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.epc_item, null);
            }
            EpcRecord epc = epcs.get(position);
            ((TextView) view.findViewById(R.id.no)).setText(String.valueOf(epc.no));
            ((TextView) view.findViewById(R.id.epc)).setText(epc.epc);
            ((TextView) view.findViewById(R.id.count)).setText(String.valueOf(epc.count));
            return view;
        }
    }

    class StartTask implements Runnable {

        @Override
        public void run() {
            int ret = service.setReadEpcStatus((byte) 1);
            handler.obtainMessage(0, ret).sendToTarget();
        }
    }

    class StopTask implements Runnable {

        @Override
        public void run() {
            int ret = service.setReadEpcStatus((byte) 0);
            handler.obtainMessage(1, ret).sendToTarget();
        }
    }

    class MainHandler extends Handler {

        public MainHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0: {
                    stopAnimation();
                    int ret = (Integer) msg.obj;
                    if (ret == 0) {
                        buttonStart.setText(">>>Stop<<<");
                        viewStatus.setText("Device start read epc");
                    } else {
                        viewStatus.setText("Device start read epc failed: " + ret);
                        buttonSetting.setEnabled(true);
                    }
                    buttonStart.setEnabled(true);
                    break;
                }
                case 1: {
                    stopAnimation();
                    buttonStart.setEnabled(true);
                    buttonSetting.setEnabled(true);
                    buttonStart.setText(">>>Start<<<");
                    viewStatus.setText("Device stop read epc");
                    break;
                }
            }
        }
    }

    private class VolumnBroadcast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            viewVolume.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            viewVolume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        }
    }
}