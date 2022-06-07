package cn.myflv.android.noanr.utils;

import java.util.List;

import cn.myflv.android.noanr.entity.FieldEnum;
import cn.myflv.android.noanr.entity.MethodEnum;
import de.robv.android.xposed.XposedHelpers;

public class ProcessUtil {
    public final static String Process = "android.os.Process";

    public final static int Freezer = 19;
    public final static int UnFreezer = 18;

    public static Class<?> getProcess(ClassLoader classLoader) {
        return XposedHelpers.findClass(Process, classLoader);
    }

    public static void freezer(int pid, ClassLoader classLoader) {
        Class<?> Process = getProcess(classLoader);
        XposedHelpers.callStaticMethod(Process, MethodEnum.sendSignal, pid, Freezer);
    }

    public static void freezer(List<Integer> pidList, ClassLoader classLoader) {
        for (Integer pid : pidList) {
            freezer(pid, classLoader);
        }
    }

    public static void unFreezer(List<Integer> pidList, ClassLoader classLoader) {
        for (Integer pid : pidList) {
            unFreezer(pid, classLoader);
        }
    }

    public static void unFreezer(int pid, ClassLoader classLoader) {
        Class<?> Process = getProcess(classLoader);
        XposedHelpers.callStaticMethod(Process, MethodEnum.sendSignal, pid, UnFreezer);
    }

    public static Object getApplicationInfo(Object processRecord) {
        return XposedHelpers.getObjectField(processRecord, FieldEnum.info);
    }

    public static List<?> getProcessList(Object activityManagerService) {
        Object mProcessList = XposedHelpers.getObjectField(activityManagerService, FieldEnum.mProcessList);
        Object mLruProcesses = XposedHelpers.getObjectField(mProcessList, FieldEnum.mLruProcesses);
        return ObjectUtil.toList(mLruProcesses);
    }

    public static String getProcessName(Object processRecord) {
        return (String) XposedHelpers.getObjectField(processRecord, FieldEnum.processName);
    }

    public static int getUid(Object processRecord) {
        return XposedHelpers.getIntField(processRecord, FieldEnum.uid);
    }

    public static int getPid(Object processRecord) {
        return XposedHelpers.getIntField(processRecord, FieldEnum.pid);
    }

    public static Object getActivityManagerService(Object processRecord){
        return XposedHelpers.getObjectField(processRecord,FieldEnum.mService);
    }

}
