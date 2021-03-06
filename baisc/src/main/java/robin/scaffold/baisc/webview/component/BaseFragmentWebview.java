package robin.scaffold.baisc.webview.component;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import robin.scaffold.baisc.BuildConfig;
import robin.scaffold.baisc.R;
import robin.scaffold.baisc.webview.IWebChromeClientDelegate;
import robin.scaffold.baisc.webview.IWebClientDelegate;
import robin.scaffold.baisc.webview.IWebViewUI;
import robin.scaffold.baisc.webview.WebController;
import robin.scaffold.baisc.webview.develop.IDevelopUi;
import robin.scaffold.baisc.webview.function.camera.CameraHelper;
import robin.scaffold.baisc.webview.function.download.DownloadFunc;
import robin.scaffold.baisc.webview.function.download.IDownloadUi;
import robin.scaffold.baisc.webview.function.js.IJsUi;
import robin.scaffold.baisc.webview.function.location.LocationHelper;
import robin.scaffold.baisc.webview.function.permission.PermissionCallbackListener;
import robin.scaffold.baisc.webview.function.permission.PermissionInterceptor;
import robin.scaffold.baisc.webview.function.permission.WebPermissionConstant;
import robin.scaffold.baisc.webview.function.upload.IChooserResult;
import robin.scaffold.baisc.webview.function.upload.IUploadUi;
import robin.scaffold.baisc.webview.function.upload.StorageHelper;


