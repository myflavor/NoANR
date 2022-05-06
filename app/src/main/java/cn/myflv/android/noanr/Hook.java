package cn.myflv.android.noanr;

import java.lang.reflect.Field;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Hook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("android")) {
            XposedBridge.log("NoANR Load success");
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
                            try {
                                Object anrHelper = param.thisObject;
                                Object processRecord = param.args[0];
                                if (processRecord == null) return null;
                                Field mErrorStateField = XposedHelpers.findField(processRecord.getClass(), "mErrorState");
                                Object mErrorState = mErrorStateField.get(processRecord);
                                if (mErrorState == null) return null;
                                XposedHelpers.callMethod(mErrorState, "setNotResponding", false);
                                Field mServiceField = XposedHelpers.findField(anrHelper.getClass(), "mService");
                                Object mService = mServiceField.get(anrHelper);
                                if (mService == null) return null;
                                Field mServicesField = XposedHelpers.findField(mService.getClass(), "mServices");
                                Object mServices = mServicesField.get(mService);
                                if (mServices == null) return null;
                                XposedHelpers.callMethod(mServices, "scheduleServiceTimeoutLocked", processRecord);
                                XposedBridge.log("NoANR Hook success");
                            } catch (Exception e) {
                                XposedBridge.log("NoANR " + e.getMessage());
                            }
                            return null;
                        }
                    });
        }
    }
}
