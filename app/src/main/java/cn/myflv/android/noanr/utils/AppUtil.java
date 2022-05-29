package cn.myflv.android.noanr.utils;

import cn.myflv.android.noanr.entity.FieldEnum;
import cn.myflv.android.noanr.entity.MethodEnum;
import de.robv.android.xposed.XposedHelpers;

public class AppUtil {

    public final static String Event = "android.app.usage.UsageEvents.Event";
    public final static String ACTIVITY_RESUMED = "ACTIVITY_RESUMED";
    public final static String ACTIVITY_PAUSED = "ACTIVITY_PAUSED";


    public static boolean isSystem(Object applicationInfo) {
        int FLAG_SYSTEM = XposedHelpers.getStaticIntField(applicationInfo.getClass(), "FLAG_SYSTEM");
        int FLAG_UPDATED_SYSTEM_APP = XposedHelpers.getStaticIntField(applicationInfo.getClass(), "FLAG_UPDATED_SYSTEM_APP");
        int flags = XposedHelpers.getIntField(applicationInfo, FieldEnum.flags);
        return (flags & (FLAG_SYSTEM | FLAG_UPDATED_SYSTEM_APP)) != 0;
    }


    public static boolean isSystem(Object packageManager, Object packageName) {
        final int MATCH_ALL = 0x00020000;
        if (packageName == null) return false;
        Object applicationInfo = XposedHelpers.callMethod(packageManager, MethodEnum.getApplicationInfo, packageName, MATCH_ALL, 0);
        int FLAG_SYSTEM = XposedHelpers.getStaticIntField(applicationInfo.getClass(), "FLAG_SYSTEM");
        int FLAG_UPDATED_SYSTEM_APP = XposedHelpers.getStaticIntField(applicationInfo.getClass(), "FLAG_UPDATED_SYSTEM_APP");
        int flags = XposedHelpers.getIntField(applicationInfo, "flags");
        return (flags & (FLAG_SYSTEM | FLAG_UPDATED_SYSTEM_APP)) != 0;
    }


    public static Class<?> getEvent(ClassLoader classLoader) {
        return XposedHelpers.findClass(Event, classLoader);
    }


    public static Object getAppOpsManager(Object activityManagerService) {
        return XposedHelpers.callMethod(activityManagerService, MethodEnum.getAppOpsManager);
    }

    public static int ACTIVITY_RESUMED(ClassLoader classLoader) {
        Class<?> Event = getEvent(classLoader);
        return XposedHelpers.getStaticIntField(Event, ACTIVITY_RESUMED);
    }

    public static int ACTIVITY_PAUSED(ClassLoader classLoader) {
        Class<?> Event = getEvent(classLoader);
        return XposedHelpers.getStaticIntField(Event, ACTIVITY_PAUSED);
    }

    public static String getPackageName(Object applicationInfo) {
        return (String) XposedHelpers.getObjectField(applicationInfo, FieldEnum.packageName);
    }

    public static Object getPackageManager(ClassLoader classLoader) {
        Class<?> AppGlobals = ClassUtil.findAppGlobals(classLoader);
        return XposedHelpers.callStaticMethod(AppGlobals, MethodEnum.getPackageManager);
    }

    public static Object getActiveServices(Object activityManagerService) {
        return XposedHelpers.getObjectField(activityManagerService, FieldEnum.mServices);
    }

    public static boolean isAppRestrictedAnyInBackground(Object activeServices, int uid, String packageName) {
        return (boolean) XposedHelpers.callMethod(activeServices, MethodEnum.appRestrictedAnyInBackground, uid, packageName);

    }

    public static int getUid(Object applicationInfo) {
        return XposedHelpers.getIntField(applicationInfo, FieldEnum.uid);
    }
}
