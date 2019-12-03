package nanorep.com.searchdemo;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.nanorep.nanoclient.AccountParams;
import com.nanorep.nanoclient.Channeling.NRChanneling;
import com.nanorep.nanoclient.Connection.NRError;
import com.nanorep.nanoclient.Interfaces.NRExtraDataListener;
import com.nanorep.nanoclient.Nanorep;

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
import nanorep.nanowidget.DataClass.FAQDataSource;
import nanorep.nanowidget.Fragments.DeepLinkFragment;
import nanorep.nanowidget.Fragments.NRMainFragment;
import nanorep.nanowidget.Fragments.ResultsFragment;
import nanorep.nanowidget.SearchInjector;
import nanorep.nanowidget.SearchViewsProvider;

public class MainActivity extends AppCompatActivity {

    private ProgressBar startSDKProgressBar;
    private ProgressBar deepLinkProgressBar;
    private SearchViewsProvider myViewsProvider;
    private Nanorep.NanoRepWidgetListener widgetListener;
    private DeepLinkFragment deepLinkFragment;
    private Nanorep nanorepInstance;
    private Button startSDKButton;
    private Button deepLinkButton;
    private String articleIdForDeepLinking;
    public static Boolean isFromDeepLink = false;
    private Boolean killedBySystem = false;
    private FAQDataSource supportCenterDataSource;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(nanorep.com.searchdemo.R.layout.activity_main);

        initWidgetListener();
        initViewsProvider();

        final EditText accountNameEditText = findViewById(R.id.accountNameEditText);
        final EditText knowledgeBaseEditText = findViewById(R.id.knowledgebaseEditText);
        final EditText apiKeyEditText = findViewById(R.id.apiKeyEditText);
        final EditText nrContextEditText = findViewById(R.id.nrContextEditText);
        final EditText deepLinkingArticleId = findViewById(R.id.deeplinkEditText);

        final RadioGroup searchModes = findViewById(R.id.searchModes);
        final CheckBox openLinksInternallyCheckBox = findViewById(R.id.openLinksInternallyCheckBox);
        final CheckBox contextExclusivity = findViewById(R.id.contextExclusivity);

        final TextView versionName = findViewById(R.id.versionName);
        versionName.setText(nanorep.nanowidget.BuildConfig.VERSION_NAME);


        contextExclusivity.setChecked(false);
        openLinksInternallyCheckBox.setChecked(true);

        startSDKButton = findViewById(R.id.goButton);
        deepLinkButton = findViewById(R.id.deepLinkButton);

        startSDKProgressBar = findViewById(R.id.startSDKProgressBar);
        deepLinkProgressBar = findViewById(R.id.deeplinkProgressBar);

        deepLinkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                enableButtons(false);

                isFromDeepLink = true;

                // Get account params from the fields:
                String accountName = accountNameEditText.getText().toString();
                String knowledgeBase = knowledgeBaseEditText.getText().toString();
                articleIdForDeepLinking = deepLinkingArticleId.getText().toString();
                String apiKey = apiKeyEditText.getText().toString();
                String articleId = deepLinkingArticleId.getText().toString();

                hideKeyboard(view);

                if (accountName.length() == 0) {
                    accountNameEditText.requestFocus();
                    accountNameEditText.setError("Please fill in your account");
                    return;
                }

                if (knowledgeBase.length() == 0) {
                    knowledgeBaseEditText.requestFocus();
                    knowledgeBaseEditText.setError("Please fill in your kb");
                    return;
                }

                if (articleIdForDeepLinking.length() == 0) {
                    deepLinkingArticleId.requestFocus();
                    deepLinkingArticleId.setError("Please fill a valid articleId");
                    return;
                }

                deepLinkProgressBar.setVisibility(View.VISIBLE);
                deepLinkButton.setVisibility(View.INVISIBLE);

                // Set the account params properties
                AccountParams accountParams = new AccountParams(accountName, knowledgeBase);
                accountParams.setOpenLinksInternally(openLinksInternallyCheckBox.isChecked());

                // Init Nanorep using the account params
                initializeNanorep(accountParams);

                accountParams.setApiKey(apiKey);

                accountParams.setOpenLinksInternally(openLinksInternallyCheckBox.isChecked());

