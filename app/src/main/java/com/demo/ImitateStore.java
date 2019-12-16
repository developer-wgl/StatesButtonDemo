package com.demo;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImitateStore {

    public static String ACTION = "com.test.demo.store";
    public static final int STATUE_NORMAL = 1;
    public static final int STATUE_DOWNLOADING = 2;
    public static final int STATUE_PAUSE = 3;
    public static final int STATUE_INSTALLING = 4;
    public static final int STATUE_OPEN = 5;
    public static final int STATUE_FAIL = 6;
    public static final int STATUE_WAIT = 7;

    private static String TAG = ImitateStore.class.getSimpleName();
    private static HashMap<String, ImitateStore> INSTANCE = new HashMap<>();
    private static ExecutorService executes = Executors.newFixedThreadPool(5);

    public static ImitateStore getInstance(Context context, String packageName) {
        if (INSTANCE.containsKey(packageName)) {
            return INSTANCE.get(packageName);
        }
        ImitateStore imitateStore = new ImitateStore(context, packageName);
        INSTANCE.put(packageName, imitateStore);
        return imitateStore;
    }

    private Context context;
    private String packageName;
    private boolean isDownload = true;
    private int progress = 0;

    private ImitateStore(Context context, String packageName) {
        this.context = context;
        this.packageName = packageName;
    }

    public void startDownload() {
        executes.execute(() -> {
            try {
                context.sendBroadcast(getIntent(STATUE_WAIT, progress));

                Thread.sleep(1000);

                while (progress <= 100) {
                    if (!isDownload) continue;
                    context.sendBroadcast(getIntent(STATUE_DOWNLOADING, progress));
                    Thread.sleep(600);
                    progress += 10;
                }

                progress = 0;
                isDownload = true;

                context.sendBroadcast(getIntent(STATUE_INSTALLING, progress));
                Thread.sleep(3000);

                context.sendBroadcast(getIntent(STATUE_OPEN, progress));
                Thread.sleep(4000);

//                context.sendBroadcast(getIntent(STATUE_NORMAL, progress));
//                Thread.sleep(5000);

            } catch (InterruptedException e) {
                context.sendBroadcast(getIntent(STATUE_FAIL, progress));
                Log.e(TAG, "startDownload: ", e);
            }
        });

    }

    private Intent getIntent(int state, int progress) {
        Intent intent = new Intent(ACTION);
        intent.putExtra("packageName", packageName);
        intent.putExtra("status", state);
        intent.putExtra("progress", progress);
        return intent;
    }

    public void pause() {
        isDownload = false;
        context.sendBroadcast(getIntent(STATUE_PAUSE, progress));
    }

    public void resume() {
        isDownload = true;
    }
}
