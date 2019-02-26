package com.clearlee.autosendwechatmsg;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.clearlee.autosendwechatmsg.AutoSendMsgService.SEND_STATUS;
import static com.clearlee.autosendwechatmsg.AutoSendMsgService.SEND_SUCCESS;
import static com.clearlee.autosendwechatmsg.AutoSendMsgService.hasSend;
import static com.clearlee.autosendwechatmsg.WechatUtils.CONTENT;
import static com.clearlee.autosendwechatmsg.WechatUtils.NAME;

/**
 * Created by Clearlee
 * 2017/12/22.
 */
public class MainActivity extends AppCompatActivity {

    private TextView start, sendStatus;
    private EditText sendName, sendContent;
    private AccessibilityManager accessibilityManager;
    private String name, content;



    private SMSContentObserver smsContentObserver;
    protected static final int MSG_INBOX = 1;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_INBOX:
                    setSmsCode();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //短信
        smsContentObserver = new SMSContentObserver(MainActivity.this, mHandler);
        //微信
        init();
    }

    private void setSmsCode() {
        Log.i("zhang", "收到短信了！");
//        login_et_sms_code.setText("hahhaha");
        Cursor cursor = null;
        // 添加异常捕捉


        try {
            cursor = getContentResolver().query(
                    Uri.parse("content://sms"),
                    new String[] { "_id", "address", "body", "date" },
                    null, null, "date desc"); //
            if (cursor != null) {
                String body = "";
                if (cursor.moveToNext()) {
                    body = cursor.getString(cursor.getColumnIndex("body"));                    // 在这里获取短信信息
                    //-----------------写自己的逻辑

                    sendName.setText("看书熊猫");
                    sendContent.setText(body);
                    checkAndStartService();
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    protected void onResume() {

        super.onResume();
        if (smsContentObserver != null) {
            getContentResolver().registerContentObserver(
                    Uri.parse("content://sms/"), true, smsContentObserver);// 注册监听短信数据库的变化
        }
    }

    @Override
    protected void onPause() {

        super.onPause();
        if (smsContentObserver != null) {
            getContentResolver().unregisterContentObserver(smsContentObserver);// 取消监听短信数据库的变化
        }

    }


    private void init() {
        start = (TextView) findViewById(R.id.testWechat);
        sendName = (EditText) findViewById(R.id.sendName);
        sendContent = (EditText) findViewById(R.id.sendContent);
        sendStatus = (TextView) findViewById(R.id.sendStatus);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAndStartService();
            }
        });
    }

    private int goWecaht() {
        try {
            setValue(name, content);
            hasSend = false;
            Intent intent = new Intent();
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
            intent.setClassName(WeChatTextWrapper.WECAHT_PACKAGENAME, WeChatTextWrapper.WechatClass.WECHAT_CLASS_LAUNCHUI);
            startActivity(intent);

            while (true) {
                if (hasSend) {
                    return SEND_STATUS;
                } else {
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                        openService();
                        e.printStackTrace();
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return SEND_STATUS;
        }
    }


    private void openService() {
        try {
            //打开系统设置中辅助功能
            Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            Toast.makeText(MainActivity.this, "找到微信自动发送消息，然后开启服务即可", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkAndStartService() {
        accessibilityManager = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);

        name = sendName.getText().toString();
        content = sendContent.getText().toString();

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(MainActivity.this, "联系人不能为空", Toast.LENGTH_SHORT).show();
        }
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(MainActivity.this, "内容不能为空", Toast.LENGTH_SHORT).show();
        }

        if (!accessibilityManager.isEnabled()) {
            openService();

        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    statusHandler.sendEmptyMessage(goWecaht());
                }
            }).start();
        }
    }

    Handler statusHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            setSendStatusText(msg.what);
        }
    };

    private void setSendStatusText(int status) {
        if (status == SEND_SUCCESS) {
            sendStatus.setText("微信发送成功");
        } else {
            sendStatus.setText("微信发送失败");
        }
    }

    public void setValue(String name, String content) {
        NAME = name;
        CONTENT = content;
        hasSend = false;
    }

}
