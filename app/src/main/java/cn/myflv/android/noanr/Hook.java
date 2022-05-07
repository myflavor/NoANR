package cn.myflv.android.noanr;

import java.lang.reflect.Field;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Hook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("android")) {
            XposedBridge.log("NoANR Load success");
            XposedHelpers.findAndHookMethod("com.android.server.am.ActiveServices",lpparam.classLoader,"serviceTimeout","com.android.server.am.ProcessRecord",XC_MethodReplacement.DO_NOTHING);
            XposedHelpers.findAndHookMethod("com.android.server.am.ActiveServices",lpparam.classLoader,"serviceForegroundTimeout","com.android.server.am.ProcessRecord",XC_MethodReplacement.DO_NOTHING);

            XposedHelpers.findAndHookMethod("com.android.server.am.AnrHelper", lpparam.classLoader, "appNotResponding",
                    "com.android.server.am.ProcessRecord",
                    String.class,
                    "android.content.pm.ApplicationInfo",
                    String.class,
                    "com.android.server.wm.WindowProcessController",
                    boolean.class,
                    String.class, new XC_MethodReplacement() {

                        @SuppressWarnings("unchecked")
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            try {
                                Object anrHelper = param.thisObject;

                                Object[] args = param.args;
                                Object proc = param.args[0];

                                Field infoField = XposedHelpers.findField(proc.getClass(), "info");
                                Object applicationInfo = null;
                                if (args[2]!=null) applicationInfo=args[2];
                                if (applicationInfo==null && infoField.get(proc)!=null){
                                    applicationInfo = infoField.get(proc);
                                }
                                if (applicationInfo==null){
                                    Field mAnrRecordsField = XposedHelpers.findField(anrHelper.getClass(), "mAnrRecords");
                                    synchronized (mAnrRecordsField.get(anrHelper)){
                                        List<Object> mAnrRecords = (List<Object>) mAnrRecordsField.get(anrHelper);
                                        Class<?> AnrRecord = XposedHelpers.findClass("com.android.server.am.AnrHelper$AnrRecord", lpparam.classLoader);
                                        Object anrRecord = XposedHelpers.newInstance(AnrRecord, args);
                                        mAnrRecords.add(anrRecord);
                                    }
                                    XposedHelpers.callMethod(anrHelper,"startAnrConsumerIfNeeded");
                                    XposedBridge.log("NoANR Skip process");
                                    return null;
                                }
                                Field flagsField = XposedHelpers.findField(applicationInfo.getClass(), "flags");
                                int FLAG_SYSTEM = XposedHelpers.getStaticIntField(applicationInfo.getClass(), "FLAG_SYSTEM");
                                int flags = (int) flagsField.get(applicationInfo);
                                if ((flags & FLAG_SYSTEM) != 0) return null;
                                Field mErrorStateField = XposedHelpers.findField(proc.getClass(), "mErrorState");
                                Object errState = mErrorStateField.get(proc);
                                if (errState == null) return null;
                                Field mServiceField = XposedHelpers.findField(anrHelper.getClass(), "mService");
                                Object mService = mServiceField.get(anrHelper);
                                if (mService == null) return null;
                                Field mProcLockField = XposedHelpers.findField(mService.getClass(), "mProcLock");
                                if (mProcLockField.get(mService) == null) return null;
                                Object packageList;
                                synchronized (mProcLockField.get(mService)) {
                                    packageList = XposedHelpers.callMethod(proc, "getPackageListWithVersionCode");
                                    boolean isPersistent = (boolean) XposedHelpers.callMethod(proc, "isPersistent");
                                    if (!isPersistent) {
                                        XposedHelpers.callMethod(errState, "setNotResponding", false);
                                    }
                                }
                                if (packageList == null) return null;
                                Field mPackageWatchdogField = XposedHelpers.findField(mService.getClass(), "mPackageWatchdog");
                                Object mPackageWatchdog = mPackageWatchdogField.get(mService);
                                int FAILURE_REASON_APP_NOT_RESPONDING = XposedHelpers.getStaticIntField(mPackageWatchdog.getClass(), "FAILURE_REASON_APP_NOT_RESPONDING");
                                XposedHelpers.callMethod(mPackageWatchdog, "onPackageFailure", packageList, FAILURE_REASON_APP_NOT_RESPONDING);
                                XposedBridge.log("NoANR Hook success");
                            } catch (Exception e) {
                                XposedBridge.log("NoANR -> " + e.getMessage());
                            }
                            return null;
                        }
                    });
        }
    }
}
