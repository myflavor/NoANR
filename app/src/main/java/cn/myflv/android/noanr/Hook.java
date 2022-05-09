package cn.myflv.android.noanr;

import android.util.Log;

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
            XposedHelpers.findAndHookMethod("com.android.server.am.AnrHelper", lpparam.classLoader, "appNotResponding",
                    "com.android.server.am.ProcessRecord",
                    String.class,
                    "android.content.pm.ApplicationInfo",
                    String.class,
                    "com.android.server.wm.WindowProcessController",
                    boolean.class,
                    String.class, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            log("Hook success");
                            return null;
                        }
                    });
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
                                Object mServices = XposedHelpers.getObjectField(mService, "mServices");
                                Object mParallelBroadcasts = XposedHelpers.getObjectField(broadcastQueue, "mParallelBroadcasts");
                                removeList(mService, mServices, mParallelBroadcasts);
                                Object mDispatcher = XposedHelpers.getObjectField(broadcastQueue, "mDispatcher");
                                Object mOrderedBroadcasts = XposedHelpers.getObjectField(mDispatcher, "mOrderedBroadcasts");
                                removeList(mService, mServices, mOrderedBroadcasts);
                                Object mAlarmBroadcasts = XposedHelpers.getObjectField(mDispatcher, "mAlarmBroadcasts");
                                removeList(mService, mServices, mAlarmBroadcasts);
                                Object mDeferredBroadcasts = XposedHelpers.getObjectField(mDispatcher, "mDeferredBroadcasts");
                                removeList(mService, mServices, mDeferredBroadcasts);
                                Object mCurrentBroadcast = XposedHelpers.getObjectField(mDispatcher, "mCurrentBroadcast");
                                remove(mService, mServices, mCurrentBroadcast);
                            } catch (Exception e) {
                                log("Exception clean broadcast " + e.getMessage());
                                }
                            }

                        public List<?> toList(Object object) {
                            if (object == null) return null;
                            return (List<?>) object;
                        }

                        public void removeList(Object activityManagerService, Object activeServices, Object broadcastRecords) {
                            List<?> broadcastRecordList = toList(broadcastRecords);
                            if (broadcastRecordList != null && broadcastRecordList.size() > 0) {
                                for (Object broadcastRecord : broadcastRecordList) {
                                    remove(activityManagerService, activeServices, broadcastRecord);
                                }
                            }
                        }

                        public void remove(Object activityManagerService, Object activeServices, Object broadcastRecord) {
                            List<?> receivers = toList(XposedHelpers.getObjectField(broadcastRecord, "receivers"));
                            if (receivers != null && receivers.size() > 0) {
                                for (Object receiver : receivers) {
                                    Class<?> ResolveInfo = XposedHelpers.findClass("android.content.pm.ResolveInfo", lpparam.classLoader);
                                    if (!ResolveInfo.isAssignableFrom(receiver.getClass())) {
                                        log("receiver is not instance of ResolveInfo");
                                        continue;
                                    }
                                    Object resolveInfo = receiver;
                                    Object activityInfo = XposedHelpers.getObjectField(resolveInfo, "activityInfo");
                                    Object targetProcess = XposedHelpers.getObjectField(activityInfo, "processName");
                                    Object applicationInfo = XposedHelpers.getObjectField(activityInfo, "applicationInfo");
                                    int uid = XposedHelpers.getIntField(applicationInfo, "uid");
                                    final int finalUid = uid;
                                    final String finalPackageName = (String) targetProcess;
                                    Object app = XposedHelpers.callMethod(activityManagerService, "getProcessRecordLocked", targetProcess, uid);
                                    boolean appRestrictedAnyInBackground = (boolean) XposedHelpers.callMethod(activeServices, "appRestrictedAnyInBackground", finalUid, finalPackageName);
                                    if (app != null && appRestrictedAnyInBackground) {
                                        boolean cleanupDisabledPackageReceiversLocked = (boolean) XposedHelpers.callMethod(broadcastRecord, "cleanupDisabledPackageReceiversLocked", targetProcess, null, -1, true);
                                        if (!cleanupDisabledPackageReceiversLocked) {
                                            log("Clean " + targetProcess + " broadcast failed");
                                        } else {
                                            log("Clean " + targetProcess + " broadcast success");
                                        }
                                        continue;
                                    }
                                    if (app == null) {
                                        log(targetProcess + " is not running");
                                    }
                                    if (app != null && !appRestrictedAnyInBackground) {
                                        log(targetProcess + " is not restricted");
                                    }
                                }
                            }
                        }
                    });

        }
    }

    public void log(String str) {
        XposedBridge.log(NO_ANR + " -> " + str);
    }


}
