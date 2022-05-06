package cn.myflv.android.noanr;

import android.util.Log;

import java.lang.reflect.Field;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Hook implements IXposedHookLoadPackage {
    private final static String NO_ANR = "NoANR";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("android")) {
            Log.i(NO_ANR, "Load success");
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
                            Log.i(NO_ANR, "Hook success");
                            Object anrHelper = param.thisObject;
                            Object processRecord = param.args[0];
                            Field mErrorStateField = XposedHelpers.findField(processRecord.getClass(), "mErrorState");
                            Object mErrorState = mErrorStateField.get(processRecord);
                            XposedHelpers.callMethod(mErrorState, "setNotResponding", boolean.class, false);
                            Field mServiceField = XposedHelpers.findField(anrHelper.getClass(), "mService");
                            Object mService = mServiceField.get(anrHelper);
                            Field mServicesField = XposedHelpers.findField(mService.getClass(), "mServices");
                            Object mServices = mServicesField.get(mService);
                            XposedHelpers.callMethod(mServices, "scheduleServiceTimeoutLocked", "com.android.server.am.ProcessRecord", processRecord);
                            return null;
                        }
                    });
        }
    }
}
