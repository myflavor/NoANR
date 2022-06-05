package cn.myflv.android.noanr;

import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.List;

import cn.myflv.android.noanr.entity.ClassEnum;
import cn.myflv.android.noanr.entity.MethodEnum;
import cn.myflv.android.noanr.utils.AnrUtil;
import cn.myflv.android.noanr.utils.AppOpsUtil;
import cn.myflv.android.noanr.utils.AppUtil;
import cn.myflv.android.noanr.utils.BroadcastUtil;
import cn.myflv.android.noanr.utils.ClassUtil;
import cn.myflv.android.noanr.utils.ProcessUtil;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Hook implements IXposedHookLoadPackage {

    private final static String NO_ANR = "NoANR";
    private final static boolean DEBUG = false;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        ClassLoader classLoader = loadPackageParam.classLoader;
        if (loadPackageParam.packageName.equals("android")) {
            XposedBridge.log(NO_ANR + " Load success");
            XposedHelpers.findAndHookMethod(ClassEnum.BroadcastQueue, classLoader, MethodEnum.deliverToRegisteredReceiverLocked,
                    ClassEnum.BroadcastRecord,
                    ClassEnum.BroadcastFilter, boolean.class, int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            Object broadcastQueue = param.thisObject;
                            Object[] args = param.args;
                            Object filter = args[1];
                            Object receiverList = BroadcastUtil.getReceiverList(filter);
                            if (receiverList == null) return;
                            Object applicationInfo = BroadcastUtil.getApplicationInfo(receiverList);
                            if (applicationInfo == null) return;
                            String packageName = AppUtil.getPackageName(applicationInfo);
                            if (packageName == null) return;
                            Object packageManager = AppUtil.getPackageManager(classLoader);
                            if (packageManager == null) return;
                            if (AppUtil.isSystem(packageManager, packageName)) return;
                            Object activityManagerService = BroadcastUtil.getActivityManagerService(broadcastQueue);
                            if (activityManagerService == null) return;
                            Object activeServices = AppUtil.getActiveServices(activityManagerService);
                            if (activeServices == null) return;
                            int uid = AppUtil.getUid(applicationInfo);
                            Object appOpsManager = AppUtil.getAppOpsManager(activityManagerService, classLoader);
                            if (appOpsManager == null) return;
                            boolean wakeLockIgnore = AppOpsUtil.checkOpIgnore(appOpsManager, AppOpsUtil.OP_WAKE_LOCK, uid, packageName, classLoader);
                            log(packageName + " -> " + wakeLockIgnore);
                            if (!wakeLockIgnore) return;
                            BroadcastUtil.clear(receiverList);
                            log("Broadcast to " + packageName + " clean success");
                        }

                    }
            );
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                XposedHelpers.findAndHookMethod(ClassEnum.AnrHelper, classLoader, MethodEnum.appNotResponding,
                        ClassEnum.ProcessRecord,
                        String.class,
                        ClassEnum.ApplicationInfo,
                        String.class,
                        ClassEnum.WindowProcessController,
                        boolean.class,
                        String.class, new XC_MethodReplacement() {
                            @Override
                            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                                Object[] args = param.args;
                                Object anrHelper = param.thisObject;
                                Object processRecord = args[0];
                                Object applicationInfo = ProcessUtil.getApplicationInfo(processRecord);
                                boolean isSystem = AppUtil.isSystem(applicationInfo);
                                if (isSystem) {
                                    synchronized (AnrUtil.mAnrRecords(anrHelper)) {
                                        Object anrRecord = AnrUtil.newInstance(classLoader, args);
                                        AnrUtil.mAnrRecords(anrHelper).add(anrRecord);
                                    }
                                    AnrUtil.startAnrConsumerIfNeeded(anrHelper);
                                } else {
                                    Object processName = XposedHelpers.getObjectField(processRecord, "processName");
                                    if (processName == null) processName = "unknown";
                                    log("Keep process " + processName + " success");
                                }
                                return null;
                            }
                        });
            } else {
                XposedHelpers.findAndHookMethod(ClassEnum.ProcessRecord, loadPackageParam.classLoader, MethodEnum.appNotResponding,
                        String.class, ClassEnum.ApplicationInfo, String.class, ClassEnum.WindowProcessController, Boolean.class, String.class, new XC_MethodReplacement() {
                            @Override
                            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                                return null;
                            }
                        });

            }

        }
    }


    public void log(String str) {
        if (DEBUG) XposedBridge.log(NO_ANR + " -> " + str);
    }


}
