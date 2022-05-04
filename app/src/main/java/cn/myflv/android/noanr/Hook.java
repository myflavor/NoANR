package cn.myflv.android.noanr;

import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Hook implements IXposedHookLoadPackage {
    private final static String NO_ANR = "NoANR";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("android")) {
            Log.i(NO_ANR, "NoANR load success");
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
                            Log.i(NO_ANR, "NoANR hook success");
                            return null;
                        }
                    });
        }
    }
}
