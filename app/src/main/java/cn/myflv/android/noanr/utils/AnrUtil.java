package cn.myflv.android.noanr.utils;

import android.os.Build;

import java.util.List;

import cn.myflv.android.noanr.entity.ClassEnum;
import cn.myflv.android.noanr.entity.FieldEnum;
import cn.myflv.android.noanr.entity.MethodEnum;
import de.robv.android.xposed.XposedHelpers;

public class AnrUtil {
    public static List<Object> mAnrRecords(Object anrHelper) {
        Object mAnrRecords = XposedHelpers.getObjectField(anrHelper, FieldEnum.mAnrRecords);
        return ObjectUtil.toObjList(mAnrRecords);
    }

    public static Object newInstance(ClassLoader classLoader, Object... args) {
        Class<?> AnrRecord = XposedHelpers.findClass(ClassEnum.AnrRecord, classLoader);
        return XposedHelpers.newInstance(AnrRecord, args);
    }

    public static void startAnrConsumerIfNeeded(Object anrHelper) {
        XposedHelpers.callMethod(anrHelper, MethodEnum.startAnrConsumerIfNeeded);
    }

    public static void resetNotResponding(Object processRecord) {
        Object activityManagerService = ProcessUtil.getActivityManagerService(processRecord);
        if (activityManagerService == null) return;
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.S) {
            if (AppUtil.getActivityManagerGlobalLock(activityManagerService) == null) {
                return;
            }
            synchronized (AppUtil.getActivityManagerGlobalLock(activityManagerService)) {
                Object mErrorState = XposedHelpers.getObjectField(processRecord, FieldEnum.mErrorState);
                if (mErrorState == null) return;
                XposedHelpers.callMethod(mErrorState, MethodEnum.setNotResponding, false);
            }
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            synchronized (ProcessUtil.getActivityManagerService(processRecord)) {
                XposedHelpers.callMethod(processRecord, MethodEnum.setNotResponding, false);
            }

        }
    }

}