public class BaseFragmentWebview extends Fragment implements IWebViewUI, IJsUi, IUploadUi, IDownloadUi, IDevelopUi {
    protected WebController controller;
    protected BaseWebView webView;
    protected ViewGroup rootView;
    protected Context context;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        getController().onCreate(savedInstanceState);
        init();
    }

    protected void init() {
    }

    @Override
    public void onDestroy() {
        getController().onDestroy();
        if(rootView != null)
            rootView.removeAllViews();
        if(webView != null){
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getController().onCreateView(savedInstanceState);
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_web, container, false);
        webView = createWebView();
        rootView.addView(webView,new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        start();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getController().onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        if(getUserVisibleHint()) {
            getController().onStart();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if(getUserVisibleHint()) {
            getController().onStop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(getUserVisibleHint()) {
            getController().onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(getUserVisibleHint()) {
            getController().onPause();
        }
    }

    public void onNewIntent(Intent intent) {
        getController().onNewIntent(intent);
        start();
    }

    protected void onRestart() {
        getController().onRestart();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        getController().onHiddenChanged(hidden);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        getController().onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        getController().onSaveInstanceState(outState);
    }

    //开始初始化和加载
    private void start() {
        Intent intent = ((Activity)context).getIntent();
        String url = intent.getData().toString();

        if (intent.hasExtra("head")) {
            String head = intent.getStringExtra("head");
            Map<String, String> map = new Gson().fromJson(head, new TypeToken<HashMap<String, String>>() {
            }.getType());
            getController().go(url,map);
        } else {
            getController().go(url);
        }
    }



    protected WebController getController() {
        if(controller == null) {
            controller = new WebController(context, this)
                    .setIDownloadUi(this)
                    .setIJsUi(this)
                    .setIUploadUi(this)
                    .setIDevelopUi(this);
            controller.init();
            controller.setPermissionInterceptor(permissionInterceptor);
        }
        return controller;
    }

    private BaseWebView createWebView() {
        BaseWebView webView = WebViewFactory.createWebView(context);
        setWebClient(webView);
        setWebChromeClient(webView);
        setWebViewPropety(webView);
        setWebListener(webView);
        return webView;
    }

    private void setWebListener(BaseWebView webView) {
        webView.setDownloadListener(getController().getDownloadListener());
    }

    private void setWebViewPropety(BaseWebView webView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAppCacheEnabled(false);
        try {
            webSettings.setBuiltInZoomControls(true);
            webSettings.setDisplayZoomControls(false);
        } catch (Exception e) {
        } catch (Error e) {
        }
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        String databasePath = context.getApplicationContext().getDir("database", Context.MODE_PRIVATE).getPath();
        webSettings.setDatabasePath(databasePath);
        webSettings.setGeolocationEnabled(true);
        webSettings.setGeolocationDatabasePath(context.getFilesDir().getPath());
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setSavePassword(false);
        webSettings.setSaveFormData(false);
        webView.setInitialScale(100);
    }

    private void setWebClient(BaseWebView webView) {



        webView.setWebViewClient(new WebViewClientWrapper(new IWebClientDelegate() {
            @Override
            public void onLoadResource(WebView view, String url) {
                getController().onLoadResource(view, url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                callBack.onPageFinish();
                getController().onPageFinished(view, url);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                callBack.onPageStart();
                getController().onPageStarted(view, url, favicon);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                getController().onReceivedError(view, errorCode, description, failingUrl);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                getController().onReceivedSslError(view, handler, error);
            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return shouldInterceptRequest(view, request.getUrl().toString());
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                return getController().shouldInterceptRequest(view, url);

            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                callBack.SchemLoadCallback(url);
                return getController().shouldOverrideUrlLoading(view, url);
            }


            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                callBack.UrlLoadCallBack(request);
                return getController().shouldOverrideUrlLoading(view, request);
            }
        }));
    }

    public ShouldOverrideUrlLoadingCallBack callBack;

    public void setshouldOverrideUrlLoadingCallBack(ShouldOverrideUrlLoadingCallBack callBack) {
        this.callBack = callBack;
    }

    public interface ShouldOverrideUrlLoadingCallBack{
        void UrlLoadCallBack(WebResourceRequest request);

        void SchemLoadCallback(String url);

        void onPageStart();

        void onPageFinish();
    }


    public String getValueByName(String url, String name) {
//        String result = "";
//        int index = url.indexOf("?");
//        String temp = url.substring(index + 1);
//        String[] keyValue = temp.split("&");
//        for (String str : keyValue) {
//            if (str.contains(name)) {
//                result = str.replace(name + "=", "");
//                break;
//            }
//        }

        Uri uri = Uri.parse(url);
        String result = uri.getQueryParameter(name);

        return result;
    }




    private void setWebChromeClient(BaseWebView webView) {
        webView.setWebChromeClient(new WebChromeClientWrapper(new IWebChromeClientDelegate() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                return getController().onConsoleMessage(consoleMessage);
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                getController().onGeolocationPermissionsShowPrompt(origin, callback);
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                return getController().onJsAlert(view, url, message, result);
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
                return getController().onJsPrompt(view, url, message, defaultValue, result);
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                getController().onProgressChanged(view, newProgress);
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                getController().onReceivedTitle(view, title);
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                return getController().onShowFileChooser(webView, filePathCallback, fileChooserParams);
            }

            @Override
            public void openFileChooserBelowLollipop(ValueCallback<Uri> uploadFile, String acceptType, String capture) {
                getController().openFileChooserBelowLollipop(uploadFile, acceptType, capture);
            }
        }));
    }

    //API 23以上
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        controller.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //API 23以下
    protected PermissionInterceptor permissionInterceptor = new PermissionInterceptor() {
        @Override
        public boolean intercept(String url, String[] permissions, int code, final PermissionCallbackListener listener) {
            if(WebPermissionConstant.REQUESTCODE_LOCATION == code) {
                if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
                    //弹出提示框
                LocationHelper.showPermissionAlert((Activity) context, url, permissions, listener);
                return true;
                }

            } else if(WebPermissionConstant.REQUESTCODE_CAMERA == code){
                if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
                    //弹出提示框
                CameraHelper.showPermissionAlert((Activity) context, url, permissions, listener);
                return true;
                }

            } else if(WebPermissionConstant.REQUESTCODE_STORAGE == code){
                if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
                    //弹出提示框
                StorageHelper.showPermissionAlert((Activity) context, url, permissions, listener);
                return true;
                }
            }
            return false;
        }
    };

    //==========================implements IWebViewUI===========================================
    @Override
    public void loadUrl(String url) {
        if(webView != null){
            webView.loadUrl(url);
        }
    }

    @Override
    public void loadUrl(String url, Map<String, String> exHeaders) {
        if(webView != null){
            webView.loadUrl(url, exHeaders);
        }
    }

    @Override
    public void onTitleCallback(String title) {
        Log.d("BaseFragmentWebview", "title=" + title);
    }

    @Override
    public void refresh() {
        if(webView != null){
            webView.reload();
        }
    }

    @Override
    public boolean goBack() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return false;
    }

    @Override
    public void goForward() {
        if(webView != null && webView.canGoForward()){
            webView.goForward();
        }
    }

    @Override
    public WebBackForwardList getWebBackForwardList() {
        if(webView != null){
            return webView.copyBackForwardList();
        }
        return null;
    }

    @Override
    public BaseWebView getWebView() {
        return webView;
    }

    @Override
    public void uiOnPageFinish() {
    }

    @Override
    public void uiOnPageStart() {
    }

    //===============================IJsUi=================================
    @Override
    public void loadJs(String script, ValueCallback<String> callback) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT){
            webView.loadUrl(script);
        }else{
            webView.evaluateJavascript(script, callback);
        }
    }

    @Override
    public void onAddJavaObjects() {
    }

    //===============================IUploadUi=================================
    @Override
    public void showChooser(String[] options, final IChooserResult chooser) {
        AlertDialog alertDialog = new AlertDialog.Builder(context)//
                .setSingleChoiceItems(options, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if(chooser != null)
                            chooser.onChoose(which);
                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        dialog.dismiss();
                        if(chooser != null)
                            chooser.onCancel();
                    }
                }).create();
        alertDialog.show();
    }

    //===============================IDownloadUi=================================
    @Override
    public void showNetWarnMessage(final String url, final long contentLength, final File file, final DownloadFunc.ForceDownloadCallback callback) {
        if(context == null || !isAdded()) {
            return;
        }
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(context)
                .setTitle("提示")
                .setMessage("您正在使用手机流量 ， 继续下载该文件吗?")
                .setNegativeButton("下载", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(callback != null) {
                            callback.forceDowload(url, contentLength, file);
                        }
                    }
                }).
                setPositiveButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    @Override
    public void showTaskRunningWarnMessage() {
        if(context == null || !isAdded()) {
            return;
        }
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(context)
                .setTitle("提示")
                .setMessage("该任务正在下载中")
                .setPositiveButton("知道了", null).create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    @Override
    public void showStartDownloadMessage(String fileName) {

    }

    @Override
    public void onDevelopmentRegister() {

    }
}
