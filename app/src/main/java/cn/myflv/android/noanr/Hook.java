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
            XposedHelpers.findAndHookConstructor("com.android.server.am.BroadcastRecord", lpparam.classLoader,
                    "com.android.server.am.BroadcastQueue", "android.content.Intent", "com.android.server.am.ProcessRecord", String.class,
                    String.class, int.class, int.class,
                    boolean.class, String.class,
                    String[].class, String[].class, int.class,
                    "android.app.BroadcastOptions", List.class, "android.content.IIntentReceiver", int.class,
                    String.class, "android.os.Bundle", boolean.class, boolean.class,
                    boolean.class, int.class, boolean.class,
                    "android.os.IBinder", boolean.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            Object broadcastRecord = param.thisObject;

                            Class<?> LocalServices = XposedHelpers.findClass("com.android.server.LocalServices", lpparam.classLoader);
                            Class<?> ActivityManagerService = XposedHelpers.findClass("com.android.server.am.ActivityManagerService", lpparam.classLoader);
                            Class<?> ActiveServices = XposedHelpers.findClass("com.android.server.am.ActiveServices ", lpparam.classLoader);
                            Object activityManagerService = XposedHelpers.callStaticMethod(LocalServices, "getService", ActivityManagerService);
                            if (activityManagerService==null) return;
                            Object activeServices = XposedHelpers.callStaticMethod(LocalServices, "getService", ActiveServices);
                            if (activeServices==null) return;
                            remove(activityManagerService,activeServices,broadcastRecord);
                        }
                        public List<?> toList(Object object) {
                            if (object == null) return null;
                            return (List<?>) object;
                        }

                        public void remove(Object activityManagerService, Object activeServices, Object broadcastRecord) {
                            try {
                                if (broadcastRecord == null) return;
                                Object receiversList = XposedHelpers.getObjectField(broadcastRecord, "receivers");
                                if (receiversList == null) return;
                                List<?> receivers = toList(receiversList);
                                if (receivers != null && receivers.size() > 0) {
                                    for (Object receiver : receivers) {
                                        if (receiver == null) continue;
                                        Class<?> ResolveInfo = XposedHelpers.findClass("android.content.pm.ResolveInfo", lpparam.classLoader);
                                        if (!ResolveInfo.isAssignableFrom(receiver.getClass())) {
                                            // log("receiver is not instance of ResolveInfo");
                                            continue;
                                        }
                                        Object resolveInfo = receiver;
                                        Object activityInfo = XposedHelpers.getObjectField(resolveInfo, "activityInfo");
                                        if (activityInfo == null) continue;
                                        Object targetProcess = XposedHelpers.getObjectField(activityInfo, "processName");
                                        if (targetProcess == null) continue;
                                        Object applicationInfo = XposedHelpers.getObjectField(activityInfo, "applicationInfo");
                                        if (applicationInfo == null) continue;
                                        Object packageName = XposedHelpers.getObjectField(applicationInfo, "packageName");
                                        if (packageName == null) continue;
                                        int FLAG_SYSTEM = XposedHelpers.getStaticIntField(applicationInfo.getClass(), "FLAG_SYSTEM");
                                        int FLAG_UPDATED_SYSTEM_APP = XposedHelpers.getStaticIntField(applicationInfo.getClass(), "FLAG_UPDATED_SYSTEM_APP");
                                        int flags = XposedHelpers.getIntField(applicationInfo, "flags");
                                        if ((flags & (FLAG_SYSTEM | FLAG_UPDATED_SYSTEM_APP)) != 0) {
//                                            log("Skip system app " + targetProcess);
                                            continue;
                                        }
                                        int uid = XposedHelpers.getIntField(applicationInfo, "uid");
                                        boolean isOnDeviceIdleAllowlistLOSP = (boolean) XposedHelpers.callMethod(activityManagerService, "isOnDeviceIdleAllowlistLOSP", uid, false);
//                                        final int finalUid = uid;
//                                        final String finalPackageName = (String) targetProcess;
                                        Object app = XposedHelpers.callMethod(activityManagerService, "getProcessRecordLocked", targetProcess, uid);
//                                        boolean appRestrictedAnyInBackground = (boolean) XposedHelpers.callMethod(activeServices, "appRestrictedAnyInBackground", finalUid, finalPackageName);
                                        if (app != null && !isOnDeviceIdleAllowlistLOSP) {
                                            boolean cleanupDisabledPackageReceiversLocked = (boolean) XposedHelpers.callMethod(broadcastRecord, "cleanupDisabledPackageReceiversLocked", packageName, null, 0, true);
                                            if (!cleanupDisabledPackageReceiversLocked) {
                                                log("Clean " + targetProcess + " broadcast failed");
                                            } else {
                                                log("Clean " + targetProcess + " broadcast success");
                                            }
//                                            continue;
                                        }
//                                        if (app != null && isOnDeviceIdleAllowlistLOSP) {
//                                            log(targetProcess + " is on whiteList");
//                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log("Exception remove broadcast " + e.getMessage());
                            }

                        }


                    });
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
                                Object mParallelBroadcasts = XposedHelpers.getObjectField(broadcastQueue, "mParallelBroadcasts");
                                removeList(mService, mServices, mParallelBroadcasts);
                                Object mPendingBroadcast = XposedHelpers.getObjectField(broadcastQueue, "mPendingBroadcast");
                                remove(mService, mServices, mPendingBroadcast);
                                Object mDispatcher = XposedHelpers.getObjectField(broadcastQueue, "mDispatcher");
                                if (mDispatcher == null) return;
                                Object mOrderedBroadcasts = XposedHelpers.getObjectField(mDispatcher, "mOrderedBroadcasts");
                                removeList(mService, mServices, mOrderedBroadcasts);
                                Object mAlarmBroadcasts = XposedHelpers.getObjectField(mDispatcher, "mAlarmBroadcasts");
                                removeDeferralList(mService, mServices, mAlarmBroadcasts);
                                Object mDeferredBroadcasts = XposedHelpers.getObjectField(mDispatcher, "mDeferredBroadcasts");
                                removeDeferralList(mService, mServices, mDeferredBroadcasts);
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


                        public void removeDeferralList(Object activityManagerService, Object activeServices, Object deferrals) {
                            List<?> deferralList = toList(deferrals);
                            if (deferralList != null && deferralList.size() > 0) {
                                for (Object deferral : deferralList) {
                                    Object broadcastRecords = XposedHelpers.getObjectField(deferral, "broadcasts");
                                    removeList(activityManagerService, activeServices, broadcastRecords);
                                }
                            }
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
                            try {
                                if (broadcastRecord == null) return;
                                Object receiversList = XposedHelpers.getObjectField(broadcastRecord, "receivers");
                                if (receiversList == null) return;
                                List<?> receivers = toList(receiversList);
                                if (receivers != null && receivers.size() > 0) {
                                    for (Object receiver : receivers) {
                                        if (receiver == null) continue;
                                        Class<?> ResolveInfo = XposedHelpers.findClass("android.content.pm.ResolveInfo", lpparam.classLoader);
                                        if (!ResolveInfo.isAssignableFrom(receiver.getClass())) {
                                            // log("receiver is not instance of ResolveInfo");
                                            continue;
                                        }
                                        Object resolveInfo = receiver;
                                        Object activityInfo = XposedHelpers.getObjectField(resolveInfo, "activityInfo");
                                        if (activityInfo == null) continue;
                                        Object targetProcess = XposedHelpers.getObjectField(activityInfo, "processName");
                                        if (targetProcess == null) continue;
                                        Object applicationInfo = XposedHelpers.getObjectField(activityInfo, "applicationInfo");
                                        if (applicationInfo == null) continue;
                                        Object packageName = XposedHelpers.getObjectField(applicationInfo, "packageName");
                                        if (packageName == null) continue;
                                        int FLAG_SYSTEM = XposedHelpers.getStaticIntField(applicationInfo.getClass(), "FLAG_SYSTEM");
                                        int FLAG_UPDATED_SYSTEM_APP = XposedHelpers.getStaticIntField(applicationInfo.getClass(), "FLAG_UPDATED_SYSTEM_APP");
                                        int flags = XposedHelpers.getIntField(applicationInfo, "flags");
                                        if ((flags & (FLAG_SYSTEM | FLAG_UPDATED_SYSTEM_APP)) != 0) {
//                                            log("Skip system app " + targetProcess);
                                            continue;
                                        }
                                        int uid = XposedHelpers.getIntField(applicationInfo, "uid");
                                        boolean isOnDeviceIdleAllowlistLOSP = (boolean) XposedHelpers.callMethod(activityManagerService, "isOnDeviceIdleAllowlistLOSP", uid, false);
//                                        final int finalUid = uid;
//                                        final String finalPackageName = (String) targetProcess;
                                        Object app = XposedHelpers.callMethod(activityManagerService, "getProcessRecordLocked", targetProcess, uid);
//                                        boolean appRestrictedAnyInBackground = (boolean) XposedHelpers.callMethod(activeServices, "appRestrictedAnyInBackground", finalUid, finalPackageName);
                                        if (app != null && !isOnDeviceIdleAllowlistLOSP) {
                                            boolean cleanupDisabledPackageReceiversLocked = (boolean) XposedHelpers.callMethod(broadcastRecord, "cleanupDisabledPackageReceiversLocked", packageName, null, 0, true);
                                            if (!cleanupDisabledPackageReceiversLocked) {
                                                log("Clean " + targetProcess + " broadcast failed");
                                            } else {
                                                log("Clean " + targetProcess + " broadcast success");
                                            }
//                                            continue;
                                        }
//                                        if (app != null && isOnDeviceIdleAllowlistLOSP) {
//                                            log(targetProcess + " is on whiteList");
//                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log("Exception remove broadcast " + e.getMessage());
                            }

                        }
                    });

        }
    }

    public void log(String str) {
        XposedBridge.log(NO_ANR + " -> " + str);
    }

}
