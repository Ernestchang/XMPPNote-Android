package cn.bingoogolapple.xmpp.ui.activity;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.nineoldandroids.animation.Animator;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import cn.bingoogolapple.androidcommon.adapter.BGARecyclerViewAdapter;
import cn.bingoogolapple.androidcommon.adapter.BGAViewHolderHelper;
import cn.bingoogolapple.titlebar.BGATitlebar;
import cn.bingoogolapple.xmpp.R;
import cn.bingoogolapple.xmpp.dao.SmsDao;
import cn.bingoogolapple.xmpp.model.MessageModel;
import cn.bingoogolapple.xmpp.provider.SmsProvider;
import cn.bingoogolapple.xmpp.service.IMService;
import cn.bingoogolapple.xmpp.util.Logger;
import cn.bingoogolapple.xmpp.util.ThreadUtil;

/**
 * 作者:王浩 邮件:bingoogolapple@gmail.com
 * 创建时间:15/9/5 下午3:35
 * 描述:
 */
public class ChatActivity extends BaseActivity {
    public static final String EXTRA_SESSION_ACCOUNT = "EXTRA_SESSION_ACCOUNT";
    public static final String EXTRA_SESSION_NICKNAME = "EXTRA_SESSION_NICKNAME";
    private LinearLayout mRootLl;
    private RecyclerView mDataRv;
    private EditText mMsgEt;
    private String mSessionAccount;
    private Chat mChat;
    private SmsDao mSmsDao;
    private ChatAdapter mChatAdapter;
    private SmsContentObserver mSmsContentObserver;
    private LinearLayoutManager mLinearLayoutManager;
    private TextView mNewmsgtipTv;

