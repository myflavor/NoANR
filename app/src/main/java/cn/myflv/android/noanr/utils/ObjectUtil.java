package cn.myflv.android.noanr.utils;

import java.util.List;

public class ObjectUtil {
    public static List<?> toList(Object object) {
        if (object == null) return null;
        return (List<?>) object;
    }


    public static List<Object> toObjList(Object object) {
        if (object == null) return null;
        return (List<Object>) object;
    }
}
