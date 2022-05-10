package cn.myflv.android.noanr;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Hook implements IXposedHookLoadPackage {

    private final static String NO_ANR = "NoANR";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("android")) {
            log("Load success");
//            XposedHelpers.findAndHookMethod("com.android.server.am.BroadcastQueue", lpparam.classLoader,
//                    "broadcastTimeoutLocked",
//                    boolean.class, new XC_MethodHook() {
//                        @Override
//                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                            super.beforeHookedMethod(param);
//                            try {
//                                Object broadcastQueue = param.thisObject;
//                                Object mService = XposedHelpers.getObjectField(broadcastQueue, "mService");
//                                Object mDispatcher = XposedHelpers.getObjectField(broadcastQueue, "mDispatcher");
//                                Object broadcastRecords = XposedHelpers.callMethod(mDispatcher, "getActiveBroadcastLocked");
//                                Class<?> ResolveInfo = XposedHelpers.findClass("android.content.pm.ResolveInfo", lpparam.classLoader);
//                                Class<?> BroadcastFilter = XposedHelpers.findClass("com.android.server.am.BroadcastFilter", lpparam.classLoader);
//                                remove(mService, broadcastRecords, ResolveInfo, BroadcastFilter);
//                            } catch (Exception e) {
//                                log("Exception clean broadcast " + e.getMessage());
//                            }
//                        }
//
//
//                    });
//            XposedHelpers.findAndHookMethod("com.android.server.am.AnrHelper", lpparam.classLoader, "appNotResponding",
//                    "com.android.server.am.ProcessRecord",
//                    String.class,
//                    "android.content.pm.ApplicationInfo",
//                    String.class,
//                    "com.android.server.wm.WindowProcessController",
//                    boolean.class,
//                    String.class, new XC_MethodReplacement() {
//                        @Override
//                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
//                            log("Hook success");
//                            return null;
//                        }
//                    });
            XposedHelpers.findAndHookMethod("com.android.server.am.BroadcastQueue", lpparam.classLoader,
                    "processNextBroadcastLocked",
                    boolean.class, boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            try {
                                Object broadcastQueue = param.thisObject;
                                Object mService = XposedHelpers.getObjectField(broadcastQueue, "mService");
                                if (mService == null) return;
                                Object mServices = XposedHelpers.getObjectField(mService, "mServices");
                                if (mServices == null) return;
                                Class<?> ResolveInfo = XposedHelpers.findClass("android.content.pm.ResolveInfo", lpparam.classLoader);
                                Class<?> BroadcastFilter = XposedHelpers.findClass("com.android.server.am.BroadcastFilter", lpparam.classLoader);
                                Class<?> AppGlobals = XposedHelpers.findClass("android.app.AppGlobals", lpparam.classLoader);
                                Object packageManager = XposedHelpers.callStaticMethod(AppGlobals, "getPackageManager");
                                if (packageManager == null) {
                                    log("packageManager is null");
                                }
                                Object mParallelBroadcasts = XposedHelpers.getObjectField(broadcastQueue, "mParallelBroadcasts");
                                removeList(mService, mParallelBroadcasts, ResolveInfo, BroadcastFilter, packageManager, mServices);
                                Object mPendingBroadcast = XposedHelpers.getObjectField(broadcastQueue, "mPendingBroadcast");
                                remove(mService, mPendingBroadcast, ResolveInfo, BroadcastFilter, packageManager, mServices);
                                Object mDispatcher = XposedHelpers.getObjectField(broadcastQueue, "mDispatcher");
                                if (mDispatcher == null) return;
                                Object mOrderedBroadcasts = XposedHelpers.getObjectField(mDispatcher, "mOrderedBroadcasts");
                                removeList(mService, mOrderedBroadcasts, ResolveInfo, BroadcastFilter, packageManager, mServices);
                                Object mAlarmBroadcasts = XposedHelpers.getObjectField(mDispatcher, "mAlarmBroadcasts");
                                removeDeferralList(mService, mAlarmBroadcasts, ResolveInfo, BroadcastFilter, packageManager, mServices);
                                Object mDeferredBroadcasts = XposedHelpers.getObjectField(mDispatcher, "mDeferredBroadcasts");
                                removeDeferralList(mService, mDeferredBroadcasts, ResolveInfo, BroadcastFilter, packageManager, mServices);
                                Object mCurrentBroadcast = XposedHelpers.getObjectField(mDispatcher, "mCurrentBroadcast");
                                remove(mService, mCurrentBroadcast, ResolveInfo, BroadcastFilter, packageManager, mServices);
                            } catch (Exception e) {
                                log("Exception clean broadcast " + e.getMessage());
                            }
                        }

                    });

        }
    }


    public boolean isSystem(Object packageManager, Object packageName) {
        final int MATCH_ALL = 0x00020000;
        if (packageName == null) return false;
        Object applicationInfo = XposedHelpers.callMethod(packageManager, "getApplicationInfo", packageName, MATCH_ALL, 0);
        int FLAG_SYSTEM = XposedHelpers.getStaticIntField(applicationInfo.getClass(), "FLAG_SYSTEM");
        int FLAG_UPDATED_SYSTEM_APP = XposedHelpers.getStaticIntField(applicationInfo.getClass(), "FLAG_UPDATED_SYSTEM_APP");
        int flags = XposedHelpers.getIntField(applicationInfo, "flags");
        return (flags & (FLAG_SYSTEM | FLAG_UPDATED_SYSTEM_APP)) != 0;
    }

    public List<?> toList(Object object) {
        if (object == null) return null;
        return (List<?>) object;
    }


    public void removeDeferralList(Object activityManagerService, Object deferrals, Class<?> ResolveInfo, Class<?> BroadcastFilter, Object packageManager, Object activeServices) {
        List<?> deferralList = toList(deferrals);
        if (deferralList != null && deferralList.size() > 0) {
            for (Object deferral : deferralList) {
                Object broadcastRecords = XposedHelpers.getObjectField(deferral, "broadcasts");
                removeList(activityManagerService, broadcastRecords, ResolveInfo, BroadcastFilter, packageManager, activeServices);
            }
        }
    }

    public void removeList(Object activityManagerService, Object broadcastRecords, Class<?> ResolveInfo, Class<?> BroadcastFilter, Object packageManager, Object activeServices) {
        List<?> broadcastRecordList = toList(broadcastRecords);
        if (broadcastRecordList != null && broadcastRecordList.size() > 0) {
            for (Object broadcastRecord : broadcastRecordList) {
                remove(activityManagerService, broadcastRecord, ResolveInfo, BroadcastFilter, packageManager, activeServices);
            }
        }
    }


    public void remove(Object activityManagerService, Object broadcastRecord, Class<?> ResolveInfo, Class<?> BroadcastFilter, Object packageManager, Object activeServices) {
        try {
            if (broadcastRecord == null) return;
            Object receiversList = XposedHelpers.getObjectField(broadcastRecord, "receivers");
            if (receiversList == null) return;
            List<?> receivers = toList(receiversList);
            if (receivers != null && receivers.size() > 0) {
                int nextReceiver = (int) XposedHelpers.getObjectField(broadcastRecord, "nextReceiver");
                Object receiver;
                for (int i = receivers.size() - 1; i >= 0; i--) {
                    String packageName = null;
                    Integer uid = null;
                    boolean isRun = false;
                    receiver = receivers.get(i);
                    if (BroadcastFilter.isAssignableFrom(receiver.getClass())) {
                        isRun = true;
                        Object broadcastFilter = receiver;
                        Object pkg = XposedHelpers.getObjectField(broadcastFilter, "packageName");
                        if (packageName == null) packageName = (String) pkg;
                        uid = XposedHelpers.getIntField(broadcastFilter, "owningUid");
                        if (isSystem(packageManager, packageName)) continue;
                    } else if (ResolveInfo.isAssignableFrom(receiver.getClass())) {
                        // log("receiver is not instance of ResolveInfo");
                        Object resolveInfo = receiver;
                        Object activityInfo = XposedHelpers.getObjectField(resolveInfo, "activityInfo");
                        if (activityInfo == null) continue;
                        Object targetProcess = XposedHelpers.getObjectField(activityInfo, "processName");
                        if (targetProcess == null) continue;
                        Object applicationInfo = XposedHelpers.getObjectField(activityInfo, "applicationInfo");
                        if (applicationInfo == null) continue;
                        Object pkg = XposedHelpers.getObjectField(applicationInfo, "packageName");
                        if (packageName == null) packageName = (String) pkg;
                        int FLAG_SYSTEM = XposedHelpers.getStaticIntField(applicationInfo.getClass(), "FLAG_SYSTEM");
                        int FLAG_UPDATED_SYSTEM_APP = XposedHelpers.getStaticIntField(applicationInfo.getClass(), "FLAG_UPDATED_SYSTEM_APP");
                        int flags = XposedHelpers.getIntField(applicationInfo, "flags");
                        if ((flags & (FLAG_SYSTEM | FLAG_UPDATED_SYSTEM_APP)) != 0) {
//                                            log("Skip system app " + targetProcess);
                            continue;
                        }
                        uid = XposedHelpers.getIntField(applicationInfo, "uid");
                        Object app = XposedHelpers.callMethod(activityManagerService, "getProcessRecordLocked", targetProcess, uid);
                        if (app != null) isRun = true;
                    }
                    if (packageName == null || uid == null || !isRun) continue;
                    final int finalUid = uid;
                    final String finalPackageName = packageName;
                    boolean appRestrictedAnyInBackground = (boolean) XposedHelpers.callMethod(activeServices, "appRestrictedAnyInBackground", finalUid, finalPackageName);
                    boolean isOnDeviceIdleAllowlistLOSP = (boolean) XposedHelpers.callMethod(activityManagerService, "isOnDeviceIdleAllowlistLOSP", uid, false);
                    if (!isOnDeviceIdleAllowlistLOSP && appRestrictedAnyInBackground) {
                        receivers.remove(i);
                        if (i < nextReceiver) {
                            nextReceiver--;
                        }
                        log("Clean " + packageName + " broadcast success");
                        nextReceiver = Math.min(nextReceiver, receivers.size());
                    }
                }
            }
        } catch (Exception e) {
            log("Exception remove broadcast " + e.getMessage());
        }

    }


    public void log(String str) {
        XposedBridge.log(NO_ANR + " -> " + str);
    }

}
