package nanorep.com.searchdemo;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import nanorep.com.searchdemo.CustomViews.CustomChannelingView;
import nanorep.com.searchdemo.CustomViews.CustomLikeViewIcons;

import com.nanorep.nanoclient.*;
import com.nanorep.nanoclient.Channeling.NRChanneling;
import com.nanorep.nanoclient.Interfaces.NRExtraDataListener;

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

        final EditText accountNameEditText = findViewById(R.id.accountNameEditText);
        final EditText knowledgeBaseEditText = findViewById(R.id.knowledgebaseEditText);
        final EditText nrContextEditText = findViewById(R.id.nrContextEditText);
        final CheckBox labelCheckBox = findViewById(R.id.labelModeCheckbox);

        final EditText deepLinkingArticleId = findViewById(R.id.deepLinkEditText);

        final Button startNanorepButton = findViewById(R.id.startNanorepButton);
        final Button deepLinkButton = findViewById(R.id.openDeepLinkButton);

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
                Nanorep.initialize(accountParams, MainActivity.this);
                Nanorep.getInstance().setHttpRequestTimeout(15);

                progressBar.setVisibility(View.VISIBLE);
            }
        });

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
     * Nanorep instance is ready to use
     */
    @Override
    public void onConfigurationFetched() {
        progressBar.setVisibility(View.INVISIBLE);
        openMainFragment();
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
        Toast.makeText(this, "Failed connecting to server",Toast.LENGTH_SHORT).show();
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

    /**
     * Opens an independent fragment for a given articleId and account params
     * @param articleId
     * @param accountParams
     */
    public void onDeepLinking(String articleId, AccountParams accountParams){

        // Extra data sample:
        Map<String, String> map = new HashMap<>();
        map.put("number", "122222");

        // DeepLinkFragment instance:
        // deepLinkingServicesProvider injects the widget listener and the search provider to the fragment, it can be null
        DeepLinkFragment deepLinkFragment = DeepLinkFragment.newInstance(articleId, accountParams, new DeepLinkFragment.deepLinkingServicesProvider() {

            // The widget listener can be null - in that case the fragment creates its own listener.
            @Override
            public Nanorep.NanoRepWidgetListener getWidgetListener() {
                return null;
            }

            // The search views provider can be null - in that case the fragment takes Nanorep's default views
            @Override
            public SearchViewsProvider getSearchViewsProvider() {
                return myViewsProvider;
            }

        });

        // sets the extra data to the fragment instance (optional)
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
