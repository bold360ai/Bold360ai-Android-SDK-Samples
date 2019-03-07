package com.conversation.demo;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import com.conversation.demo.forms.DemoMain;

import static com.conversation.demo.forms.DemoMainKt.DemoMain_TAG;


public class MainActivity extends AppCompatActivity implements DemoMain.FragmentInteraction /*implements ChatEventListener, AccountListener*/ {

    public static final String My_TAG = "MainActivity";

    private ProgressBar progressBar;

    //private Menu menu;

    /*



    private Notifiable notificationsReceiver = new NotificationsReceiver();

    private Map<String, AccountInfo> accounts = new HashMap<>();

*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        progressBar = findViewById(R.id.progressBar);


        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        fragmentTransaction.replace(R.id.content_main, DemoMain.newInstance(), DemoMain_TAG);
        fragmentTransaction.commit();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
       // this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            case R.id.bold_end:

                return false;
            default:
                break;
        }

        return false;
    }

    /*private void clearsAllResources() {
        try {
            chatController.unsubscribeNotifications(notificationsReceiver);
            chatController.terminateChat();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }*/

    /*@Override
    public void onStop() {
        if (isFinishing()) {
            clearsAllResources();
        }

        super.onStop();
    }*/

    @Override
    public void onBackPressed() {
        showWaitIndication(View.GONE);
        super.onBackPressed();
    }

    @Override
    public void enableWaiting(boolean enable) {
        showWaitIndication(enable? View.VISIBLE : View.GONE);
    }

    private void showWaitIndication(int state) {
        progressBar.setVisibility(state);
    }

}

