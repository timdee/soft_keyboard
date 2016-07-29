/*
 * Copyright (c) 2013 Menny Even-Danan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.anysoftkeyboard.ui.settings.widget;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.AbsSavedState;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.TextView;

import com.anysoftkeyboard.addons.AddOn;
import com.anysoftkeyboard.addons.AddOnsFactory;
import com.anysoftkeyboard.addons.IconHolder;
import com.anysoftkeyboard.addons.ScreenshotHolder;
import com.menny.android.anysoftkeyboard.R;

import net.evendanan.pushingpixels.Banner;
import net.evendanan.pushingpixels.ListPreference;

import java.util.ArrayList;
import java.util.List;

/*using this import requires using the Android Library from https://github.com/menny/PushingPixels*/

public class AddOnListPreference extends ListPreference {

    public static <E extends AddOn> void populateAddOnListPreference(AddOnListPreference preference,
                                                                     List<E> list, AddOn selectedAddOn) {
        AddOn[] addOns = new AddOn[list.size()];
        list.toArray(addOns);
        preference.setAddOnsList(addOns);
        preference.setSelectedAddOn(selectedAddOn);
    }

    @Nullable
    private AddOn[] mAddOns;
    @Nullable
    private AddOn mSelectedAddOn;

    public AddOnListPreference(Context context) {
        super(context);
    }

