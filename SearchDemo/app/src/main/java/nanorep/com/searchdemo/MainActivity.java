package nanorep.com.searchdemo;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nanorep.nanoclient.*;
import com.nanorep.nanoclient.Channeling.NRChanneling;
import com.nanorep.nanoclient.Interfaces.NRExtraDataListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import nanorep.com.searchdemo.CustomViews.CustomChannelingView;
import nanorep.com.searchdemo.CustomViews.CustomLikeViewIcons;
import nanorep.nanowidget.Components.AbstractViews.NRCustomChannelView;
import nanorep.nanowidget.Components.AbstractViews.NRCustomLikeView;
import nanorep.nanowidget.Components.AbstractViews.ViewIdsFactory;
import nanorep.nanowidget.Components.AbstractViews.dislikeDialog.DislikeConfiguration;
import nanorep.nanowidget.Components.AbstractViews.dislikeDialog.NRCustomDislikeDialog;
import nanorep.nanowidget.Components.NRDislikeDialog;
import nanorep.nanowidget.Fragments.DeepLinkFragment;
import nanorep.nanowidget.Fragments.NRMainFragment;
import nanorep.nanowidget.SearchInjector;
import nanorep.nanowidget.SearchViewsProvider;

public class MainActivity extends AppCompatActivity  implements Nanorep.NanoRepWidgetListener {

    private ProgressBar progressBar;
    private SearchViewsProvider myViewsProvider;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(nanorep.com.searchdemo.R.layout.activity_main);

        // Init and set the views:

        final EditText accountNameEditText = (EditText) findViewById(R.id.acoontNameId);
        final EditText knowledgeBaseEditText = (EditText) findViewById(R.id.kbId);
        final EditText deepLinkingArticleId = findViewById(R.id.deepLinkArticleId);
        final Button openDeepLink = findViewById(R.id.deepLink);
        final TextView versionName = (TextView) findViewById(R.id.versionName);
        final EditText nrContextEditText = (EditText) findViewById(R.id.nr_context);
        final CheckBox labelCheckBox = (CheckBox) findViewById(R.id.checkbox_label_mode);
        final Button goButton = (Button) findViewById(R.id.goButton);

        progressBar = (ProgressBar) findViewById(R.id.pb);

