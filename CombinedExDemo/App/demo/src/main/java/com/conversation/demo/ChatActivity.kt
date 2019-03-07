package com.conversation.demo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import com.conversation.demo.forms.DemoMain
import com.conversation.demo.forms.DemoMain_TAG
import com.nanorep.convesationui.structure.controller.ChatEventListener
import com.nanorep.nanoengine.Account
import kotlinx.android.synthetic.main.activity_main.*

/**
 * This interface must be implemented by activities that contain this
 * fragment to allow an interaction in this fragment to be communicated
 * to the activity and potentially other fragments contained in that
 * activity.
 *
 *
 * See the Android Training lesson [Communicating with Other Fragments]
 * (http://developer.android.com/training/basics/fragments/communicating.html)
 * for more information.
 */
interface ChatFlowHandler : ChatEventListener {
    fun waitingVisibility(visible: Boolean)
    fun onAccountReady(account: Account)
}

class ChatActivity : AppCompatActivity(), ChatFlowHandler {

    //private var progressBar: ProgressBar? = null

    //private Menu menu;

    /*



    private Notifiable notificationsReceiver = new NotificationsReceiver();

    private Map<String, AccountInfo> accounts = new HashMap<>();

*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        //progressBar = findViewById(R.id.progressBar)


        val fm = supportFragmentManager
        val fragmentTransaction = fm.beginTransaction()
        fragmentTransaction.replace(R.id.content_main, DemoMain.newInstance(), DemoMain_TAG)
        fragmentTransaction.commit()

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        // this.menu = menu;
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> return true
            R.id.bold_end ->

                return false
            else -> {
            }
        }

        return false
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

    override fun onBackPressed() {
        showWaitIndication(View.GONE)
        super.onBackPressed()
    }

    //@Override
    fun enableWaiting(enable: Boolean) {
        showWaitIndication(if (enable) View.VISIBLE else View.GONE)
    }

    private fun showWaitIndication(state: Int) {
        progressBar.visibility = state
    }

    companion object {

        val My_TAG = "ChatActivity"
    }

}