    public AddOnListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        if (mAddOns != null) {
            // mAddOns is null happens when activity gets recreated, e.g. on
            // rotating the device.
            ListAdapter listAdapter = new AddOnArrayAdapter(getContext(),
                    R.layout.addon_list_item_pref, mAddOns);

            builder.setAdapter(listAdapter, this);
        }
        super.onPrepareDialogBuilder(builder);
    }

    public void setAddOnsList(@NonNull AddOn[] addOns) {
        mAddOns = addOns;

        String[] ids = new String[mAddOns.length];
        String[] names = new String[mAddOns.length];
        int entryPos = 0;
        for (AddOn addOn : mAddOns) {
            ids[entryPos] = addOn.getId();
            names[entryPos] = addOn.getName();
            entryPos++;
        }
        setEntries(names);
        setEntryValues(ids);
    }

    private class AddOnArrayAdapter extends ArrayAdapter<AddOn> implements
            OnClickListener {

        public AddOnArrayAdapter(Context context, int textViewResourceId,
                                 AddOn[] objects) {
            super(context, textViewResourceId, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final AddOn addOn = getItem(position);
            final View row;
            if (convertView == null) {
                // inflate layout
                LayoutInflater inflater = LayoutInflater.from(getContext());
                row = inflater.inflate(R.layout.addon_list_item_pref, parent, false);
            } else {
                row = convertView;
            }

            row.setTag(addOn);

            // set on click listener for row
            row.setOnClickListener(this);

            // set addon details
            TextView title = (TextView) row.findViewById(R.id.addon_title);
            title.setText(addOn.getName());
            TextView description = (TextView) row
                    .findViewById(R.id.addon_description);
            description.setText(addOn.getDescription());
            Drawable icon = null;
            if (addOn instanceof IconHolder) {
                IconHolder iconHolder = (IconHolder) addOn;
                icon = iconHolder.getIcon();
            }
            if (icon == null) {
                try {
                    PackageManager packageManager = getContext().getPackageManager();
                    PackageInfo packageInfo = packageManager.getPackageInfo(addOn.getPackageName(), 0);
                    icon = packageInfo.applicationInfo.loadIcon(packageManager);
                } catch (PackageManager.NameNotFoundException e) {
                    icon = null;
                }
            }

            ImageView addOnIcon = (ImageView) row
                    .findViewById(R.id.addon_image);
            addOnIcon.setImageDrawable(icon);

            View more = row.findViewById(R.id.addon_image_more_overlay);

            if (addOn instanceof ScreenshotHolder) {
                if (((ScreenshotHolder) addOn).hasScreenshot()) {
                    addOnIcon.setOnClickListener(this);
                    addOnIcon.setTag(addOn);
                    more.setVisibility(View.VISIBLE);
                } else {
                    more.setVisibility(View.GONE);
                }
            }

            // set checkbox
            RadioButton tb = (RadioButton) row
                    .findViewById(R.id.addon_checkbox);
            tb.setClickable(false);
            if (mSelectedAddOn != null)
                tb.setChecked(addOn.getId().equals(mSelectedAddOn.getId()));
            else
                tb.setChecked(false);

            return row;
        }

        public void onClick(View v) {
            if (v.getId() == R.id.addon_list_item_layout) {
                AddOn newSelectedAddOn = (AddOn) v.getTag();
                setSelectedAddOn(newSelectedAddOn);
                AddOnListPreference.this.setValue(newSelectedAddOn.getId());
                Dialog dialog = getDialog();
                if (dialog != null)
                    dialog.dismiss();// it is null if the dialog is not shown.
            } else if (v.getId() == R.id.addon_image) {
                // showing a screenshot (if available)
                AddOn addOn = (AddOn) v.getTag();
                Drawable screenshot = null;
                if (addOn instanceof ScreenshotHolder) {
                    ScreenshotHolder holder = (ScreenshotHolder) addOn;
                    screenshot = holder.getScreenshot();
                }
                if (screenshot == null) {
                    screenshot = ((ImageView) v).getDrawable();
                }
                //
                if (screenshot == null)
                    return;
                // inflating the screenshot view
                LayoutInflater inflator = LayoutInflater.from(getContext());
                ViewGroup layout = (ViewGroup) inflator.inflate(R.layout.addon_screenshot, null);
                final PopupWindow popup = new PopupWindow(getContext());
                popup.setContentView(layout);
                DisplayMetrics dm = getContext().getResources()
                        .getDisplayMetrics();
                popup.setWidth(dm.widthPixels);
                popup.setHeight(dm.heightPixels);
                popup.setAnimationStyle(R.style.AddonScreenshotPopupAnimation);
                layout.findViewById(R.id.addon_screenshot_close)
                        .setOnClickListener(new OnClickListener() {
                            public void onClick(View v) {
                                popup.dismiss();
                            }
                        });
                ((Banner)layout.findViewById(R.id.addon_screenshot)).setImageDrawable(screenshot);
                popup.showAtLocation(v, Gravity.CENTER, 0, 0);
            }
        }
    }

    public void setSelectedAddOn(AddOn currentSelectedAddOn) {
        mSelectedAddOn = currentSelectedAddOn;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superP = super.onSaveInstanceState();
        if (mSelectedAddOn == null || mAddOns == null) {
            //no state to save
            return superP;
        } else {
            AddOnsListSavedState myState = new AddOnsListSavedState(superP);

            //some add-ons can be none.
            myState.selectedAddOnId = mSelectedAddOn.getId();
            String[] addOns = new String[mAddOns.length];
            for (int i = 0; i < addOns.length; i++)
                addOns[i] = mAddOns[i].getId();
            myState.addOnIds = addOns;

            return myState;
        }
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !(state instanceof AddOnsListSavedState)) {
            // although, this should not happen
            super.onRestoreInstanceState(state);
        } else {
            AddOnsListSavedState myState = (AddOnsListSavedState) state;
            String selectedAddOnId = myState.selectedAddOnId;
            String[] addOnIds = myState.addOnIds;
            //now, this is tricky: it is possible that by the time we need to restore the state
            //some of the add-ons (and even the default) will no longer exist!
            //this can happen when going out of the App to uninstall an add-on, and then returning to the App.
            List<AddOn> validAddOns = new ArrayList<>(addOnIds.length);
            for (String addOnId : addOnIds) {
                AddOn addOn = AddOnsFactory.locateAddOn(addOnId, getContext().getApplicationContext());
                if (addOn != null) {
                    validAddOns.add(addOn);
                }
            }

            AddOn[] addOns = new AddOn[validAddOns.size()];
            validAddOns.toArray(addOns);
            setAddOnsList(addOns);
            AddOn selectedAddOn = AddOnsFactory.locateAddOn(selectedAddOnId, getContext().getApplicationContext());
            if (selectedAddOn == null && validAddOns.size() > 0) {
                selectedAddOn = validAddOns.get(0);
            }
            setSelectedAddOn(selectedAddOn);

            super.onRestoreInstanceState(myState.getSuperState());
        }
    }

    private static class AddOnsListSavedState extends AbsSavedState {

        String selectedAddOnId;
        String[] addOnIds;

        public AddOnsListSavedState(Parcel source) {
            super(source);
            selectedAddOnId = source.readString();
            int addOnCount = source.readInt();
            addOnIds = new String[addOnCount];
            source.readStringArray(addOnIds);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(selectedAddOnId);
            dest.writeInt(addOnIds.length);
            dest.writeStringArray(addOnIds);
        }

        public AddOnsListSavedState(Parcelable superState) {
            super(superState);
        }

        public static final Parcelable.Creator<AddOnsListSavedState> CREATOR = new Parcelable.Creator<AddOnsListSavedState>() {
            public AddOnsListSavedState createFromParcel(Parcel in) {
                return new AddOnsListSavedState(in);
            }

            public AddOnsListSavedState[] newArray(int size) {
                return new AddOnsListSavedState[size];
            }
        };
    }
}