        LinearLayout layout = (LinearLayout) findViewById(R.id.linearlayout);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard(v);
            }
        });

        versionName.setText(BuildConfig.VERSION_NAME);

        deepLinkingArticleId.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() > 0){
                    openDeepLink.setVisibility(View.VISIBLE);
                }
            }
        });

        openDeepLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String accountName = accountNameEditText.getText().toString();
                String knowledgeBase = knowledgeBaseEditText.getText().toString();
                String articleId = deepLinkingArticleId.getText().toString();

                AccountParams params = new AccountParams(accountName, knowledgeBase);

                hideKeyboard(view);

                if (accountName.length() == 0) {
                    Toast.makeText(MainActivity.this, "Please fill in your account", Toast.LENGTH_LONG).show();

                    return;
                }

                if (knowledgeBase.length() == 0) {
                    Toast.makeText(MainActivity.this, "Please fill in your kb", Toast.LENGTH_LONG).show();
                    return;
                }

                if (articleId.length() == 0) {
                    Toast.makeText(MainActivity.this, "Please fill a valid articleId", Toast.LENGTH_LONG).show();
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);

                onDeepLinking(articleId, params);
            }
        });

        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View button) {

                // Get account params from the fields:
                String accountName = accountNameEditText.getText().toString();
                String knowledgeBase = knowledgeBaseEditText.getText().toString();

                if (accountName.length() == 0) {
                    Toast.makeText(MainActivity.this, "Please fill in your account", Toast.LENGTH_LONG).show();
                    return;
                }

                if (knowledgeBase.length() == 0) {
                    Toast.makeText(MainActivity.this, "Please fill in your kb", Toast.LENGTH_LONG).show();
                    return;
                }

                AccountParams accountParams = new AccountParams(accountName, knowledgeBase);

                // Get the selected context:
                // Example: Service: "MY-SEARCH-CONTEXT"
                String nrContext = nrContextEditText.getText().toString();
                if(nrContext.length() > 0) {
                    accountParams.setContext(nrContext);
                }

                // Set the account params settings
                accountParams.setLabelsMode(labelCheckBox.isChecked());
                accountParams.setOpenLinksInternally(false);
                accountParams.setContextExclusivity(false);

                // Init Nanorep using the account params
                Nanorep.initialize(accountParams, MainActivity.this);
                Nanorep.getInstance().setHttpRequestTimeout(15);
                progressBar.setVisibility(View.VISIBLE);
            }
        });

        // Init the views provider for the customer - used to enable the views to be overridden by the customer

        myViewsProvider = new SearchViewsProvider() {
            @Override
            public NRCustomDislikeDialog getDislikeDialog(DislikeConfiguration configuration) {
                NRDislikeDialog dialog = NRDislikeDialog.newInstance(configuration, new ViewIdsFactory(getDislikeDialogLayout(), getDislikeIdsProducer()));
                dialog.setStyle(R.style.CustomDialog, R.style.myTheme);
                dialog.setDialogGravity(Gravity.BOTTOM);
                return dialog;
            }

            @Override
            public int getDislikeDialogLayout() {
                return R.layout.custom_dislike_dialog;
            }

            @Override
            public int getArticleLayout() {
                return R.layout.custom_article_layout;
            }

            @Override
            public int getChannelingLayout() {
                return R.layout.custom_channelings_linearlayout;
            }

            @Override
            public NRCustomLikeView getLikeView(Context context) {
                return new CustomLikeViewIcons(context);
            }

            @Override
            public int getLikeViewLayout() {
                return R.layout.custom_like_view_icons;
            }

            @Override
            public NRCustomChannelView getChannelView(Context context) {
                return new CustomChannelingView(context, this);
            }

            @Override
            public boolean isSearchBarAlwaysOnTop() {
                return false;
            }

            @Override
            public int getLabelItemTitleLayout() {
                return R.layout.custom_title_item_label;
            }
        };
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onConfigurationFetched() {
        progressBar.setVisibility(View.INVISIBLE);
        openMainFragment();
    }

    @Override
    public void onCachedImageRequest(String url, Nanorep.NRCachedImageResponder responder) {

        // fetch bitmap from network, cache, resources or any other way
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.blue_nano_icon);
        responder.onBitmapResponse(bitmap);
    }

    @Override
    public void onInitializationFailure() {
        progressBar.setVisibility(View.INVISIBLE);
        Toast.makeText(this, "Failed connecting to server",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEmptyDataResponse() {
        openMainFragment();
    }


    // handle channeling on the customer side

    @Override
    public void onChannel(NRChanneling.NRChannelingType channelingType, Object extraData) {
        switch (channelingType) {
            case PhoneNumber:
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", (String) extraData, null));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.d("dialerError", "Unable to start dialer");
                }
                break;
        }
    }

    @Override
    public void personalInfoWithExtraData(String channelDescription, String extraData, NRExtraDataListener listener) {

        // Extra data example:

        Log.i("formData", "Got form personal data request");
        Map<String, String> map = new HashMap<>();
        map.put("number", "122222");
        listener.onExtraData(map);
    }

    @Override
    public void onSubmitSupportForm(String formData, ArrayList<String> fileUploadPaths) {

        if (formData != null) {
            Log.i("formData", formData);
        } else {
            Log.e("formSubmitError", "the form result is null");
        }

        if (fileUploadPaths != null) {
            Log.i("filesToUpload", fileUploadPaths.toString());
        } else {
            Log.e("filesToUploadError", "fileUploadPaths is null");
        }
    }


    public void onDeepLinking(String articleId, AccountParams accountParams){

        Map<String, String> map = new HashMap<>();
        map.put("number", "122222");

        DeepLinkFragment deepLinkFragment = DeepLinkFragment.newInstance(articleId, accountParams, new DeepLinkFragment.deepLinkingServicesProvider() {
            @Override
            public Nanorep.NanoRepWidgetListener getWidgetListener() {
                return null;
            }

            @Override
            public SearchViewsProvider getSearchViewsProvider() {
                return myViewsProvider;
            }

        });

        deepLinkFragment.setArticleExtraData(map);

        progressBar.setVisibility(View.INVISIBLE);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_main, deepLinkFragment)
                .commitAllowingStateLoss();
    }

    private void openMainFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_main, NRMainFragment.newInstance(new SearchInjector() {
                    @Override
                    public SearchViewsProvider getUiProvider() {
                        return myViewsProvider;
                    }
                }))
                .commitAllowingStateLoss();
    }
}
