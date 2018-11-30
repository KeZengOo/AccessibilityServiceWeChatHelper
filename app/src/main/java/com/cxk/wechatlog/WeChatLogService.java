package com.cxk.wechatlog;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.List;

/**
 * Created by 曾轲
 * <p>
 * 获取即时微信聊天记录服务类,该功能在微信6.5.8上可正常使用
 */

public class WeChatLogService extends AccessibilityService {

    /**
     * 聊天对象
     */
    private String ChatName;

    /**
     * 小视频的秒数，格式为00:00
     */
    private String VideoSecond;


    private String ChatRecord;

    /**
     *  根据聊天列表两次滚动后最底部的人是不是同一个,来判断聊天列表是否到了底部
     */
    private boolean listIsBottom = false;
    private String bottomName = "";

    /**
     *  根据聊天消息滚动前后三条是否重复,用来判断消息界面是否滚动到了最顶部
     */
    private boolean chatMessageIsTop = false;
    private String chatMessage1 = "";
    private String chatMessage2 = "";
    private String chatMessage3 = "";
    String topMessage1= "";
    String topMessage2= "";
    String topMessage3= "";
    /**
     * 聊天人的姓名
     */
    String chatName= "";

    /**
     * 聊天信息保存目录
     */
    private String mCurrApkPath =  Environment.getExternalStorageDirectory().getPath() + "/";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        switch (eventType) {
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED: {
                String currentActivity = event.getClassName().toString();
                //如果在微信界面
                if (currentActivity.equals(WeChatTextWrapper.WechatClass.WECHAT_CLASS_LAUNCHUI)) {
                    Log.e("界面","微信主页");
                    AccessibilityNodeInfo rootNodeInfo = getRootInActiveWindow();
                    //获取聊天列表的 listview
                    List<AccessibilityNodeInfo> listview = rootNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/bny");
                    if (listview!=null&&listview.size() != 0) {
                        //如果 listview 没有滑动到最底部,就一直无限循环遍历聊天记录
                        while (!listIsBottom) {
                            //获取当前页面聊天 list的单个条目和聊天人名字
                            List<AccessibilityNodeInfo> ChatList = rootNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/agu");
                            List<AccessibilityNodeInfo> NameList = rootNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/agw");
                            //如果没有聊天信息,直接跳出,并终止循环
                            if(NameList.size()==0||NameList==null){
                                listIsBottom=true;
                               return;
                            }
                            //获取当前list 最后一个人员的名字,与上一次滑动比较,如果相同说明到了最底部
                            String lastName = NameList.get(NameList.size() - 1).getText().toString();
                            if (bottomName.equals(lastName)) {
                                listIsBottom = true;
                            } else {
                                bottomName = lastName;
                                listIsBottom = false;
                                //遍历循环列表,进入聊天详情页进行备份
                                if (ChatList!=null&ChatList.size() != 0) {
                                    for (int j = 0; j < ChatList.size(); j++) {
                                        //页面停顿防止过快无法找到目标
                                        try {
                                            Thread.sleep(500);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        //进入聊天界面
                                        ChatList.get(j).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                        //延迟防止页面还没完全载入,拿不到根节点信息
                                        try {
                                            Thread.sleep(500);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        AccessibilityNodeInfo chatRootNodeInfo = getRootInActiveWindow();
                                        //遍历下一个人的聊天记录,需要重置定置状态
                                        chatMessageIsTop=false;
                                        getWeChatLog(chatRootNodeInfo);
                                    }
                                }
                                //循环遍历完当前页面的列表后滑动遍历下一个页面
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                listview.get(0).performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                           }
                        }else{

                    }

                    } else if (currentActivity.equals(WeChatTextWrapper.WechatClass.WECHAT_CLASS_CONTACTINFOUI)) {
                        Log.e("界面","联系人界面");
                    } else if (currentActivity.equals(WeChatTextWrapper.WechatClass.WECHAT_CLASS_CHATUI)) {
                        Log.e("界面","聊天界面");
                    }
                }
                break;
            }


    }

    /**
     * 遍历获取聊天消息
     * @param rootNode
     */
    private void getWeChatLog(AccessibilityNodeInfo rootNode) {
        if (rootNode != null) {
            //获取聊天人的姓名
            List<AccessibilityNodeInfo> listName = rootNode.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/gp");
            if(listName!=null&&listName.size()!=0){
               chatName = listName.get(0).getText().toString();
                Log.e("name",chatName);
            }

            //获取聊天详情页的listview
            List<AccessibilityNodeInfo> listChatRecord = rootNode.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/a3e");
            //如果没有聊天记录,直接返回
            if(listChatRecord!=null&&listChatRecord.size()==0){
                Log.e("消息类型","当前没有聊天记录");
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                return;
            }
            //利用姓名创建文件


            //当消息详情不在最顶部时无限循环遍历消息记录
            while(!chatMessageIsTop){
            //有聊天记录,开始遍历循环
            //获取聊天头像list
            List<AccessibilityNodeInfo> imageName = rootNode.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ik");
            //获取聊天信息list
            List<AccessibilityNodeInfo> record = rootNode.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/im");

            //有聊天头像说明有聊天记录(但是不一定有文字信息,可能是图片,分享等等)
            if (imageName!=null&&imageName.size() != 0) {
                //如果record的 size 为零说明当前截取到的 list 没有文字
                if (record.size() == 0) {
                    Log.e("消息类型","当前没有文字消息");
                    //判断当前这条消息是不是和上一条一样，防止重复
                        //获取聊天对象
                        ChatName = imageName.get(0).getContentDescription().toString().replace("头像", "");
                        Log.e("AAAA", ChatName + "：" + "对方发的是图片或者表情");

                        try {
                            Thread.sleep(800);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        //当前 listview 显示的都是非文字消息,乡下滚动一次后再次遍历循环
                        listChatRecord.get(0).performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
                        return;
                //有文本消息,开始辨析备份文本消息
                }else {
                    //获取当前页面顶部的三条文字,与上一次滚动的三条文字对比,判断是否滚动到最顶部了
                    switch (record.size()) { //判断 size 防止索引越界
                        case 1:
                             topMessage1 = record.get(record.size() - 1).getText().toString();
                             topMessage2="";
                             chatMessage2="";
                             topMessage3="";
                             chatMessage3="";
                            break;

                        case 2:
                             topMessage1 = record.get(record.size() - 1).getText().toString();
                             topMessage2 = record.get(record.size() - 2).getText().toString();

                            topMessage3="";
                            chatMessage3="";
                            break;
                        case 3:
                             topMessage1 = record.get(record.size() - 1).getText().toString();
                             topMessage2 = record.get(record.size() - 2).getText().toString();
                             topMessage3 = record.get(record.size() - 3).getText().toString();

                            break;

                            default:
                                topMessage1 = record.get(record.size() - 1).getText().toString();
                                topMessage2 = record.get(record.size() - 2).getText().toString();
                                topMessage3 = record.get(record.size() - 3).getText().toString();

                                break;
                    }
                        //如果三条消息都重复证明已经滚动到了最顶部,返回聊天列表
                        if(chatMessage1.equals(topMessage1)&&chatMessage2.equals(topMessage2)&&chatMessage3.equals(topMessage3)){
                            chatMessageIsTop=true;
                            performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                        }else{
                            //不是第一条,重新赋值最后三条消息记录,遍历保存文字信息
                            chatMessage1=topMessage1;
                            chatMessage2=topMessage2;
                            chatMessage3=topMessage3;
                            chatMessageIsTop=false;
                            //遍历获取文字,并写入文件
                            for (int i = record.size()-1; i >= 0; i--) {
                                //保存消息
                                //Log.e("文字消息",record.get(i).getText().toString());
                                //Log.e("index",record.get(i).toString());
                                //获取文本消息的位置,根据位置来判断但前消息是谁发的
                                Rect outBounds = new Rect();
                                record.get(i).getBoundsInScreen(outBounds);
                                //因为对方发送的消息的左侧 left 坐标是永远不会变的,自己发送的消息会被换行折叠,因此可以用此来区分
                                String leftIndex = outBounds.left+"";
                                //不同手机这个值不一样,需要自己适配
                                if(leftIndex.equals("156")){
                                    //对方发送的消息
                                    saveMassage(chatName,record.get(i).getText().toString());
                                }else{
                                    //自己发送的消息
                                    saveMassage("我",record.get(i).getText().toString());
                                }

                            }
                            //遍历完毕后滚动下一屏聊天记录开始,并停止.8秒等待聊天记录加载
                            listChatRecord.get(0).performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
                            try {
                                Thread.sleep(800);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    //有聊天列表但是没有聊天头像和文字消息.类似于招行信用卡的公众号,不作处理直接返回聊天列表遍历下一个
                    }else{
                        chatMessageIsTop=true;
                        performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                         }
                    }
                }
            }
        }

    private void saveMassage(String chatName, String message) {


    }


    /**
     * 遍历所有控件，找到头像Imagview，里面有对联系人的描述
     */
    private void GetChatName(AccessibilityNodeInfo node) {
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo node1 = node.getChild(i);
            if ("android.widget.ImageView".equals(node1.getClassName()) && node1.isClickable()) {
                //获取聊天对象,这里两个if是为了确定找到的这个ImageView是头像的
                if (!TextUtils.isEmpty(node1.getContentDescription())) {
                    ChatName = node1.getContentDescription().toString();
                    if (ChatName.contains("头像")) {
                        ChatName = ChatName.replace("头像", "");
                    }
                }

            }
            GetChatName(node1);
        }
    }


    /**
     * 必须重写的方法：系统要中断此service返回的响应时会调用。在整个生命周期会被调用多次。
     */
    @Override
    public void onInterrupt() {
        Toast.makeText(this, "我快被终结了啊-----", Toast.LENGTH_SHORT).show();
    }

    /**
     * 服务开始连接
     */
    @Override
    protected void onServiceConnected() {
        Toast.makeText(this, "服务已开启", Toast.LENGTH_SHORT).show();
        super.onServiceConnected();
    }

    /**
     * 服务断开
     *
     * @param intent
     * @return
     */
    @Override
    public boolean onUnbind(Intent intent) {
        Toast.makeText(this, "服务已被关闭", Toast.LENGTH_SHORT).show();
        return super.onUnbind(intent);
    }

    /**
     * 遍历所有控件:这里分四种情况
     * 文字聊天: 一个TextView，并且他的父布局是android.widget.RelativeLayout
     * 语音的秒数: 一个TextView，并且他的父布局是android.widget.RelativeLayout，但是他的格式是0"的格式，所以可以通过这个来区分
     * 图片:一个ImageView,并且他的父布局是android.widget.FrameLayout,描述中包含“图片”字样（发过去的图片），发回来的图片现在还无法监听
     * 表情:也是一个ImageView,并且他的父布局是android.widget.LinearLayout
     * 小视频的秒数:一个TextView，并且他的父布局是android.widget.FrameLayout，但是他的格式是00:00"的格式，所以可以通过这个来区分
     *
     * @param node
     */
    public void GetChatRecord(AccessibilityNodeInfo node) {
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo nodeChild = node.getChild(i);

            //聊天内容是:文字聊天(包含语音秒数)
            if ("android.widget.TextView".equals(nodeChild.getClassName()) && "android.widget.RelativeLayout".equals(nodeChild.getParent().getClassName().toString())) {
                if (!TextUtils.isEmpty(nodeChild.getText())) {
                    String RecordText = nodeChild.getText().toString();
                    //这里加个if是为了防止多次触发TYPE_VIEW_SCROLLED而打印重复的信息

                    if (!RecordText.equals(ChatRecord)) {
                        ChatRecord = RecordText;
                        //判断是语音秒数还是正常的文字聊天,语音的话秒数格式为5"
                        if (ChatRecord.contains("\"")) {
                            Toast.makeText(this, ChatName + "发了一条" + ChatRecord + "的语音", Toast.LENGTH_SHORT).show();

                            Log.e("WeChatLog",ChatName + "发了一条" + ChatRecord + "的语音");
                        } else {
                            //这里在加多一层过滤条件，确保得到的是聊天信息，因为有可能是其他TextView的干扰，例如名片等
                            if (nodeChild.isLongClickable()) {
                                Toast.makeText(this, ChatName + "：" + ChatRecord, Toast.LENGTH_SHORT).show();

                                Log.e("WeChatLog",ChatName + "：" + ChatRecord);
                            }

                        }
                        return;
                    }
                }
            }

            //聊天内容是:表情
            if ("android.widget.ImageView".equals(nodeChild.getClassName()) && "android.widget.LinearLayout".equals(nodeChild.getParent().getClassName().toString())) {
                Toast.makeText(this, ChatName+"发的是表情", Toast.LENGTH_SHORT).show();

                Log.e("WeChatLog",ChatName+"发的是表情");

                return;
            }

            //聊天内容是:图片
            if ("android.widget.ImageView".equals(nodeChild.getClassName())) {
                //安装软件的这一方发的图片（另一方发的暂时没实现）
                if("android.widget.FrameLayout".equals(nodeChild.getParent().getClassName().toString())){
                    if(!TextUtils.isEmpty(nodeChild.getContentDescription())){
                        if(nodeChild.getContentDescription().toString().contains("图片")){
                            Toast.makeText(this, ChatName+"发的是图片", Toast.LENGTH_SHORT).show();

                            Log.e("WeChatLog",ChatName+"发的是图片");
                        }
                    }
                }
            }

            //聊天内容是:小视频秒数,格式为00：00
            if ("android.widget.TextView".equals(nodeChild.getClassName()) && "android.widget.FrameLayout".equals(nodeChild.getParent().getClassName().toString())) {
                if (!TextUtils.isEmpty(nodeChild.getText())) {
                    String second = nodeChild.getText().toString().replace(":", "");
                    //正则表达式，确定是不是纯数字,并且做重复判断
                    if (second.matches("[0-9]+") && !second.equals(VideoSecond)) {
                        VideoSecond = second;
                        Toast.makeText(this, ChatName + "发了一段" + nodeChild.getText().toString() + "的小视频", Toast.LENGTH_SHORT).show();

                        Log.e("WeChatLog","发了一段" + nodeChild.getText().toString() + "的小视频");
                    }
                }

            }

            GetChatRecord(nodeChild);
        }
    }


}