package cn.myflv.android.noanr.utils;

import cn.myflv.android.noanr.entity.FieldEnum;
import de.robv.android.xposed.XposedHelpers;

public class BroadcastUtil {
    public static Object getReceiverList(Object broadcastFilter) {
        return XposedHelpers.getObjectField(broadcastFilter, FieldEnum.receiverList);
    }

    public static Object getProcessRecord(Object receiverList) {
        return XposedHelpers.getObjectField(receiverList, FieldEnum.app);
    }

    public static Object getApplicationInfo(Object receiverList) {
        if (receiverList == null) return null;
        Object processRecord = getProcessRecord(receiverList);
        if (processRecord == null) return null;
        return ProcessUtil.getApplicationInfo(processRecord);
    }

    public static Object getActivityManagerService(Object broadcastQueue) {
        return XposedHelpers.getObjectField(broadcastQueue, FieldEnum.mService);
    }

    public static void clear(Object receiverList) {
        XposedHelpers.setObjectField(receiverList, FieldEnum.app, null);
    }

}
