package cn.bingoogolapple.xmpp.util;

import android.os.Handler;

/**
 * 作者:王浩 邮件:bingoogolapple@gmail.com
 * 创建时间:15/9/2 下午4:39
 * 描述:
 */
public class ThreadUtil {
    private static Handler sHandler = new Handler();

    private ThreadUtil() {
    }

    public static void runInThread(Runnable task) {
        new Thread(task).start();
    }

    public static void runInUIThread(Runnable task) {
        sHandler.post(task);
    }

    public static void runInUIThread(Runnable task, long delayMillis) {
        sHandler.postDelayed(task, delayMillis);
    }

}