                onDeepLinking(articleId);

            }
        });

        startSDKButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View button) {

                enableButtons(false);

                isFromDeepLink = false;

//                accountNameEditText.setText("gojek");
//                knowledgeBaseEditText.setText("English");
//
                accountNameEditText.setText("Skrill");
                knowledgeBaseEditText.setText("---Staging---");
//                knowledgeBaseEditText.setText("Conversational");
                apiKeyEditText.setText("961ce682-9f70-4b9a-b809-2a338714c31b");
//
                // Get the account params from the fields:
                String accountName = accountNameEditText.getText().toString();
                String knowledgeBase = knowledgeBaseEditText.getText().toString();
                String apiKey = apiKeyEditText.getText().toString();

                if (accountName.isEmpty()) {
                    accountNameEditText.requestFocus();
                    accountNameEditText.setError("Please fill in your account");
                    return;
                }

                if (knowledgeBase.isEmpty()) {
                    knowledgeBaseEditText.requestFocus();
                    knowledgeBaseEditText.setError("Please fill in your kb");
                    return;
                }

                AccountParams accountParams = new AccountParams(accountName, knowledgeBase);

                accountParams.setApiKey(apiKey);

                // Get the selected context:
                // For example: Service: "MY-SEARCH-CONTEXT"
                String nrContext = nrContextEditText.getText().toString();
                if(nrContext.length() > 0) {
                    accountParams.setContext(nrContext);
                }

                // Set the account params properties
                switch (searchModes.getCheckedRadioButtonId()) {
                    case R.id.labelsMode:  {
                        accountParams.setLabelsMode(true);
                        accountParams.setSupportCenterMode(false);
                    }
                    break;

                    case R.id.supportCenterMode: {
                        accountParams.setLabelsMode(false);
                        accountParams.setSupportCenterMode(true);
                    }
                    break;

                    default: {
                        accountParams.setLabelsMode(false);
                        accountParams.setSupportCenterMode(false);
                    }
                }

                accountParams.setOpenLinksInternally(openLinksInternallyCheckBox.isChecked());
                accountParams.setContextExclusivity(contextExclusivity.isChecked());
                
                // Init Nanorep using the account params
                initializeNanorep(accountParams);

                startSDKProgressBar.setVisibility(View.VISIBLE);
                startSDKButton.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void initializeNanorep(AccountParams accountParams) {
        Nanorep.initialize(accountParams);
        nanorepInstance = Nanorep.getInstance();
        nanorepInstance.setHttpRequestTimeout(15);
        nanorepInstance.setWidgetListener(widgetListener);
    }

    private void initWidgetListener() {
        widgetListener = new MyWidgetListener();
    }

    private void initViewsProvider() {

        // Search views provider initialization - The views provider is being used to enable the views to be overridden by the customer
        // All the views options are available at the Nanorep search documentation under "customize views"
        // If not set, the customer can use "new SearchInjector.DefaultsInjector().getUiProvider()" to get the default views provider

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
                return new CustomLikeViewIcons(context, this);
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
                return true;
            }

            @Override
            public int getLabelItemLayout() {
                return R.layout.custom_title_item_label;
            }

            @Override
            public boolean showChannelConfirmationDialogs(NRChanneling channeling) {
                return true;
            }

            @Override
            public boolean showFeedbackConfirmationDialogs() {
                return true;
            }
        };
    }

    /**
     * Opens an independent fragment for a given articleId and account params
     * @param articleId
     */
    public void onDeepLinking(String articleId){

        Map<String, String> map = new HashMap<>();
        map.put("number", "122222");

        DeepLinkFragment deepLinkFragment = DeepLinkFragment.newInstance(articleId, new DeepLinkFragment.deepLinkingServicesProvider() {

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

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.activity_content_main, deepLinkFragment, articleId)
                .addToBackStack(null)
                .commit();

        getSupportFragmentManager().executePendingTransactions();

        deepLinkProgressBar.setVisibility(View.INVISIBLE);
        deepLinkButton.setVisibility(View.VISIBLE);
    }

    class MyWidgetListener implements Nanorep.NanoRepWidgetListener {

        /**
         * Nanorep instance is ready to use
         */
        @Override
        public void onConfigurationFetched() {

            if (isFromDeepLink) {
                if (TextUtils.isDigitsOnly(articleIdForDeepLinking)) {
                    onDeepLinking(articleIdForDeepLinking);
                    enableButtons(true);
                } else {
                    Toast.makeText(MainActivity.this, "Invalid ArticleID", Toast.LENGTH_SHORT).show();
                }
            } else {
                openSearchFragment();
                enableButtons(true);
            }
        }

        @Override
        public void onError(NRError error) {
            Log.e("Widget Error", "error location: " + error.getDomain() +", error code: " + error.getCode() + ", error description: " + error.getDescription());
        }

        /**
         * Fetch bitmap from network, cache or other resources to use at the labels menu
         * @param url
         * @param responder
         */
        @Override
        public void onCachedImageRequest(String url, Nanorep.NRCachedImageResponder responder) {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.blue_nano_icon);
            responder.onBitmapResponse(bitmap);
        }

        @Override
        public void onInitializationFailure() {
            startSDKProgressBar.setVisibility(View.INVISIBLE);
            startSDKButton.setVisibility(View.VISIBLE);
            Toast.makeText(MainActivity.this, "Failed connecting with server", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onEmptyDataResponse() {
            openSearchFragment();
        }

        /**
         * Handle channeling on the customer side
         * @param channelingType
         * @param extraData
         */
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

        /**
         * A function that gives the ability of adding extra data to the channels
         * @param channelDescription
         * @param extraData
         * @param listener
         */
        @Override
        public void personalInfoWithExtraData(String channelDescription, String extraData, NRExtraDataListener listener) {

            // Extra data example:
            Log.i("formData", "Got form personal data request");
            Map<String, String> map = new HashMap<>();
            map.put("number", "122222");
            listener.onExtraData(map);
        }

        /**
         * A callback of a form submission
         * @param formData
         * @param fileUploadPaths
         */
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
    };

    private void openSearchFragment() {

        FragmentManager fragmentManager = getSupportFragmentManager();

        if (fragmentManager == null) {
            return;
        }

        View view = getCurrentFocus();
        if (view != null) {
            view.clearFocus();
        }

        if (Nanorep.getInstance().getAccountParams().isLabelsMode()) {

            NRMainFragment mainFragment = NRMainFragment.newInstance(new SearchInjector() {
                @Override
                public SearchViewsProvider getUiProvider() {
                    return myViewsProvider;
                }
            });

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.activity_content_main, mainFragment, NRMainFragment.TAG)
                    .addToBackStack(null)
                    .commit();

        }  else if (Nanorep.getInstance().getAccountParams().isSupportCenterMode()){

            supportCenterDataSource = new FAQDataSource();

            fragmentManager
                .beginTransaction()
                .replace(R.id.activity_content_main, ResultsFragment.newInstance(null, supportCenterDataSource, myViewsProvider),  ResultsFragment.TAG)
                .addToBackStack(null)
                .commit();

        } else { // Start with Faqs mode

            fragmentManager
                .beginTransaction()
                .replace(R.id.activity_content_main, ResultsFragment.newInstance(new SearchInjector() {
                    @Override
                    public SearchViewsProvider getUiProvider() {
                        return myViewsProvider;
                    }
                }),  ResultsFragment.TAG)
                .addToBackStack(null)
                .commit();
        }

        startSDKProgressBar.setVisibility(View.INVISIBLE);
        startSDKButton.setVisibility(View.VISIBLE);
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    void enableButtons(boolean enable) {
        startSDKButton.setClickable(enable);
        deepLinkButton.setClickable(enable);
    }


    @Override
    protected void onDestroy() {

        if (nanorepInstance != null) {
            nanorepInstance.clearSession();
            nanorepInstance = null;
        }

        if (!killedBySystem) {
            Nanorep.clearInstance();
            if (supportCenterDataSource != null && getSupportFragmentManager().findFragmentByTag(ResultsFragment.TAG) == null) {
                supportCenterDataSource.onDestroy();
            }

            super.onDestroy();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        killedBySystem = true;
        super.onSaveInstanceState(outState);
    }
}
