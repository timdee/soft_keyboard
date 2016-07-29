package com.anysoftkeyboard.ui.settings;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.AsyncTaskCompat;
import android.support.v7.graphics.Palette;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.anysoftkeyboard.theme.KeyboardTheme;
import com.anysoftkeyboard.theme.KeyboardThemeFactory;
import com.anysoftkeyboard.ui.settings.setup.SetUpKeyboardWizardFragment;
import com.anysoftkeyboard.ui.settings.setup.SetupSupport;
import com.anysoftkeyboard.ui.tutorials.ChangeLogFragment;
import com.anysoftkeyboard.utils.Log;
import com.menny.android.anysoftkeyboard.BuildConfig;
import com.menny.android.anysoftkeyboard.R;

import net.evendanan.chauffeur.lib.FragmentChauffeurActivity;
import net.evendanan.chauffeur.lib.experiences.TransitionExperiences;

public class MainFragment extends Fragment {

    private static final String TAG = "MainFragment";
    private AnimationDrawable mNotConfiguredAnimation = null;
    private AsyncTask<Drawable, Void, Palette.Swatch> mPaletteTask;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.main_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState == null) {
            //I to prevent leaks and duplicate ID errors, I must use the getChildFragmentManager
            //to add the inner fragments into the UI.
            //See: https://github.com/AnySoftKeyboard/AnySoftKeyboard/issues/285
            FragmentManager fragmentManager = getChildFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.change_log_fragment, new ChangeLogFragment.CardedChangeLogFragment())
                    .commit();
        }
        View testingView = view.findViewById(R.id.testing_build_message);
        testingView.setVisibility(BuildConfig.TESTING_BUILD? View.VISIBLE : View.GONE);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        //I'm doing the setup of the link in onViewStateRestored, since the links will be restored too
        //and they will probably refer to a different scoop (Fragment).
        //setting up the underline and click handler in the keyboard_not_configured_box layout
        TextView clickHere = (TextView) getView().findViewById(R.id.not_configured_click_here);
        mNotConfiguredAnimation = clickHere.getVisibility() == View.VISIBLE ?
                (AnimationDrawable) clickHere.getCompoundDrawables()[0] : null;

        String fullText = getString(R.string.not_configured_with_click_here);
        String justClickHereText = getString(R.string.not_configured_with_just_click_here);
        SpannableStringBuilder sb = new SpannableStringBuilder(fullText);
        // Get the index of "click here" string.
        int start = fullText.indexOf(justClickHereText);
        int length = justClickHereText.length();
        if (start == -1) {
            //this could happen when the localization is not correct
            start = 0;
            length = fullText.length();
        }
        ClickableSpan csp = new ClickableSpan() {
            @Override
            public void onClick(View v) {
                FragmentChauffeurActivity activity = (FragmentChauffeurActivity) getActivity();
                activity.addFragmentToUi(new SetUpKeyboardWizardFragment(), TransitionExperiences.DEEPER_EXPERIENCE_TRANSITION);
            }
        };
        sb.setSpan(csp, start, start + length, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        clickHere.setMovementMethod(LinkMovementMethod.getInstance());
        clickHere.setText(sb);

        ClickableSpan gplusLink = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.main_site_url)));
                try {
                    startActivity(browserIntent);
                } catch (ActivityNotFoundException weirdException) {
                    //https://github.com/AnySoftKeyboard/AnySoftKeyboard/issues/516
                    //this means that there is nothing on the device
                    //that can handle Intent.ACTION_VIEW with "https" schema..
                    //silently swallowing it
                    Log.w(TAG, "Can not open '%' since there is nothing on the device that can handle it.", browserIntent.getData());
                }
            }
        };
        setupLink(getView(), R.id.ask_gplus_link, gplusLink, false);

        ClickableSpan openSettingsLink = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                MainSettingsActivity mainSettingsActivity = (MainSettingsActivity) getActivity();
                mainSettingsActivity.openDrawer();
            }
        };
        setupLink(getView(), R.id.open_settings_view, openSettingsLink, false);
    }

    public static void setupLink(View root, int showMoreLinkId, ClickableSpan clickableSpan, boolean reorderLinkToLastChild) {
        TextView clickHere = (TextView) root.findViewById(showMoreLinkId);
        if (reorderLinkToLastChild) {
            ViewGroup rootContainer = (ViewGroup) root;
            rootContainer.removeView(clickHere);
            rootContainer.addView(clickHere);
        }

        SpannableStringBuilder sb = new SpannableStringBuilder(clickHere.getText());
        sb.clearSpans();//removing any previously (from instance-state) set click spans.
        sb.setSpan(clickableSpan, 0, clickHere.getText().length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        clickHere.setMovementMethod(LinkMovementMethod.getInstance());
        clickHere.setText(sb);
    }

    @Override
    public void onStart() {
        super.onStart();
        MainSettingsActivity.setActivityTitle(this, getString(R.string.how_to_pointer_title));

        View notConfiguredBox = getView().findViewById(R.id.not_configured_click_here);
        //checking if the IME is configured
        final Context context = getActivity().getApplicationContext();

        if (SetupSupport.isThisKeyboardSetAsDefaultIME(context)) {
            notConfiguredBox.setVisibility(View.GONE);
        } else {
            notConfiguredBox.setVisibility(View.VISIBLE);
        }

        //updating the keyboard layout to the current theme screen shot (if exists).
        KeyboardTheme theme = KeyboardThemeFactory.getCurrentKeyboardTheme(getActivity().getApplicationContext());
        if (theme == null)
            theme = KeyboardThemeFactory.getFallbackTheme(getActivity().getApplicationContext());
        Drawable themeScreenShot = theme.getScreenshot();

        ImageView screenShotHolder = (ImageView) getView().findViewById(R.id.keyboard_screen_shot);
        if (themeScreenShot == null)
            themeScreenShot = ContextCompat.getDrawable(getActivity(), R.drawable.lean_dark_theme_screenshot);
        screenShotHolder.setImageDrawable(themeScreenShot);
        mPaletteTask = new AsyncTask<Drawable, Void, Palette.Swatch>() {
            @Override
            protected Palette.Swatch doInBackground(Drawable... params) {
                Bitmap bitmap = drawableToBitmap(params[0]);
                Palette p = Palette.from(bitmap).generate();
                Palette.Swatch highestSwatch = null;
                for (Palette.Swatch swatch : p.getSwatches()) {
                    if (highestSwatch == null || highestSwatch.getPopulation() < swatch.getPopulation())
                        highestSwatch = swatch;
                }
                return highestSwatch;
            }

            @Override
            protected void onPostExecute(Palette.Swatch swatch) {
                super.onPostExecute(swatch);
                if (!isCancelled()) {
                    final View rootView = getView();
                    if (swatch != null && rootView != null) {
                        final int backgroundRed = Color.red(swatch.getRgb());
                        final int backgroundGreed = Color.green(swatch.getRgb());
                        final int backgroundBlue = Color.blue(swatch.getRgb());
                        final int backgroundColor = Color.argb(200/*~80% alpha*/, backgroundRed, backgroundGreed, backgroundBlue);
                        TextView gplusLink = (TextView) rootView.findViewById(R.id.ask_gplus_link);
                        gplusLink.setTextColor(swatch.getTitleTextColor());
                        gplusLink.setBackgroundColor(backgroundColor);

                        TextView subtitle = (TextView) rootView.findViewById(R.id.main_settings_hero_sub_title);
                        subtitle.setTextColor(swatch.getBodyTextColor());
                        subtitle.setBackgroundColor(backgroundColor);
                    }
                }
            }
        };
        AsyncTaskCompat.executeParallel(mPaletteTask, themeScreenShot);

        if (mNotConfiguredAnimation != null)
            mNotConfiguredAnimation.start();
    }

    @Override
    public void onStop() {
        super.onStop();
        mPaletteTask.cancel(false);
        mPaletteTask = null;
    }

    private static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}