package com.cognition.android.mailboxapp.activities;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.cognition.android.mailboxapp.R;
import com.cognition.android.mailboxapp.Summary_Utils.Summary;
import com.cognition.android.mailboxapp.View_type;
import com.cognition.android.mailboxapp.activity_swipe;
import com.cognition.android.mailboxapp.models.Message;
import com.cognition.android.mailboxapp.utils.EndlessRecyclerViewScrollListener;
import com.cognition.android.mailboxapp.utils.MessagesAdapter;
import com.cognition.android.mailboxapp.utils.Utils;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.client.util.StringUtils;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.raizlabs.android.dbflow.sql.language.SQLite;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import pub.devrel.easypermissions.EasyPermissions;
import static com.cognition.android.mailboxapp.activities.MainActivity.PREF_ACCOUNT_NAME;
import static com.cognition.android.mailboxapp.activities.MainActivity.REQUEST_AUTHORIZATION;
import static com.cognition.android.mailboxapp.activities.MainActivity.REQUEST_GOOGLE_PLAY_SERVICES;
import static com.cognition.android.mailboxapp.activities.MainActivity.SCOPES;
import static com.cognition.android.mailboxapp.activities.MainActivity.TAG;

public class InboxActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    CoordinatorLayout lytParent;
    //Toolbar toolbar;
    SwipeRefreshLayout refreshMessages;
    RecyclerView listMessages;
    //FloatingActionButton fabCompose;
    List<Message> messageList;
    MessagesAdapter messagesAdapter;

    GoogleAccountCredential mCredential;
    Gmail mService;
    SharedPreferences sharedPref;
    Utils mUtils;
    String pageToken = null;
    boolean isFetching = false;

    //nav bar
    Toolbar toolbar;
    private DrawerLayout drawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

        //nav bar
        toolbar = (Toolbar)findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.openNavDrawer, R.string.closeNavDrawer);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setCheckedItem(R.id.nav_all_mails);

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        mService = null;
        sharedPref = InboxActivity.this.getSharedPreferences(getString(R.string.preferences_file_name), Context.MODE_PRIVATE);
        mUtils = new Utils(InboxActivity.this);

        String accountName = sharedPref.getString(PREF_ACCOUNT_NAME, null);
        if (accountName != null) {
            mCredential.setSelectedAccountName(accountName);

            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.gmail.Gmail.Builder(
                    transport, jsonFactory, mCredential)
                    .setApplicationName("MailBox App")
                    .build();

        } else {
            startActivity(new Intent(InboxActivity.this, MainActivity.class));
            ActivityCompat.finishAffinity(InboxActivity.this);
        }

        messageList = new ArrayList<>();
        messagesAdapter = new MessagesAdapter(InboxActivity.this, messageList);

        initViews();
        getMessagesFromDB();

        findViewById(R.id.floatingActionButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(InboxActivity.this, View_type.class));
                ActivityCompat.finishAffinity(InboxActivity.this);
            }
        });
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     *
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode  code indicating the result of the incoming
     *                    activity result.
     * @param data        Intent (containing result data) returned by incoming
     *                    activity result.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_AUTHORIZATION) {
            if (resultCode == RESULT_OK) {
                if (!isFetching && mUtils.isDeviceOnline()) {
                    getMessagesFromDB();
                } else
                    mUtils.showSnackbar(lytParent, getString(R.string.device_is_offline));
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(InboxActivity.this);
                builder.setMessage(R.string.app_requires_auth);
                builder.setPositiveButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        ActivityCompat.finishAffinity(InboxActivity.this);
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     *
     * @param requestCode  The request code passed in
     *                     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Initialize the views
     */
    private void initViews() {
        lytParent = findViewById(R.id.lytParent);
       // toolbar = findViewById(R.id.toolbar);
        refreshMessages = findViewById(R.id.refreshMessages);
        listMessages = findViewById(R.id.listMessages);
        //fabCompose = findViewById(R.id.fabCompose);

        //toolbar.inflateMenu(R.menu.menu_inbox);

        /*SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) toolbar.getMenu().findItem(R.id.action_search).getActionView();
        searchView.setQueryHint(getString(R.string.search));
        searchView.setSearchableInfo(searchManager != null ? searchManager.getSearchableInfo(getComponentName()) : null);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                messagesAdapter.getFilter().filter(query);

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                messagesAdapter.getFilter().filter(newText);

                return true;
            }
        });*/

        refreshMessages.setColorSchemeResources(R.color.colorPrimary);
        refreshMessages.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (!isFetching && mUtils.isDeviceOnline()) {
                    getMessagesFromDB();
                } else
                    mUtils.showSnackbar(lytParent, getString(R.string.device_is_offline));
            }
        });

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(InboxActivity.this);
        listMessages.setLayoutManager(mLayoutManager);
        listMessages.setItemAnimator(new DefaultItemAnimator());
        listMessages.addOnScrollListener(new EndlessRecyclerViewScrollListener(mLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                if (!isFetching && mUtils.isDeviceOnline())
                    new GetEmailsTask(false).execute();
            }
        });
        listMessages.setAdapter(messagesAdapter);

        /*fabCompose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(InboxActivity.this, ComposeActivity.class));
            }
        });*/
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     *
     * @param connectionStatusCode code describing the presence (or lack of)
     *                             Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                InboxActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * Get cached emails
     */
    private void getMessagesFromDB() {
        InboxActivity.this.refreshMessages.setRefreshing(true);
        InboxActivity.this.messageList.clear();
        InboxActivity.this.messageList.addAll(SQLite.select().from(Message.class).queryList());
        InboxActivity.this.messagesAdapter.notifyDataSetChanged();
        InboxActivity.this.refreshMessages.setRefreshing(false);

        if (mUtils.isDeviceOnline())
            new GetEmailsTask(true).execute();
        else
            mUtils.showSnackbar(lytParent, getString(R.string.device_is_offline));
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.nav_all_mails:
                Toast.makeText(InboxActivity.this,"Pressed All Mails",Toast.LENGTH_LONG).show();
                break;

            case R.id.nav_change_account:
                Toast.makeText(InboxActivity.this,"Pressed Change Account",Toast.LENGTH_LONG).show();
                break;

            case R.id.nav_educational_mails:
                Toast.makeText(InboxActivity.this,"Pressed Educational Mails",Toast.LENGTH_LONG).show();
                break;

            case R.id.nav_financial_mails:
                Toast.makeText(InboxActivity.this,"Pressed Financial Mails",Toast.LENGTH_LONG).show();
                System.out.println("FINANCIAL");
                break;

            case R.id.nav_job_mails:
                Toast.makeText(InboxActivity.this,"Pressed Job Mails",Toast.LENGTH_LONG).show();
                break;

            case R.id.nav_offer_mails:
                Toast.makeText(InboxActivity.this,"Pressed Offer Mails",Toast.LENGTH_LONG).show();
                break;

            case R.id.nav_settings:
                Toast.makeText(InboxActivity.this,"Pressed Settings",Toast.LENGTH_LONG).show();
                break;

            case R.id.nav_social_mails:
                Toast.makeText(InboxActivity.this,"Pressed Social Mails",Toast.LENGTH_LONG).show();
                break;

            case R.id.nav_sms:
                Toast.makeText(InboxActivity.this,"SMS Pressed",Toast.LENGTH_LONG).show();
                break;

            case R.id.nav_events:
                Toast.makeText(InboxActivity.this,"Events Pressed",Toast.LENGTH_LONG).show();
                startActivity(new Intent(InboxActivity.this, EventsActivity.class));
                ActivityCompat.finishAffinity(InboxActivity.this);

        }
        return true;
    }

    /**
     * Get emails in the background
     */
    @SuppressLint("StaticFieldLeak")
    private class GetEmailsTask extends AsyncTask<Void, Void, List<Message>> {

        private int itemCount = 0;
        private boolean clear;
        private Exception mLastError = null;

        GetEmailsTask(boolean clear) {
            this.clear = clear;
        }

        @Override
        protected List<Message> doInBackground(Void... voids) {
            isFetching = true;
            List<Message> messageListReceived = null;

            if (clear) {
                //Delete.table(Message.class);
                InboxActivity.this.pageToken = null;
            }

            try {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        InboxActivity.this.refreshMessages.setRefreshing(true);
                    }
                });

                String user = "me";
                String query = "in:inbox";
                ListMessagesResponse messageResponse = mService.users().messages().list(user).setQ(query).setMaxResults(100L).setPageToken(InboxActivity.this.pageToken).execute();
                InboxActivity.this.pageToken = messageResponse.getNextPageToken();

                messageListReceived = new ArrayList<>();
                List<com.google.api.services.gmail.model.Message> receivedMessages = messageResponse.getMessages();
                for (com.google.api.services.gmail.model.Message message : receivedMessages) {
                    com.google.api.services.gmail.model.Message actualMessage = mService.users().messages().get(user, message.getId()).execute();


                    Map<String, String> headers = new HashMap<>();
                    for (MessagePartHeader messagePartHeader : actualMessage.getPayload().getHeaders())
                        headers.put(
                                messagePartHeader.getName(), messagePartHeader.getValue()
                        );

                    MessagePart msg = actualMessage.getPayload();
                    String body = StringUtils.newStringUtf8(Base64.decodeBase64(msg.getBody().getData()));
                    //String summary = Summary.summarize(body, headers.get("Subject"),getApplicationContext());


                    Message newMessage = new Message(
                            actualMessage.getLabelIds(),
                            actualMessage.getSnippet(),
                            actualMessage.getPayload().getMimeType(),
                            headers,
                            actualMessage.getPayload().getParts(),
                            actualMessage.getInternalDate(),
                            InboxActivity.this.mUtils.getRandomMaterialColor(),
                            actualMessage.getPayload(),
                            body);

                    /*
                    try {
                        JSONObject parentPart = new JSONObject(newMessage.getParentPartJson());

                        if (parentPart.getJSONObject("body").getInt("size") != 0) {
                            byte[] dataBytes = Base64.decodeBase64(parentPart.getJSONObject("body").getString("data"));
                            String data = new String(dataBytes, StandardCharsets.UTF_8);

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

                            data = data.replaceAll("\\<.*?>", "");


                            newMessage.setSummary(Summary.summarize(document.text(), newMessage.getSubject(),getApplicationContext()));
                            //System.out.println("THE DATA::::" + data);

                            //Log.d(MainActivity.TAG, newMessage.getMimetype());
                        } else {
                            JSONArray partsArray = new JSONArray(newMessage.getPartsJson());

                            String[] result = getData(partsArray);
                            if (result[0] != null && result[1] != null) {
                                //(data,mimeType,encoding)

                                result[1] = result[1].replaceAll("\\<.*?>", "");
                                //System.out.println("THE DATA IS::::" + result[1]);

                                Document document = Jsoup.parse(result[1]);
                                //Log.d("DOCUMENT TEXT IS::", document.text());


                                newMessage.setSummary(Summary.summarize(document.text(), newMessage.getSubject(),getApplicationContext()));
                                //Log.d(MainActivity.TAG, result[0]);
                            }

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                     */
                    newMessage.save();
                    messageListReceived.add(newMessage);

                    itemCount++;
                }
            } catch (Exception e) {
                Log.w(TAG, e);
                mLastError = e;
                cancel(true);
            }

            return messageListReceived;
        }

        @Override
        protected void onPostExecute(List<Message> output) {
            isFetching = false;

            if (output != null && output.size() != 0) {
                if (clear) {
                    InboxActivity.this.messageList.clear();
                    InboxActivity.this.messageList.addAll(output);
                    InboxActivity.this.messagesAdapter.notifyDataSetChanged();
                } else {
                    int listSize = InboxActivity.this.messageList.size();
                    InboxActivity.this.messageList.addAll(output);
                    InboxActivity.this.messagesAdapter.notifyItemRangeInserted(listSize, itemCount);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        InboxActivity.this.refreshMessages.setRefreshing(false);
                    }
                });
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        InboxActivity.this.refreshMessages.setRefreshing(false);
                    }
                });
                InboxActivity.this.mUtils.showSnackbar(lytParent, getString(R.string.fetch_failed));
            }
        }

        @Override
        protected void onCancelled() {
            isFetching = false;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    InboxActivity.this.refreshMessages.setRefreshing(false);
                }
            });
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            REQUEST_AUTHORIZATION);
                } else {
                    mUtils.showSnackbar(lytParent, getString(R.string.an_error_occurred));
                }
            } else {
                mUtils.showSnackbar(lytParent, getString(R.string.an_error_occurred));
            }
        }

    }

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