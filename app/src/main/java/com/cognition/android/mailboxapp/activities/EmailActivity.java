package com.cognition.android.mailboxapp.activities;

import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.Log;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import com.cognition.android.mailboxapp.R;
import com.cognition.android.mailboxapp.Summary_Utils.Summary;
import com.cognition.android.mailboxapp.models.Message;
import com.cognition.android.mailboxapp.models.Message_Table;
import com.cognition.android.mailboxapp.utils.Utils;
import com.google.api.client.util.Base64;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.transitionseverywhere.TransitionManager;
import com.wang.avi.AVLoadingIndicatorView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class EmailActivity extends AppCompatActivity {

    CoordinatorLayout lytParent;
    AppCompatTextView txtSubject, txtFromPreview, txtFrom, txtDate;
    ConstraintLayout lytFromPreviewParent;
    WebView myWebView;
    AVLoadingIndicatorView avLoadingIndicatorView;
    LinearLayoutCompat lytError;
    AppCompatButton btnRetry;
    String mySummary ;

    Utils mUtils;
    Message mMessage = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email);
        mUtils = new Utils(EmailActivity.this);

        int messageId = getIntent().getIntExtra("messageId", -1);
        mMessage = SQLite.select().from(Message.class).where(Message_Table.id.eq(messageId)).querySingle();

        if (mMessage != null) {
            initViews();
            setUpWebView();
        } else
            onBackPressed();
    }

    /**
     * Initialize views
     */
    //@SuppressLint("SetJavaScriptEnabled")
    private void initViews() {
        lytParent = findViewById(R.id.lytParent);
        //toolbar = findViewById(R.id.toolbar);
        txtSubject = findViewById(R.id.txtSubject);
        txtFromPreview = findViewById(R.id.txtFromPreview);
        txtFrom = findViewById(R.id.txtFrom);
        txtDate = findViewById(R.id.txtDate);
        lytFromPreviewParent = findViewById(R.id.lytFromPreviewParent);
        myWebView = findViewById(R.id.webView);
        avLoadingIndicatorView = findViewById(R.id.aviLoadingIndicator);
        lytError = findViewById(R.id.lytError);
        btnRetry = findViewById(R.id.btnRetry);
        /*toolbar.setTitle(mMessage.getSubject());
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });*/

        txtSubject.setText(mMessage.getSubject());
        txtFromPreview.setText(mMessage.getFrom().substring(0, 1).toUpperCase(Locale.ENGLISH));
        txtFrom.setText(mMessage.getFrom());
        txtDate.setText(mUtils.timestampToDate(mMessage.getTimestamp()));

        android.graphics.drawable.GradientDrawable gradientDrawable = (android.graphics.drawable.GradientDrawable) lytFromPreviewParent.getBackground();
        gradientDrawable.setColor(mMessage.getColor());

        WebSettings myWebSettings = myWebView.getSettings();
        myWebSettings.setUseWideViewPort(true);
        myWebSettings.setSupportZoom(true);
        myWebSettings.setBuiltInZoomControls(true);
        myWebSettings.setDisplayZoomControls(false);
        myWebSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        myWebSettings.setDomStorageEnabled(true);
        myWebView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        myWebView.setScrollbarFadingEnabled(true);
        myWebView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                TransitionManager.beginDelayedTransition(lytParent);
                lytError.setVisibility(View.GONE);
                avLoadingIndicatorView.smoothToHide();
                myWebView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onReceivedError(WebView view, int statuscode, String description, String url) {
                super.onReceivedError(view, statuscode, description, url);

                Log.e(MainActivity.TAG, String.format("code: %d\tdescription: %s\turl: %s", statuscode, description, url));

                TransitionManager.beginDelayedTransition(lytParent);
                myWebView.setVisibility(View.GONE);
                lytError.setVisibility(View.VISIBLE);
                avLoadingIndicatorView.smoothToHide();
            }

            //@RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Log.e(MainActivity.TAG, String.format("code: %d\tdescription: %s\turl: %s", error.getErrorCode(), error.getDescription(), request.getUrl()));
                }

                TransitionManager.beginDelayedTransition(lytParent);
                myWebView.setVisibility(View.GONE);
                lytError.setVisibility(View.VISIBLE);
                avLoadingIndicatorView.smoothToHide();
            }

            //@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse response) {
                super.onReceivedHttpError(view, request, response);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Log.e(MainActivity.TAG, String.format("code: %d\tdescription: %s\turl: %s", response.getStatusCode(), response.getReasonPhrase(), request.getUrl()));
                }

                TransitionManager.beginDelayedTransition(lytParent);
                myWebView.setVisibility(View.GONE);
                lytError.setVisibility(View.VISIBLE);
                avLoadingIndicatorView.smoothToHide();
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                super.onReceivedSslError(view, handler, error);
                handler.proceed();

                Log.e(MainActivity.TAG, error.toString());

            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
                return true;
            }

            //@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    i.setData(request.getUrl());
                }
                startActivity(i);
                return true;
            }

        });

        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setUpWebView();
            }
        });
    }

    private void showSummary() {
        Intent intent = new Intent(EmailActivity.this, SummaryActivity.class);
        intent.putExtra("summary",mySummary);
        //finish();
        startActivity(intent);
    }

    /**
     * Prepare the webview to display the email
     */
    private void setUpWebView() {
        TransitionManager.beginDelayedTransition(lytParent);
        myWebView.setVisibility(View.GONE);
        lytError.setVisibility(View.GONE);
        avLoadingIndicatorView.smoothToShow();

        try {
            JSONObject parentPart = new JSONObject(mMessage.getParentPartJson());

            if (parentPart.getJSONObject("body").getInt("size") != 0) {
                byte[] dataBytes = Base64.decodeBase64(parentPart.getJSONObject("body").getString("data"));
                String data = new String(dataBytes, StandardCharsets.UTF_8);

                //improved_summary is = new improved_summary(data);

                myWebView.loadData(data, mMessage.getMimetype(), "UTF-8");

                Document document = Jsoup.parse(data);
                Elements el = document.getAllElements();
                for (Element e : el) {
                    Attributes at = e.attributes();
                    for (Attribute a : at) {
                        e.removeAttr(a.getKey());
                    }
                }

                document.getElementsByTag("style").remove();
                document.select("[style]").removeAttr("style");

                //Log.d("DOCUMENT TEXT IS::",document.body());

                data=data.replaceAll("<.*?>","");

                //System.out.println("THE DATA::::"+ data);

                Log.d(MainActivity.TAG, mMessage.getMimetype());
                TextView textView = findViewById(R.id.summary_text);
                textView.setText(Summary.summarize(document.text(), mMessage.getSubject(),getApplicationContext()));
            } else {
                JSONArray partsArray = new JSONArray(mMessage.getPartsJson());

                String[] result = getData(partsArray);
                if (result[0] != null && result[1] != null) {
                    myWebView.loadData(result[1], result[0], "UTF-8");
                        //(data,mimeType,encoding)

                    result[1]=result[1].replaceAll("<.*?>","");
                    //System.out.println("THE DATA IS::::"+result[1]);

                    Document document = Jsoup.parse(result[1]);
                    Log.d("DOCUMENT TEXT IS::",document.text());

                    Log.d(MainActivity.TAG, result[0]);
                    TextView textView = findViewById(R.id.summary_text);
                    textView.setText(Summary.summarize(document.text(), mMessage.getSubject(),getApplicationContext()));

                } else {
                    TransitionManager.beginDelayedTransition(lytParent);
                    myWebView.setVisibility(View.GONE);
                    lytError.setVisibility(View.VISIBLE);
                    avLoadingIndicatorView.smoothToHide();
                }
            }
        } catch (Exception e) {
            Log.w(MainActivity.TAG, e);
            e.printStackTrace();

            TransitionManager.beginDelayedTransition(lytParent);
            myWebView.setVisibility(View.GONE);
            lytError.setVisibility(View.VISIBLE);
            avLoadingIndicatorView.smoothToHide();
        }
    }
    /**
     * Get email data
     *
     * @param parts JSONArray
     * @return String[]
     * @throws JSONException                JSONException
     */
    private String[] getData(JSONArray parts) throws JSONException {
        for (int i = 0; i < parts.length(); i++) {
            JSONObject part = new JSONObject(parts.getString(i));
            if (part.has("parts"))
                return getData(new JSONArray(part.getString("parts")));
            else {
                if (part.getString("mimeType").equals("text/html"))
                    return new String[]{
                            part.getString("mimeType"),
                            new String(
                                    Base64.decodeBase64(part.getJSONObject("body").getString("data")),
                                    StandardCharsets.UTF_8
                            )
                    };
            }
        }
        for (int i = 0; i < parts.length(); i++) {
            JSONObject part = new JSONObject(parts.getString(i));
            if (part.getString("mimeType").equals("text/plain"))
                return new String[]{
                        part.getString("mimeType"),
                        new String(
                                Base64.decodeBase64(part.getJSONObject("body").getString("data")),
                                StandardCharsets.UTF_8
                        )
                };
        }
        return new String[]{null, null};
    }
}
