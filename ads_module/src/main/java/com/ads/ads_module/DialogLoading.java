package com.ads.ads_module;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

public class DialogLoading extends Dialog {


    public DialogLoading(Context context) {
        super(context, R.style.AppTheme);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_ad_loading);
    }
}
