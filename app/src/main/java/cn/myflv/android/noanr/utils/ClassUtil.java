package cn.myflv.android.noanr.utils;

import cn.myflv.android.noanr.entity.ClassEnum;
import de.robv.android.xposed.XposedHelpers;

public class ClassUtil {
    public static Class<?> findAppGlobals(ClassLoader classLoader) {
        return XposedHelpers.findClass(ClassEnum.AppGlobals, classLoader);
    }
}
