package com.clearlee.autosendwechatmsg;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.clearlee.autosendwechatmsg.WechatUtils.CONTENT;
import static com.clearlee.autosendwechatmsg.WechatUtils.NAME;


/**
 * 收到短信自动转发到指定的微信
 * Created by rogerGao
 * 2017/12/22.
 */
public class AutoSendMsgService extends AccessibilityService {
    /**
     * 短信数据库变动广播接收
     */

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
    public int onStartCommand(Intent intent, int flags, int startId){
        //短信
        smsContentObserver = new SMSContentObserver(mHandler);
        if (smsContentObserver != null) {
            getContentResolver().registerContentObserver(
                    Uri.parse("content://sms/"), true, smsContentObserver);// 注册监听短信数据库的变化
        }
        //微信
        name="看书熊猫";//发送给微信用户名
        //返回值,异常杀死后尝试重建
        return START_STICKY;
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


                    content=body;
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
/*
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
*/

    /**
     * 自动发微信part1
     *
     */
    private int goWecaht() {
        try {
            NAME = name;
            CONTENT = content;
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
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            Log.d("accessibility授权", "找到微信自动发送消息，然后开启服务即可");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkAndStartService() {
        accessibilityManager = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        if (TextUtils.isEmpty(name)) {
            Log.d("checkAndStartService()", "联系人不能为空");
        }
        if (TextUtils.isEmpty(content)) {
            Log.d("checkAndStartService()", "内容不能为空");
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
            Log.d("setSendStatusText()","微信发送成功");
        } else {
            Log.d("setSendStatusText()","微信发送失败");
        }
    }


    /**
     * 自动发微信part2
     *
     */


    private static final String TAG = "AutoSendMsgService";
    private List<String> allNameList = new ArrayList<>();
    private int mRepeatCount;

    public static boolean hasSend;
    public static final int SEND_FAIL = 0;
    public static final int SEND_SUCCESS = 1;
    public static int SEND_STATUS;

    /**
     * 必须重写的方法，响应各种事件。
     *
     * @param event
     */
    @Override
    public void onAccessibilityEvent(final AccessibilityEvent event) {
        int eventType = event.getEventType();
        switch (eventType) {
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED: {

                String currentActivity = event.getClassName().toString();

                if (hasSend) {
                    return;
                }

                if (currentActivity.equals(WeChatTextWrapper.WechatClass.WECHAT_CLASS_LAUNCHUI)) {
                    handleFlow_LaunchUI();
                } else if (currentActivity.equals(WeChatTextWrapper.WechatClass.WECHAT_CLASS_CONTACTINFOUI)) {
                    handleFlow_ContactInfoUI();
                } else if (currentActivity.equals(WeChatTextWrapper.WechatClass.WECHAT_CLASS_CHATUI)) {
                    handleFlow_ChatUI();
                }
            }
            break;
        }
    }

    private void handleFlow_ChatUI() {

        //如果微信已经处于聊天界面，需要判断当前联系人是不是需要发送的联系人
        String curUserName = WechatUtils.findTextById(this, WeChatTextWrapper.WechatId.WECHATID_CHATUI_USERNAME_ID);
        if (!TextUtils.isEmpty(curUserName) && curUserName.equals(WechatUtils.NAME)) {
            if (WechatUtils.findViewByIdAndPasteContent(this, WeChatTextWrapper.WechatId.WECHATID_CHATUI_EDITTEXT_ID, WechatUtils.CONTENT)) {
                sendContent();
            } else {
                //当前页面可能处于发送语音状态，需要切换成发送文本状态
                WechatUtils.findViewIdAndClick(this, WeChatTextWrapper.WechatId.WECHATID_CHATUI_SWITCH_ID);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (WechatUtils.findViewByIdAndPasteContent(this, WeChatTextWrapper.WechatId.WECHATID_CHATUI_EDITTEXT_ID, WechatUtils.CONTENT)) {
                    sendContent();
                }
            }
        } else {
            //回到主界面
            WechatUtils.findViewIdAndClick(this, WeChatTextWrapper.WechatId.WECHATID_CHATUI_BACK_ID);
        }
    }

    private void handleFlow_ContactInfoUI() {
        WechatUtils.findTextAndClick(this, "发消息");
    }

    private void handleFlow_LaunchUI() {

        try {
            //点击通讯录，跳转到通讯录页面
            //WechatUtils.findTextAndClick(this, "通讯录");

            //Thread.sleep(50);

            //再次点击通讯录，确保通讯录列表移动到了顶部
            WechatUtils.findTextAndClick(this, "通讯录");

            Thread.sleep(200);

            //遍历通讯录联系人列表，查找联系人
            AccessibilityNodeInfo itemInfo = TraversalAndFindContacts();
            if (itemInfo != null) {
                WechatUtils.performClick(itemInfo);
            } else {
                SEND_STATUS = SEND_FAIL;
                resetAndReturnApp();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 从头至尾遍历寻找联系人
     *
     * @return
     */
    private AccessibilityNodeInfo TraversalAndFindContacts() {
        Log.i(TAG,"TraversalAndFindContacts: "+WechatUtils.NAME);
        if (allNameList != null) allNameList.clear();

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        List<AccessibilityNodeInfo> listview = rootNode.findAccessibilityNodeInfosByViewId(WeChatTextWrapper.WechatId.WECHATID_CONTACTUI_LISTVIEW_ID);

        //是否滚动到了底部
        boolean scrollToBottom = false;
        if (listview != null && !listview.isEmpty()) {
            while (true) {
                //获取当前屏幕上的联系人信息
                List<AccessibilityNodeInfo> nameList = rootNode.findAccessibilityNodeInfosByViewId(WeChatTextWrapper.WechatId.WECHATID_CONTACTUI_NAME_ID);
                List<AccessibilityNodeInfo> itemList = rootNode.findAccessibilityNodeInfosByViewId(WeChatTextWrapper.WechatId.WECHATID_CONTACTUI_ITEM_ID);

                if (nameList != null && !nameList.isEmpty()) {
                    for (int i = 0; i < nameList.size(); i++) {
                        if (i == 0) {
                            //必须在一个循环内，防止翻页的时候名字发生重复
                            mRepeatCount = 0;
                        }
                        AccessibilityNodeInfo itemInfo = itemList.get(i);
                        AccessibilityNodeInfo nodeInfo = nameList.get(i);
                        String nickname = nodeInfo.getText().toString();
                        Log.d(TAG, "nickname = " + nickname);
                        if (nickname.equals(WechatUtils.NAME)) {
                            return itemInfo;
                        }
                        if (!allNameList.contains(nickname)) {
                            allNameList.add(nickname);
                        } else if (allNameList.contains(nickname)) {
                            Log.d(TAG, "mRepeatCount = " + mRepeatCount);
                            if (mRepeatCount == 3) {
                                //表示已经滑动到顶部了
                                if (scrollToBottom) {
                                    Log.d(TAG, "没有找到联系人");
                                    //此次发消息操作已经完成
                                    hasSend = true;
                                    return null;
                                }
                                scrollToBottom = true;
                            }
                            mRepeatCount++;
                        }
                    }
                }

                if (!scrollToBottom) {
                    //向下滚动
                    listview.get(0).performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                } else {
                    return null;
                }

                //必须等待，因为需要等待滚动操作完成
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private void sendContent() {
        WechatUtils.findTextAndClick(this, "发送");
        SEND_STATUS = SEND_SUCCESS;
        resetAndReturnApp();
    }

    private void resetAndReturnApp() {
        hasSend = true;
        ActivityManager activtyManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfos = activtyManager.getRunningTasks(3);
        for (ActivityManager.RunningTaskInfo runningTaskInfo : runningTaskInfos) {
            if (this.getPackageName().equals(runningTaskInfo.topActivity.getPackageName())) {
                activtyManager.moveTaskToFront(runningTaskInfo.id, ActivityManager.MOVE_TASK_WITH_HOME);
                return;
            }
        }
    }

    @Override
    public void onInterrupt() {

    }


}
