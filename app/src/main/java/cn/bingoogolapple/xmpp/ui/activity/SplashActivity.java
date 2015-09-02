package cn.bingoogolapple.xmpp.ui.activity;

import android.os.Bundle;
import android.widget.TextView;

import cn.bingoogolapple.xmpp.R;
import cn.bingoogolapple.xmpp.util.ThreadUtil;

/**
 * 作者:王浩 邮件:bingoogolapple@gmail.com
 * 创建时间:15/9/2 下午4:14
 * 描述:
 */
public class SplashActivity extends BaseActivity {

    @Override
    protected void initView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_splash);
    }

    @Override
    protected void setListener() {
    }

    @Override
    protected void processLogic(Bundle savedInstanceState) {
        TextView versionTv = getViewById(R.id.tv_splash_version);
        versionTv.setText("V" + mApp.getCurrentVersionName());

        ThreadUtil.runInUIThread(new Runnable() {
            @Override
            public void run() {
                forwardAndFinish(LoginActivity.class);
            }
        }, 1500);
    }
}