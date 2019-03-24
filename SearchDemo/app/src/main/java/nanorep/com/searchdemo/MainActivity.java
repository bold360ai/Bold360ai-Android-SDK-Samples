package nanorep.com.searchdemo;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import nanorep.nanowidget.Fragments.DeepLinkFragment;
import nanorep.nanowidget.Fragments.NRMainFragment;
import nanorep.nanowidget.SearchInjector;
import nanorep.nanowidget.SearchViewsProvider;

public class MainActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private SearchViewsProvider myViewsProvider;
    private Nanorep.NanoRepWidgetListener widgetListener;
    private DeepLinkFragment deepLinkFragment;
    private Nanorep nanorepInstance;
    private Button startNanorepButton;
    private Button deepLinkButton;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(nanorep.com.searchdemo.R.layout.activity_main);

        initWidgetListener();
        initViewsProvider();

        final EditText accountNameEditText = findViewById(R.id.accountNameEditText);
        final EditText knowledgeBaseEditText = findViewById(R.id.knowledgebaseEditText);
        final EditText nrContextEditText = findViewById(R.id.nrContextEditText);
        final CheckBox labelCheckBox = findViewById(R.id.labelModeCheckbox);

        final EditText deepLinkingArticleId = findViewById(R.id.deepLinkEditText);

        startNanorepButton = findViewById(R.id.startNanorepButton);
        deepLinkButton = findViewById(R.id.openDeepLinkButton);

        progressBar = findViewById(R.id.progressBar);
        final TextView versionName = findViewById(R.id.versionName);

        versionName.setText(BuildConfig.VERSION_NAME);

        deepLinkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String accountName = accountNameEditText.getText().toString();
                String knowledgeBase = knowledgeBaseEditText.getText().toString();
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

                if (articleId.length() == 0) {
                    deepLinkingArticleId.requestFocus();
                    deepLinkingArticleId.setError("Please fill a valid articleId");
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);

                AccountParams params = new AccountParams(accountName, knowledgeBase);

                onDeepLinking(articleId, params);
            }
        });

        startNanorepButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View button) {

                // Get account params from the fields:
                String accountName = accountNameEditText.getText().toString();
                String knowledgeBase = knowledgeBaseEditText.getText().toString();

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

                AccountParams accountParams = new AccountParams(accountName, knowledgeBase);
                
                // Get the selected context:
                // For example: Service: "MY-SEARCH-CONTEXT"
                String nrContext = nrContextEditText.getText().toString();
                if(nrContext.length() > 0) {
                    accountParams.setContext(nrContext);
                }

                // Set the account params properties
                accountParams.setLabelsMode(labelCheckBox.isChecked());
                accountParams.setOpenLinksInternally(false);
                accountParams.setContextExclusivity(false);

                // Init Nanorep using the account params
               initializeNanorep(accountParams);

                progressBar.setVisibility(View.VISIBLE);
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
                return false;
            }

            @Override
            public int getLabelItemTitleLayout() {
                return R.layout.custom_title_item_label;
            }

            @Override
            public boolean showChannelConfirmationDialogs(NRChanneling channeling) {
                return false;
            }

            @Override
            public boolean showFeedbackConfirmationDialogs() {
                return false;
            }
        };
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Opens an independent fragment for a given articleId and account params
     * @param articleId
     * @param accountParams
     */
    public void onDeepLinking(String articleId, AccountParams accountParams){

        Map<String, String> map = new HashMap<>();
        map.put("number", "122222");


      /*
        An example creates a deepLinkFragment with that gets an initialized Nanorep instance
        initializeNanorep(accountParams);
        Nanorep nanorepInstance = Nanorep.getInstance();

        deepLinkFragment = DeepLinkFragment.newInstance(articleId, nanorepInstance, new DeepLinkFragment.ServicesProvider(){

            @Override
            public Nanorep.NanoRepWidgetListener getWidgetListener() {
                return widgetListener;
            }

            @Override
            public SearchViewsProvider getSearchViewsProvider() {
                return myViewsProvider;
            }
        });*/

        // An example creates a deepLinkFragment with that creates a Nanorep instance and initiates its own widget listener
        deepLinkFragment = DeepLinkFragment.newInstance(articleId, accountParams, myViewsProvider);

        deepLinkFragment.setArticleExtraData(map);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_main, deepLinkFragment)
                .commitAllowingStateLoss();

        deepLinkButton.setClickable(true);
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

    class MyWidgetListener implements Nanorep.NanoRepWidgetListener {

        public Boolean isFromDeepLink() {
            return false;
        }

        /**
         * Nanorep instance is ready to use
         */
        @Override
        public void onConfigurationFetched() {
            progressBar.setVisibility(View.INVISIBLE);
            if (isFromDeepLink()) {
                deepLinkFragment.fetchAnswer();
            } else {
                openMainFragment();
            }
        }

        @Override
        public void onError(NRError error) {
            Log.e("Widget Error", "error location: " + error.getDomain() +", error code: " + error.getCode() + ", error description: " + error.getDescription());

            if (isFromDeepLink()) {
                getFragmentManager().popBackStack();
                nanorepInstance.clearSession();
                Nanorep.clearInstance();
            }
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
            progressBar.setVisibility(View.INVISIBLE);
            startNanorepButton.setVisibility(View.VISIBLE);
            Log.e("Initialization", "Failed connecting to server");
        }

        @Override
        public void onEmptyDataResponse() {
            openMainFragment();
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

}
