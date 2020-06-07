package com.dds.java.voip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.dds.App;
import com.dds.skywebrtc.AVEngineKit;

/**
 * Created by dds on 2019/8/25.
 * android_shuai@163.com
 */
public class VoipReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Utils.ACTION_VOIP_RECEIVER.equals(action)) {
            String room = intent.getStringExtra("room");
            boolean audioOnly = intent.getBooleanExtra("audioOnly", true);
            String inviteId = intent.getStringExtra("inviteId");
            String userList = intent.getStringExtra("userList");
            String[] list = userList.split(",");
            AVEngineKit.init(new VoipEvent());
            boolean b = AVEngineKit.Instance().startCall(App.getInstance(), room, 2, inviteId, audioOnly, true);
            if (b) {
                if (list.length == 1) {
                    CallSingleActivity.openActivity(context, inviteId, false, audioOnly);
                } else {
                    // 群聊
                }

            }


        }

    }
}
