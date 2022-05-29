package cn.myflv.android.noanr.utils;

import cn.myflv.android.noanr.entity.MethodEnum;
import de.robv.android.xposed.XposedHelpers;

public class AppOpsUtil {
    public final static String AppOpsManager = "android.app.AppOpsManager";

    public final static String OP_WAKE_LOCK = "OP_WAKE_LOCK";
    public final static String OP_RUN_ANY_IN_BACKGROUND = "OP_RUN_ANY_IN_BACKGROUND";
    public final static String MODE_IGNORED = "MODE_IGNORED";
    public final static String MODE_DEFAULT = "MODE_DEFAULT";

    public static Class<?> getAppOpsManager(ClassLoader classLoader) {
        return XposedHelpers.findClass(AppOpsManager, classLoader);
    }

    public static int OP_WAKE_LOCK(ClassLoader classLoader) {
        Class<?> AppOpsManager = getAppOpsManager(classLoader);
        return XposedHelpers.getStaticIntField(AppOpsManager, OP_WAKE_LOCK);
    }

    public static int MODE_IGNORED(ClassLoader classLoader) {
        Class<?> AppOpsManager = getAppOpsManager(classLoader);
        return XposedHelpers.getStaticIntField(AppOpsManager, MODE_IGNORED);
    }

    public static boolean checkOpIgnore(Object appOpsManager, String op, int uid, String packageName, ClassLoader classLoader) {
        Class<?> AppOpsManager = getAppOpsManager(classLoader);
        int OP = XposedHelpers.getStaticIntField(AppOpsManager, op);
        int mode = (int) XposedHelpers.callMethod(appOpsManager, MethodEnum.checkOpNoThrow, OP, uid, packageName);
        return mode == MODE_IGNORED(classLoader);
    }

    public static void setMode(Object appOpsManager, String op, int uid, String packageName, String mode, ClassLoader classLoader) {
        Class<?> AppOpsManager = getAppOpsManager(classLoader);
        int code = XposedHelpers.getStaticIntField(AppOpsManager, op);
        int state = XposedHelpers.getStaticIntField(AppOpsManager, mode);
        XposedHelpers.callMethod(appOpsManager, MethodEnum.setMode, code, uid, packageName, state);
    }

    public static void setRunAnyInBackgroundIgnore(Object appOpsManager, int uid, String packageName, ClassLoader classLoader) {
        setMode(appOpsManager, OP_RUN_ANY_IN_BACKGROUND, uid, packageName, MODE_IGNORED, classLoader);
    }

    public static void setRunAnyInBackgroundDefault(Object appOpsManager, int uid, String packageName, ClassLoader classLoader) {
        setMode(appOpsManager, OP_RUN_ANY_IN_BACKGROUND, uid, packageName, MODE_DEFAULT, classLoader);
    }

}
