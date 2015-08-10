package me.ngargi.nachat;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.pubnub.api.*;
import org.json.*;

import java.util.ArrayList;
import java.util.Objects;


public class ChatActivity extends ActionBarActivity {
    public String name;
    public String channel;
    public String history = "<div style='font-family:monospace'>";
    public String color = generateColor();
    public ArrayList<String> online = new ArrayList<>();

    public void updateHTML(final JSONObject message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                WebView webView = (WebView)findViewById(R.id.webView);
                webView.loadDataWithBaseURL(null, generateHTML(message)+"</div>", "text/html", "utf-8", null);
            }
        });
    }
    public String generateHTML(JSONObject message){
        try {
            history += "</br><br><font color='" + message.getString("color") + "'>" + message.getString("name") + "</font>: " + message.getString("text");
            notify(message.getString("name")+": "+message.getString("text"));
            return history;
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }
    private boolean mIsInForegroundMode;

    @Override
    protected void onPause() {
        super.onPause();
        mIsInForegroundMode = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsInForegroundMode = true;
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(1000);
    }

    // Some function.
    public boolean isInForeground() {
        return mIsInForegroundMode;
    }
    public void notify(String message){
        if (!mIsInForegroundMode){
            Context context = this;
            Intent notificationIntent = new Intent(context, ChatActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent intent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle("New NaChat Message!")
                    .setContentIntent(intent)
                    .setPriority(5) //private static final PRIORITY_HIGH = 5;
                    .setContentText(message)
                    .setAutoCancel(true)
                    .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS);
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(1000, mBuilder.build());
    }
    }
    public void spinner(final Boolean thing){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBar);
                if (thing){
                    progressBar.setVisibility(View.VISIBLE);
                }
                else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }
    public String generateColor(){
        String letters = "00123456789ABCDE";
        String color = "#";
        for (int i = 0; i < 6; i++) {
            color += letters.charAt((int) Math.floor(Math.random() * 16));
        }
        return color;
    }
    public void sendMessage(String message){
        final Pubnub pubnub = new Pubnub("pub-c-80ab65f4-2c8e-4ad4-8f8e-8d27f05c1218", "sub-c-c49bb262-6a08-11e4-915f-02ee2ddab7fe");
        try {
            JSONObject jObj = new JSONObject("{\"name\": \"" + name + "\", \"color\": \"" + color + "\", \"text\": \"" + message + "\"}");
            pubnub.publish(channel, jObj, new Callback() {
                @Override
                public void successCallback(String channel, Object message) {
                    super.successCallback(channel, message);
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public void onSendClick(View view){
        EditText text = (EditText)findViewById(R.id.editText3);
        sendMessage(String.valueOf(text.getText()));
        text.setText("");
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Intent intent = getIntent();
        if (intent.hasExtra("history") && intent.hasExtra("color")){
            color = intent.getStringExtra("color");
            history = intent.getStringExtra("history");
        }
        else {
            history += "Welcome to NaChat!";
        }
        name = intent.getStringExtra("name");
        channel = intent.getStringExtra("channel");

        setTitle("NaChat - " + channel);
        final WebView webView = (WebView)findViewById(R.id.webView);
        webView.loadDataWithBaseURL(null, history, "text/html", "utf-8", null);
        System.out.println(name);
        final Pubnub pubnub = new Pubnub("pub-c-80ab65f4-2c8e-4ad4-8f8e-8d27f05c1218", "sub-c-c49bb262-6a08-11e4-915f-02ee2ddab7fe");

        try {
            pubnub.subscribe(channel, new Callback() {
                        @Override
                        public void connectCallback(String channel, Object message) {
                            spinner(Boolean.FALSE);
                        }

                        @Override
                        public void disconnectCallback(String channel, Object message) {
                            System.out.println("SUBSCRIBE : DISCONNECT on channel:" + channel
                                    + " : " + message.getClass() + " : "
                                    + message.toString());
                            spinner(Boolean.TRUE);
                        }

                        public void reconnectCallback(String channel, Object message) {
                            System.out.println("SUBSCRIBE : RECONNECT on channel:" + channel
                                    + " : " + message.getClass() + " : "
                                    + message.toString());
                            spinner(Boolean.FALSE);
                        }

                        @Override
                        public void successCallback(String channel, Object message) {
                            System.out.println("SUBSCRIBE : " + channel + " : "
                                    + message.getClass() + " : " + message.toString());
                            try {
                                JSONObject messageObj = new JSONObject(message.toString());
//                                TextView messages = (TextView)findViewById(R.id.messages);
                                if (!messageObj.has("ping")) {
                                    updateHTML(messageObj);
                                }
                            }
                            catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        @Override
                        public void errorCallback(String channel, PubnubError error) {
                            System.out.println("SUBSCRIBE : ERROR on channel " + channel
                                    + " : " + error.toString());
                        }
                    }
            );
        } catch (PubnubException e) {
            System.out.println(e.toString());
        }
    }

    @Override
   public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
