package com.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.demo.StatesButton.StateStyle;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private @interface ButtonStateType {
        int NORMAL = 0;
        int DOWNLOADING = -1;
        int PAUSE = -2;
        int INSTALLING = -3;
        int OPEN = -4;
        int FAIL = -5;
        int WAIT = -6;
    }

    private StateStyle mNormalStateStyle;
    private StateStyle mDownloadingStateStyle;
    private StateStyle mInstallingStateStyle;
    private StateStyle mSuccessStateStyle;
    private StateStyle mFailStateStyle;
    private StateStyle mWaitingStateStyle;
    private StateStyle mPauseStateStyle;

    private StateStyle mNormalStateStyle2;
    private StateStyle mDownloadingStateStyle2;
    private StateStyle mInstallingStateStyle2;
    private StateStyle mSuccessStateStyle2;
    private StateStyle mFailStateStyle2;
    private StateStyle mWaitingStateStyle2;
    private StateStyle mPauseStateStyle2;

    private BroadcastReceiver receiver = new AppStoreDownloadReceiver();
    private HashMap<String, StatesButton> stateButtons = new HashMap<>();

    private String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StatesButton statesButton = findViewById(R.id.state1);
        addStateStyle(statesButton);
        statesButton.setState(ButtonStateType.NORMAL);

        StatesButton statesButton2 = findViewById(R.id.state2);
        addStateStyle2(statesButton2);
        statesButton2.setState(ButtonStateType.NORMAL);

        initButtonData(statesButton, "com.ten.cc");
        initButtonData(statesButton2, "com.demo.ss");

        IntentFilter filter = new IntentFilter();
        filter.addAction(ImitateStore.ACTION);
        registerReceiver(receiver, filter);
    }

    private void initButtonData(StatesButton statesButton, String packageName) {
        View.OnClickListener appCardOnClickListener = v -> {
            if (statesButton.getState() == ButtonStateType.INSTALLING
                    || statesButton.getState() == ButtonStateType.WAIT) {
                return;
            }
            if (statesButton.getState() == ButtonStateType.NORMAL) {
                ImitateStore.getInstance(this, packageName).startDownload();
            } else if (statesButton.getState() == ButtonStateType.PAUSE) {
                ImitateStore.getInstance(this, packageName).resume();
            } else if (statesButton.getState() == ButtonStateType.DOWNLOADING) {
                ImitateStore.getInstance(this, packageName).pause();
            } else if (statesButton.getState() == ButtonStateType.OPEN) {
                statesButton.setState(ButtonStateType.FAIL);
                Toast.makeText(this, "打开失败", Toast.LENGTH_SHORT).show();
            } else if (statesButton.getState() == ButtonStateType.FAIL) {
                statesButton.setState(ButtonStateType.NORMAL);
                Toast.makeText(this, "状态重置成功！", Toast.LENGTH_SHORT).show();
            }
        };
        statesButton.setOnClickListener(appCardOnClickListener);

        if (stateButtons.containsKey(packageName)) return;
        stateButtons.put(packageName, statesButton);
    }

    private class AppStoreDownloadReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String packageName = intent.getStringExtra("packageName");
            int state = intent.getIntExtra("status", Integer.MIN_VALUE);
            int progress = intent.getIntExtra("progress", 0);

            StatesButton statesButton = stateButtons.get(packageName);
            if (statesButton == null) return;

            Log.d(TAG, "onReceive: " + packageName + " " + state + " " + progress);

            switch (state) {
                case ImitateStore.STATUE_NORMAL:
                    statesButton.setState(ButtonStateType.NORMAL);
                    break;
                case ImitateStore.STATUE_DOWNLOADING:
                    statesButton.setState(ButtonStateType.DOWNLOADING);
                    statesButton.setProgress(progress);
                    break;
                case ImitateStore.STATUE_INSTALLING:
                    statesButton.setState(ButtonStateType.INSTALLING);
                    break;
                case ImitateStore.STATUE_WAIT:
                    statesButton.setState(ButtonStateType.WAIT);
                    break;
                case ImitateStore.STATUE_OPEN:
                    statesButton.setState(ButtonStateType.OPEN);
                    break;
                case ImitateStore.STATUE_PAUSE:
                    statesButton.setState(ButtonStateType.PAUSE);
                    break;
                case ImitateStore.STATUE_FAIL:
                    statesButton.setState(ButtonStateType.FAIL);
                    break;
            }
        }
    }

    private void addStateStyle(StatesButton statesButton) {
        mNormalStateStyle = new StateStyle()
                .setBgColor(
                        getResources().getColor(R.color.app_state_bg_normal))
                .setText("安装")
                .setTextColor(
                        getResources().getColor(R.color.app_state_text_normal));
        mDownloadingStateStyle = new StateStyle()
                .setBgColor(
                        getResources().getColor(R.color.app_state_bg_normal),
                        getResources().getColor(R.color.app_state_bg_cover_normal))
                .setTextColor(
                        getResources().getColor(R.color.app_state_text_normal))
                .setKeepPreviousProgress(true);
        mInstallingStateStyle = new StateStyle()
                .setBgColor(
                        getResources().getColor(R.color.app_state_bg_cover_normal))
                .setText("安装中")
                .setTextColor(
                        getResources().getColor(R.color.app_state_text_normal));
        mSuccessStateStyle = new StateStyle()
                .setBgColor(
                        getResources().getColor(R.color.app_state_bg_open))
                .setText("启动").setTextColor(Color.WHITE);
        mFailStateStyle = new StateStyle().setText("失败")
                .setTextColor(
                        getResources().getColor(R.color.app_state_text_fail));
        mWaitingStateStyle = mNormalStateStyle.cloneState()
                .setText("等待中");
        mPauseStateStyle = mDownloadingStateStyle.cloneState()
                .setText("继续")
                .setKeepPreviousProgress(true);

        statesButton.addStateStyle(ButtonStateType.NORMAL, mNormalStateStyle);
        statesButton.addStateStyle(ButtonStateType.DOWNLOADING, mDownloadingStateStyle);
        statesButton.addStateStyle(ButtonStateType.PAUSE, mPauseStateStyle);
        statesButton.addStateStyle(ButtonStateType.INSTALLING, mInstallingStateStyle);
        statesButton.addStateStyle(ButtonStateType.OPEN, mSuccessStateStyle);
        statesButton.addStateStyle(ButtonStateType.FAIL, mFailStateStyle);
        statesButton.addStateStyle(ButtonStateType.WAIT, mWaitingStateStyle);
    }

    private void addStateStyle2(StatesButton statesButton) {
        int green = getResources().getColor(R.color.colorPrimary);
        int greenDeep = getResources().getColor(R.color.colorPrimaryDark);
        mNormalStateStyle2 = new StateStyle().setBorderColor(green)
                .setBgColor(Color.WHITE)
                .setText("开始")
                .setTextColor(green)
                .setRadius(80)
                .setBorderWidth(3F);
        mDownloadingStateStyle2 = mNormalStateStyle2.cloneState()
                .setKeepPreviousProgress(true)
                .setBgColor(Color.WHITE, greenDeep)
                .setRadius(80)
                .setBorderWidth(3F)
                .setText(progress -> "当前已处理到 " + String.format("%.2f", progress) + "% 了！")
                .setTextColor(green, Color.WHITE);
        mInstallingStateStyle2 = new StateStyle()
                .setBorderColor(green)
                .setBgColor(greenDeep)
                .setText("装载中...").setTextColor(Color.WHITE)
                .setRadius(80).setBorderWidth(3F);
        mSuccessStateStyle2 = new StateStyle().setBorderColor(Color.GRAY).setBgColor(Color.WHITE)
                .setText("打开").setTextColor(Color.BLACK)
                .setRadius(80).setBorderWidth(3F);
        mFailStateStyle2 = mSuccessStateStyle2.cloneState().setBgColor(Color.DKGRAY)
                .setText("失败了").setTextColor(Color.GRAY)
                .setRadius(80).setBorderWidth(3F);
        mWaitingStateStyle2 = mNormalStateStyle2.cloneState().setText("等待中")
                .setRadius(80).setBorderWidth(3F);
        mPauseStateStyle2 = mDownloadingStateStyle2.cloneState()
                .setText("暂停中").setRadius(80).setBorderWidth(3F)
                .setKeepPreviousProgress(true);

        statesButton.addStateStyle(ButtonStateType.NORMAL, mNormalStateStyle2);
        statesButton.addStateStyle(ButtonStateType.DOWNLOADING, mDownloadingStateStyle2);
        statesButton.addStateStyle(ButtonStateType.PAUSE, mPauseStateStyle2);
        statesButton.addStateStyle(ButtonStateType.INSTALLING, mInstallingStateStyle2);
        statesButton.addStateStyle(ButtonStateType.OPEN, mSuccessStateStyle2);
        statesButton.addStateStyle(ButtonStateType.FAIL, mFailStateStyle2);
        statesButton.addStateStyle(ButtonStateType.WAIT, mWaitingStateStyle2);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }
}