    @Override
    protected void initView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_chat);
        mRootLl = getViewById(R.id.ll_chat_root);
        mTitlebar = getViewById(R.id.titlebar);
        mDataRv = getViewById(R.id.rv_chat_data);
        mMsgEt = getViewById(R.id.et_chat_msg);
        mNewmsgtipTv = getViewById(R.id.tv_chat_newmsgtip);
    }

    @Override
    protected void setListener() {
        mTitlebar.setDelegate(new BGATitlebar.BGATitlebarDelegate() {
            @Override
            public void onClickLeftCtv() {
                onBackPressed();
            }
        });
        mMsgEt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    send();
                }
                return true;
            }
        });
        getViewById(R.id.tv_chat_send).setOnClickListener(this);
        mRootLl.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int heightDiff = mRootLl.getRootView().getHeight() - mRootLl.getHeight();
                if (heightDiff > 300) {
                    mDataRv.smoothScrollToPosition(mChatAdapter.getItemCount());
                }
            }
        });
    }

    @Override
    protected void processLogic(Bundle savedInstanceState) {
        registerSmsContentObserver();

        mTitlebar.setTitleText(String.format(getString(R.string.chat_title), getIntent().getStringExtra(EXTRA_SESSION_NICKNAME)));

        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mDataRv.setLayoutManager(mLinearLayoutManager);
        mChatAdapter = new ChatAdapter(mDataRv);
        mDataRv.setAdapter(mChatAdapter);

        mSessionAccount = getIntent().getStringExtra(EXTRA_SESSION_ACCOUNT);
        mChat = IMService.sConn.getChatManager().createChat(mSessionAccount, new ChatMessageListener());

        reloadData();
    }

    private void reloadData() {
        ThreadUtil.runInThread(new Runnable() {
            @Override
            public void run() {
                if (mSmsDao == null) {
                    mSmsDao = new SmsDao();
                }
                final List<MessageModel> messageModels = mSmsDao.getMessages();
                ThreadUtil.runInUIThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mLinearLayoutManager.findLastVisibleItemPosition() == mChatAdapter.getItemCount() - 1) {
//                            mChatAdapter.setDatas(messageModels);
                            mChatAdapter.addMoreDatas(messageModels.subList(mChatAdapter.getItemCount(), messageModels.size()));
                            mDataRv.smoothScrollToPosition(mChatAdapter.getItemCount());
                        } else {
//                            mChatAdapter.setDatas(messageModels);
                            mChatAdapter.addMoreDatas(messageModels.subList(mChatAdapter.getItemCount(), messageModels.size()));
                            showNewmsgtip();
                        }
                    }
                });
            }
        });
    }

    private void showNewmsgtip() {
        YoYo.with(Techniques.FadeInUp).duration(1500).withListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mNewmsgtipTv.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                hiddenNewmsgtip();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        }).playOn(mNewmsgtipTv);
    }

    private void hiddenNewmsgtip() {
        YoYo.with(Techniques.FadeOutDown).duration(1500).withListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mNewmsgtipTv.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        }).playOn(mNewmsgtipTv);
    }

    @Override
    public void onDestroy() {
        unregisterSmsContentObserver();
        super.onDestroy();
    }

    private void registerSmsContentObserver() {
        mSmsContentObserver = new SmsContentObserver(new Handler());
        getContentResolver().registerContentObserver(SmsProvider.URI_SMS, true, mSmsContentObserver);
    }

    private void unregisterSmsContentObserver() {
        getContentResolver().unregisterContentObserver(mSmsContentObserver);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv_chat_send) {
            send();
        }
    }

    private void send() {
        final String msg = mMsgEt.getText().toString().trim();
        if (!TextUtils.isEmpty(msg)) {
            ThreadUtil.runInThread(new Runnable() {
                @Override
                public void run() {
                    Message message = new Message();
                    message.setFrom(IMService.sAccount);
                    message.setTo(mSessionAccount);
                    message.setBody(msg);
                    message.setType(Message.Type.chat);
                    try {
                        mChat.sendMessage(message);

                        mSmsDao.saveMessage(message, mSessionAccount);
                    } catch (XMPPException e) {
                        e.printStackTrace();
                    }

                    ThreadUtil.runInUIThread(new Runnable() {
                        @Override
                        public void run() {
                            mMsgEt.setText("");
                        }
                    });
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        backward();
    }

    private final class ChatMessageListener implements MessageListener {

        @Override
        public void processMessage(Chat chat, Message message) {
            Logger.i(TAG, "消息=" + message.getBody() + " 类型=" + message.getType().name());
            if (message.getType() == Message.Type.chat && !TextUtils.isEmpty(message.getBody())) {
                mSmsDao.saveMessage(message, chat.getParticipant());
            }
        }
    }

    private final class SmsContentObserver extends ContentObserver {

        public SmsContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            reloadData();
        }
    }

    private static final class ChatAdapter extends BGARecyclerViewAdapter<MessageModel> {
        private SimpleDateFormat mSimpleDateFormat;

        public ChatAdapter(RecyclerView recyclerView) {
            super(recyclerView, R.layout.item_chat);
            mSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }

        @Override
        public void setItemChildListener(BGAViewHolderHelper viewHolderHelper) {
        }

        @Override
        public void fillData(BGAViewHolderHelper viewHolderHelper, int position, MessageModel model) {
            viewHolderHelper.setText(R.id.tv_item_chat_time, mSimpleDateFormat.format(new Date(Long.parseLong(model.time))));
            if (model.fromAccount.contains(model.sessionAccount)) {
                viewHolderHelper.setVisibility(R.id.rl_item_chat_me, View.GONE);
                viewHolderHelper.setVisibility(R.id.rl_item_chat_other, View.VISIBLE);
                viewHolderHelper.setText(R.id.tv_item_chat_other_msg, model.body);
            } else {
                viewHolderHelper.setVisibility(R.id.rl_item_chat_other, View.GONE);
                viewHolderHelper.setVisibility(R.id.rl_item_chat_me, View.VISIBLE);
                viewHolderHelper.setText(R.id.tv_item_chat_me_msg, model.body);
            }
        }

    }
}