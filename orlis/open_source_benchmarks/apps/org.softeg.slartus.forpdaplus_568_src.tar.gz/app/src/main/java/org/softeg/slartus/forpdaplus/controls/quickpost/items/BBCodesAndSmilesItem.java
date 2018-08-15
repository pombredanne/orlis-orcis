package org.softeg.slartus.forpdaplus.controls.quickpost.items;/*
 * Created by slinkin on 18.04.2014.
 */

import android.content.Context;

import org.softeg.slartus.forpdaplus.controls.quickpost.QuickPostItem;

public class BBCodesAndSmilesItem extends QuickPostItem {
    private BBCodesAndSmilesQuickView view;
    @Override
    public String getTitle() {
        return "BB-коды и смайлы";
    }

    @Override
    public String getName() {
        return "bbcodes_and_emotics";
    }

    @Override
    public BaseQuickView createView(Context context) {
        view = new BBCodesAndSmilesQuickView(context);
        return view;
    }

    @Override
    public BaseQuickView getBaseQuickView() {
        return view;
    }
}
