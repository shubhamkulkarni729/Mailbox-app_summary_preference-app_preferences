package com.cognition.android.mailboxapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.cognition.android.mailboxapp.Summary_Utils.Summary;
import com.cognition.android.mailboxapp.activities.EventsActivity;
import com.cognition.android.mailboxapp.activities.MainActivity;
import com.cognition.android.mailboxapp.models.Message;
import com.cognition.android.mailboxapp.utils.MessagesAdapter;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
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
import com.mindorks.placeholderview.SwipeDecor;
import com.mindorks.placeholderview.SwipePlaceHolderView;
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
import static com.cognition.android.mailboxapp.activities.MainActivity.SCOPES;
import static com.cognition.android.mailboxapp.activities.MainActivity.TAG;

public class activity_swipe extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    List<Message> messageList;
    MessagesAdapter messagesAdapter;
    List<String> ID = new ArrayList<>();
    GoogleAccountCredential mCredential;
    Gmail mService;
    SharedPreferences sharedPref;
    com.cognition.android.mailboxapp.utils.Utils mUtils;
    String pageToken = null;
    boolean isFetching = false;
    FrameLayout lytParent;


    //nav bar
    Toolbar toolbar;
    private DrawerLayout drawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swipe);

        //nav bar
        toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
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
        sharedPref = activity_swipe.this.getSharedPreferences(getString(R.string.preferences_file_name), Context.MODE_PRIVATE);
        mUtils = new com.cognition.android.mailboxapp.utils.Utils(activity_swipe.this);

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
            startActivity(new Intent(activity_swipe.this, MainActivity.class));
            ActivityCompat.finishAffinity(activity_swipe.this);
        }

        messageList = new ArrayList<>();
        messagesAdapter = new MessagesAdapter(activity_swipe.this, messageList);
        getMessagesFromDB();

        lytParent = findViewById(R.id.lytParent);
        SwipePlaceHolderView mSwipeView = findViewById(R.id.swipeView);
        Context mContext = getApplicationContext();

        mSwipeView.getBuilder()
                .setDisplayViewCount(3)
                .setSwipeDecor(new SwipeDecor()
                        .setPaddingTop(20)
                        .setRelativeScale(0.01f)
                        .setSwipeInMsgLayoutId(R.layout.tinder_swipe_in_msg_view)
                        .setSwipeOutMsgLayoutId(R.layout.tinder_swipe_out_msg_view));


        /*for(com.cognition.android.mailboxapp.Profile profile : Utils.loadProfiles(this.getApplicationContext())){
            mSwipeView.addView(new TinderCard(mContext, profile, mSwipeView));
        }

        if(done==true) {
            for (String id : ID) {
                Message message = SQLite.select().from(Message.class).where(Message_Table.id.eq(Integer.valueOf(id))).querySingle();

                Profile profile = new Profile();
                profile.setId(message.getId());
                profile.setSubject(message.getSubject());
                profile.setMessage(message.getSnippet());
                profile.setSender(message.getFrom());

                mSwipeView.addView(new TinderCard(mContext, profile, mSwipeView));
            }
        }
        */
        List<Message> messages = SQLite.select().from(Message.class).queryList();

        for(Message message: messages) {
            Profile profile = new Profile();
            profile.setId(String.valueOf(message.getId()));
            profile.setSubject(message.getSubject());
            profile.setSender(message.getFrom());
            profile.setMessage(message.getSnippet());

            mSwipeView.addView(new TinderCard(mContext, profile, mSwipeView));
        }

        /*findViewById(R.id.rejectBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSwipeView.doSwipe(false);
            }
        });

        findViewById(R.id.acceptBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSwipeView.doSwipe(true);
            }
        });

         */



    }
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
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity_swipe.this);
                    builder.setMessage(R.string.app_requires_auth);
                    builder.setPositiveButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            ActivityCompat.finishAffinity(activity_swipe.this);
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    private void getMessagesFromDB() {

        if (mUtils.isDeviceOnline())
            new GetEmailsTask(true).execute();
        else
            mUtils.showSnackbar(lytParent, getString(R.string.device_is_offline));
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

    @Override           //Navigation Drawer
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.nav_all_mails:
                Toast.makeText(activity_swipe.this,"Pressed All Mails",Toast.LENGTH_LONG).show();
                break;

            case R.id.nav_change_account:
                Toast.makeText(activity_swipe.this,"Pressed Change Account",Toast.LENGTH_LONG).show();
                break;

            case R.id.nav_educational_mails:
                Toast.makeText(activity_swipe.this,"Pressed Educational Mails",Toast.LENGTH_LONG).show();
                break;

            case R.id.nav_financial_mails:
                Toast.makeText(activity_swipe.this,"Pressed Financial Mails",Toast.LENGTH_LONG).show();
                System.out.println("FINANCIAL");
                break;

            case R.id.nav_job_mails:
                Toast.makeText(activity_swipe.this,"Pressed Job Mails",Toast.LENGTH_LONG).show();
                break;

            case R.id.nav_offer_mails:
                Toast.makeText(activity_swipe.this,"Pressed Offer Mails",Toast.LENGTH_LONG).show();
                break;

            case R.id.nav_settings:
                Toast.makeText(activity_swipe.this,"Pressed Settings",Toast.LENGTH_LONG).show();
                break;

            case R.id.nav_social_mails:
                Toast.makeText(activity_swipe.this,"Pressed Social Mails",Toast.LENGTH_LONG).show();
                break;

            case R.id.nav_sms:
                Toast.makeText(activity_swipe.this,"SMS Pressed",Toast.LENGTH_LONG).show();
                break;

            case R.id.nav_events:
                Toast.makeText(activity_swipe.this,"Events Pressed",Toast.LENGTH_LONG).show();
                startActivity(new Intent(activity_swipe.this, EventsActivity.class));
                ActivityCompat.finishAffinity(activity_swipe.this);

        }
        return true;
    }

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
                activity_swipe.this.pageToken = null;
            }

            try {
                String user = "me";
                String query = "in:inbox";
                ListMessagesResponse messageResponse = mService.users().messages().list(user).setQ(query).setMaxResults(100L).setPageToken(activity_swipe.this.pageToken).execute();

                messageListReceived = new ArrayList<>();
                List<com.google.api.services.gmail.model.Message> receivedMessages = messageResponse.getMessages();
                for (com.google.api.services.gmail.model.Message message : receivedMessages) {
                    com.google.api.services.gmail.model.Message actualMessage = mService.users().messages().get(user, message.getId()).execute();

                    Map<String, String> headers = new HashMap<>();
                    for (MessagePartHeader messagePartHeader : actualMessage.getPayload().getHeaders())
                        headers.put(
                                messagePartHeader.getName(), messagePartHeader.getValue()
                        );

                    //FirebaseDatabase database = FirebaseDatabase.getInstance();
                    //DatabaseReference myRef = database.getReference("messages");
                    //myRef.setValue(actualMessage.getRaw());

                    MessagePart msg = actualMessage.getPayload();
                    String body = StringUtils.newStringUtf8(Base64.decodeBase64(msg.getBody().getData()));
                    //String summary = Summary.summarize(body, headers.get("Subject"), getApplicationContext());

                    Message newMessage = new Message(
                            actualMessage.getLabelIds(),
                            actualMessage.getSnippet(),
                            actualMessage.getPayload().getMimeType(),
                            headers,
                            actualMessage.getPayload().getParts(),
                            actualMessage.getInternalDate(),
                            activity_swipe.this.mUtils.getRandomMaterialColor(),
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

                                document.getElementsByTag("style").remove();
                                document.select("[style]").removeAttr("style");

                                newMessage.setSummary(Summary.summarize(document.text(), newMessage.getSubject(),getApplicationContext()));
                                //Log.d(MainActivity.TAG, result[0]);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                     */
                    ID.add(actualMessage.getId());
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
    }
}