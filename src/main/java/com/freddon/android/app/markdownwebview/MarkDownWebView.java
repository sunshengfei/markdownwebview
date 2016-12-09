package com.freddon.android.app.markdownwebview;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.Locale;

/**
 * Created by fred on 2016/11/17.
 */
public class MarkDownWebView extends WebView {

    public static final String HTML_URL = "file:///android_asset/html/index.html";

    /**
     * 该`句柄`可通过url传递或者js传递实现动态配置
     * 本例不涉及
     */
    private static final String APPJSNAME = "freddon";//对应到网页js中的回调window自定义子对象的键

    /**
     * 由于JAVA和JS采用的不同标准进行encode和decode，
     * 导致换行符传递过程中解析不正确，继而转markdown会出现错乱，
     * 故先替换掉换行符，在js中渲染之前再替换回去
     */
    private final static String SWAP_BREAK_TAG = "<freddon>";

    private String content;

    /**
     * 分段通知到js
     */
    private final static int SEND_LENGTH_UNIT = 500;

    public MarkDownWebView(Context context) {
        this(context, null);
    }

    public MarkDownWebView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MarkDownWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (!isInEditMode()) {
            initialize();
        }
    }

    private void initialize() {
        setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (!TextUtils.isEmpty(url)) {
                    if (url.startsWith("tel:")) {
                        Intent intent = new Intent(Intent.ACTION_DIAL, Uri
                                .parse(url));
                        view.getContext().startActivity(intent);
                        return true;
                    } else {
                        Intent intent = new Intent();
                        intent.setAction("android.intent.action.VIEW");
                        Uri content_url = Uri.parse(url);
                        intent.setData(content_url);
                        view.getContext().startActivity(intent);
                        return true;
                    }
                }
                return super.shouldOverrideUrlLoading(view, url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (onMarkDownEvent != null)
                    onMarkDownEvent.onPageFinished(MarkDownWebView.this);
            }
        });
        setWebChromeClient(new WebChromeClient() {

        });
        WebSettings webSetting = this.getSettings();
        webSetting.setDisplayZoomControls(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //解决issues # android.view.ThreadedRenderer.finalize() timed out after 10 seconds 或 设置android:hardwareAccelerated="true"
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            if (0 != (getContext().getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE)) {
                setWebContentsDebuggingEnabled(true);
            }
        }
        webSetting.setSupportZoom(true);
//        ws.setDefaultZoom(ZoomDensity.CLOSE);
        //设置 缓存模式
        webSetting.setCacheMode(WebSettings.LOAD_DEFAULT);
        // 开启 DOM storage API 功能
        webSetting.setDomStorageEnabled(true);
        webSetting.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
//        loadUrl(HTML_URL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webSetting.setJavaScriptEnabled(true);
            addJavascriptInterface(new JavascriptInterfaceImpl() {
                @JavascriptInterface
                public void onDataDidLoad() {
                    if (onMarkDownEvent != null)
                        onMarkDownEvent.onDataDidLoad(MarkDownWebView.this);
                }

                @JavascriptInterface
                public void onDOMContentLoaded() {
                    if (TextUtils.isEmpty(content)) return;
                    splitSend(content);

                }
            }, APPJSNAME);
        }
    }

    public void setContent(String content) {
        this.content = content;
        loadUrl(HTML_URL);
    }

    /**
     * 推荐用rxjava
     * 但是写起来太麻烦了 我这里就用handler
     */
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String script = String.valueOf(msg.obj);
            loadUrl(script);
        }
    };

    private void splitSend(String content) {
        if (content == null) content = "";
        if (content.length() < SEND_LENGTH_UNIT) {
            content = content.replace("\n", SWAP_BREAK_TAG);
            final String script = String.format(Locale.CHINESE, "javascript:loadSpan('%s',%d);", content, 1);
            Message msg = Message.obtain();
            msg.obj = script;
            msg.what = 1;
            handler.sendMessage(msg);
        } else {
            String sendedString = content.substring(0, SEND_LENGTH_UNIT);
            content = content.substring(sendedString.length());
            sendedString = sendedString.replace("\n", SWAP_BREAK_TAG);
            Message msg = Message.obtain();
            msg.what = 0;
            if (TextUtils.isEmpty(content)) {
                msg.what = 1;
            }
            msg.obj = String.format(Locale.CHINESE, "javascript:loadSpan('%s',%d);", sendedString, msg.what);
            handler.sendMessage(msg);
            splitSend(content);
        }
    }


    public void setOnMarkDownEvent(OnMarkDownEvent onMarkDownEvent) {
        this.onMarkDownEvent = onMarkDownEvent;
    }

    OnMarkDownEvent onMarkDownEvent;

    public interface OnMarkDownEvent {

        void onPageFinished(MarkDownWebView view);

        void onDataDidLoad(MarkDownWebView markDownWebView);
    }


    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (handler != null) handler.removeCallbacksAndMessages(null);
        handler = null;
    }


    @Override
    public boolean canGoBack() {
        return false;
    }
}
