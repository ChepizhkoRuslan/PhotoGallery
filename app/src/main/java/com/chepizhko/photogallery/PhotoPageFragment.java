package com.chepizhko.photogallery;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;


public class PhotoPageFragment extends VisibleFragment {
    private static final String ARG_URI = "photo_page_url";
    private Uri mUri;
    private WebView mWebView;
    private ProgressBar mProgressBar;

    public static PhotoPageFragment newInstance(Uri uri) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_URI, uri);
        PhotoPageFragment fragment = new PhotoPageFragment();
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUri = getArguments().getParcelable(ARG_URI);
    }
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_photo_page, container, false);
        mProgressBar = (ProgressBar)v.findViewById(R.id.progress_bar);
        mProgressBar.setMax(100); // Значения в диапазоне 0-100
        mWebView = (WebView) v.findViewById(R.id.web_view);
        // мы включаем JavaScript
        mWebView.getSettings().setJavaScriptEnabled(true);
        // WebChromeClient определяет событийный интерфейс об-работки событий,
        // которые должны изменять элементы «хрома» (chrome) в браузере.
        // К этой категории относятся сигналы (alerts) JavaScript, значки сайтов
        // favicon, обновления прогресса загрузки и т. д.
        mWebView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView webView, int newProgress) {
                if (newProgress == 100) {
                    mProgressBar.setVisibility(View.GONE);
                } else {
                    mProgressBar.setVisibility(View.VISIBLE);
                    mProgressBar.setProgress(newProgress);
                }
            }
            public void onReceivedTitle(WebView webView, String title) {
                AppCompatActivity activity = (AppCompatActivity) getActivity();
                activity.getSupportActionBar().setSubtitle(title);
            }
        });
        // создаём клиент
        mWebView.setWebViewClient(new WebViewClient());
        mWebView.loadUrl(mUri.toString());
        return v;
    }
}
