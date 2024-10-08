package com.ads.ads_module;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ConsentHelper {
    public static ConsentHelper instance = null;
    private boolean showingForm = false;
    private ConsentInformation consentInformation;
    SharedPreferences prefs;

    public ConsentHelper(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        consentInformation = UserMessagingPlatform.getConsentInformation(context);
    }

    public static ConsentHelper getInstance(Context context) {
        if (instance == null) {
            instance = new ConsentHelper(context);
        }
        return instance;
    }

    public boolean isGDPR() {
        return prefs.getInt("IABTCF_gdprApplies", 0) == 1;
    }

    public boolean canShowAds() {

        String purposeConsent = prefs.getString("IABTCF_PurposeConsents", "");
        String vendorConsent = prefs.getString("IABTCF_VendorConsents", "");
        String vendorLI = prefs.getString("IABTCF_VendorLegitimateInterests", "");
        String purposeLI = prefs.getString("IABTCF_PurposeLegitimateInterests", "");

        int googleId = 755;
        boolean hasGoogleVendorConsent = hasAttribute(vendorConsent, googleId);
        boolean hasGoogleVendorLI = hasAttribute(vendorLI, googleId);

        return hasConsentFor(Collections.singletonList(1), purposeConsent, hasGoogleVendorConsent)
                && hasConsentOrLegitimateInterestFor(
                Arrays.asList(2, 7, 9, 10),
                purposeConsent,
                purposeLI,
                hasGoogleVendorConsent,
                hasGoogleVendorLI
        );
    }

    public boolean canShowPersonalizedAds() {
        String purposeConsent = prefs.getString("IABTCF_PurposeConsents", "");
        String vendorConsent = prefs.getString("IABTCF_VendorConsents", "");
        String vendorLI = prefs.getString("IABTCF_VendorLegitimateInterests", "");
        String purposeLI = prefs.getString("IABTCF_PurposeLegitimateInterests", "");

        int googleId = 755;
        boolean hasGoogleVendorConsent = hasAttribute(vendorConsent, googleId);
        boolean hasGoogleVendorLI = hasAttribute(vendorLI, googleId);

        return hasConsentFor(Arrays.asList(1, 3, 4), purposeConsent, hasGoogleVendorConsent)
                && hasConsentOrLegitimateInterestFor(
                Arrays.asList(2, 7, 9, 10),
                purposeConsent,
                purposeLI,
                hasGoogleVendorConsent,
                hasGoogleVendorLI
        );
    }

    public boolean isConsent() {
        return canShowAds() || canShowPersonalizedAds();
    }
    public boolean canLoadAndShowAds() {
        return true;
    }

    private boolean hasAttribute(String input, int index) {
        return input.length() >= index && input.charAt(index - 1) == '1';
    }

    private boolean hasConsentFor(List<Integer> purposes, String purposeConsent, boolean hasVendorConsent) {
        for (int p : purposes) {
            if (!hasAttribute(purposeConsent, p) || !hasVendorConsent) {
                return false;
            }
        }
        return true;
    }

    private boolean hasConsentOrLegitimateInterestFor(
            List<Integer> purposes,
            String purposeConsent,
            String purposeLI,
            boolean hasVendorConsent,
            boolean hasVendorLI
    ) {
        for (int p : purposes) {
            if ((hasAttribute(purposeLI, p) && hasVendorLI) || (hasAttribute(purposeConsent, p) && hasVendorConsent)) {
                return true;
            }
        }
        return false;
    }

    public boolean isUpdateConsentButtonRequired(Context context) {
        consentInformation = UserMessagingPlatform.getConsentInformation(context);
        return consentInformation.getPrivacyOptionsRequirementStatus() ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    public void reset() {
        if (consentInformation != null)
            consentInformation.reset();
    }

    public void obtainConsentAndShow(Activity activity, Runnable loadAds, Runnable onError) {
        ConsentRequestParameters params;

//        for debug
        /*ConsentDebugSettings debugSettings = new ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .addTestDeviceHashedId("DCBFEDF00D8E97B95C1EC50330F105D2")
                .build();

        params = new ConsentRequestParameters
                .Builder()
                .setConsentDebugSettings(debugSettings)
                .build();*/
//for release
        params = new ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .build();

        consentInformation = UserMessagingPlatform.getConsentInformation(activity);
        consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                () -> {
                    if (showingForm) {
                        return;
                    }

                    showingForm = true;
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, error -> {
                        showingForm = false;
                        handleConsentResult(activity, consentInformation, loadAds);
                    });
                },
                error -> {
                    onError.run();
                });

    }

    public boolean canRequestAds() {
        return consentInformation.canRequestAds();
    }

    private void handleConsentResult(Activity context, ConsentInformation ci, Runnable loadAds) {
        if (ci.canRequestAds()) {
            logConsentChoices(context);
            loadAds.run();
        } else {
            logConsentChoices(context);
        }
    }

    private void logConsentChoices(Activity context) {
        boolean canShow = canShowAds();
        boolean isEEA = isGDPR();
    }
}