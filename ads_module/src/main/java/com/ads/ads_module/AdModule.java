package com.ads.ads_module;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.facebook.appevents.AppEventsConstants;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.appmetrica.analytics.AppMetrica;
import io.appmetrica.analytics.AppMetricaConfig;

public class AdModule {
    private static final String TAG = AdModule.class.getSimpleName();
    private static AdModule instance;

    private DialogLoading dialog;

    private Map<String, String> adIdsToNameMap = new HashMap<>();

    public static AdModule getInstance() {
        if (instance == null) {
            instance = new AdModule();
        }
        return instance;
    }

    private AdModule() {

    }

    public Map<String, String> getAdIdsToNameMap() {
        return adIdsToNameMap;
    }

    public void setAdIdsToNameMap(Map<String, String> adIdsToNameMap) {
        this.adIdsToNameMap = adIdsToNameMap;
    }

    public void init(Context context, List<String> testDeviceList, Map<String, String> adIds, String appmetrica) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            String processName = Application.getProcessName();
            String packageName = context.getPackageName();
            if (!packageName.equals(processName)) {
                WebView.setDataDirectorySuffix(processName);
            }
        }
        MobileAds.initialize(context, initializationStatus -> {
        });
        MobileAds.setRequestConfiguration(new RequestConfiguration.Builder().setTestDeviceIds(testDeviceList).build());
        setAdIdsToNameMap(adIds);
        // Khởi tạo AppMetrica
        AppMetricaConfig config = AppMetricaConfig.newConfigBuilder(appmetrica).build();
        AppMetrica.activate(context, config);

    }

    private void createDialog(Context context) {
        dialog = new DialogLoading(context);
        dialog.setCancelable(false);
    }

    public AdRequest getAdRequest() {
        AdRequest.Builder builder = new AdRequest.Builder();
        return builder.build();
    }

    private boolean isOpenSplash = false;

    private void checkTimeoutSplash(long timeout, AdCallback adCallback) {
        new Handler().postDelayed(() -> {
            if (!isOpenSplash) {
                adCallback.doNextAction();
                isOpenSplash = true;
            }
        }, timeout);
    }

    public void loadAndShowSplash(Activity context, String id, AdCallback adCallback, long timeout) {
        checkTimeoutSplash(timeout, adCallback);
        InterstitialAd.load(context, id, getAdRequest(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        if (!isOpenSplash) {
                            new Handler().postDelayed(() -> {
                                if (!isOpenSplash) {
                                    if (!context.isDestroyed()) {
                                        showInter(context, interstitialAd, adCallback);
                                    }
                                    isOpenSplash = true;
                                }
                            }, 1800);
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        if (adCallback != null)
                            adCallback.onAdFailedToLoad(loadAdError);
                    }
                });
    }

    public void loadInter(Context context, String id, AdCallback adCallback) {
        InterstitialAd.load(context, id, getAdRequest(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        if (adCallback != null)
                            adCallback.onInterstitialLoad(interstitialAd);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        if (adCallback != null)
                            adCallback.onAdFailedToLoad(loadAdError);
                    }
                });
    }

    public void showInter(Activity context, InterstitialAd interstitialAd, AdCallback callback) {
        if (interstitialAd == null) {
            if (callback != null) {
                callback.doNextAction();
            }
            return;
        }
        interstitialAd.setOnPaidEventListener(adValue -> {
            logFromFacebook(context, adValue, Objects.requireNonNull(getAdIdsToNameMap().get(interstitialAd.getAdUnitId())), "interstitialAd");
            AppMetrica.reportExternalAdRevenue(adValue, interstitialAd);
            Log.d(TAG, "on paid inter : "+Objects.requireNonNull(getAdIdsToNameMap().get(interstitialAd.getAdUnitId())));
        });
        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent();
                if (callback != null) {
                    callback.onAdClosed();
                }
                if (dialog != null) {
                    dialog.dismiss();
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                super.onAdFailedToShowFullScreenContent(adError);
                // Called when fullscreen content failed to show.
                if (callback != null) {
                    callback.onAdFailedToShow(adError);
                    callback.doNextAction();
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
            }

            @Override
            public void onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent();
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                if (callback != null) {
                    callback.onAdClicked();
                }
            }
        });
        showInterstitialAd(context, interstitialAd, callback);
    }

    private void showInterstitialAd(Activity context, InterstitialAd mInterstitialAd, AdCallback callback) {
        if (ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            try {
                if (dialog != null && dialog.isShowing())
                    dialog.dismiss();
                createDialog(context);
                try {
                    callback.onInterstitialShow();
                    dialog.show();
                } catch (Exception e) {
                    callback.doNextAction();
                    return;
                }
            } catch (Exception e) {
                dialog = null;
                e.printStackTrace();
            }
            new Handler().postDelayed(() -> {
                if (((AppCompatActivity) context).getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                    if (callback != null) {
                        callback.doNextAction();
                        new Handler().postDelayed(() -> {
                            if (dialog != null && dialog.isShowing() && !(context).isDestroyed())
                                dialog.dismiss();
                        }, 1500);
                    }
                    mInterstitialAd.show(context);
                } else {
                    if (dialog != null && dialog.isShowing() && !(context).isDestroyed())
                        dialog.dismiss();
                    callback.onAdFailedToShow(new AdError(0, "Fail to show in background", "cns"));
                }
            }, 800);
        }
    }

    public void loadNativeAd(Context context, String id, AdCallback callback) {
        VideoOptions videoOptions = new VideoOptions.Builder()
                .setStartMuted(true)
                .build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder()
                .setVideoOptions(videoOptions)
                .build();
        AdLoader adLoader = new AdLoader.Builder(context, id)
                .forNativeAd(adMobNativeAd -> {
                    adMobNativeAd.setOnPaidEventListener(adValue -> {
                        AppMetrica.reportExternalAdRevenue(adValue, adMobNativeAd);
                        logFromFacebook(context, adValue, Objects.requireNonNull(getAdIdsToNameMap().get(id)), "native");
                        Log.d(TAG, "on paid native : "+Objects.requireNonNull(getAdIdsToNameMap().get(id)));
                    });
                    callback.onUnifiedNativeAdLoaded(adMobNativeAd);
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        callback.onAdFailedToLoad(error);
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        if (callback != null) {
                            callback.onAdClicked();
                        }
                    }
                })
                .withNativeAdOptions(adOptions)
                .build();
        adLoader.loadAd(getAdRequest());
    }

    public void populateNativeAdView(Activity activity, NativeAd apNativeAd, FrameLayout
            adPlaceHolder, ShimmerFrameLayout containerShimmerLoading, int layoutCustomNative) {
        if (apNativeAd == null) {
            containerShimmerLoading.setVisibility(View.GONE);
            return;
        }
        @SuppressLint("InflateParams") NativeAdView adView = (NativeAdView) LayoutInflater.from(activity).inflate(layoutCustomNative, null);
        containerShimmerLoading.stopShimmer();
        containerShimmerLoading.setVisibility(View.GONE);
        adPlaceHolder.setVisibility(View.VISIBLE);
        populateUnifiedNativeAdView(apNativeAd, adView);
        adPlaceHolder.removeAllViews();
        adPlaceHolder.addView(adView);
    }

    public void populateUnifiedNativeAdView(NativeAd nativeAd, NativeAdView adView) {
        adView.setMediaView(adView.findViewById(R.id.ad_media));
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));
        try {
            ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (nativeAd.getBody() == null) {
                adView.getBodyView().setVisibility(View.INVISIBLE);
            } else {
                adView.getBodyView().setVisibility(View.VISIBLE);
                ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (nativeAd.getCallToAction() == null) {
                Objects.requireNonNull(adView.getCallToActionView()).setVisibility(View.INVISIBLE);
            } else {
                Objects.requireNonNull(adView.getCallToActionView()).setVisibility(View.VISIBLE);
                ((TextView) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (nativeAd.getIcon() == null) {
                Objects.requireNonNull(adView.getIconView()).setVisibility(View.GONE);
            } else {
                ((ImageView) adView.getIconView()).setImageDrawable(
                        nativeAd.getIcon().getDrawable());
                adView.getIconView().setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (nativeAd.getPrice() == null) {
                Objects.requireNonNull(adView.getPriceView()).setVisibility(View.INVISIBLE);
            } else {
                Objects.requireNonNull(adView.getPriceView()).setVisibility(View.VISIBLE);
                ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (nativeAd.getStarRating() == null) {
                Objects.requireNonNull(adView.getStarRatingView()).setVisibility(View.INVISIBLE);
            } else {
                ((RatingBar) Objects.requireNonNull(adView.getStarRatingView())).setRating(nativeAd.getStarRating().floatValue());
                adView.getStarRatingView().setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (nativeAd.getAdvertiser() == null) {
                adView.getAdvertiserView().setVisibility(View.INVISIBLE);
            } else {
                ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
                adView.getAdvertiserView().setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        adView.setNativeAd(nativeAd);
    }

    public void loadCollapsibleBanner(final Activity mActivity, String id, String gravity, final AdCallback callback) {
        final FrameLayout adContainer = mActivity.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = mActivity.findViewById(R.id.shimmer_container_banner);
        loadCollapsibleBanner(mActivity, id, gravity, adContainer, containerShimmer, callback);
    }

    public static final String BANNER_INLINE_LARGE_STYLE = "BANNER_INLINE_LARGE_STYLE";

    private final int MAX_SMALL_INLINE_BANNER_HEIGHT = 50;

    private AdSize getAdSize(Activity mActivity, Boolean useInlineAdaptive, String inlineStyle) {

        Display display = mActivity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;

        int adWidth = (int) (widthPixels / density);

        if (useInlineAdaptive) {
            if (inlineStyle.equalsIgnoreCase(BANNER_INLINE_LARGE_STYLE)) {
                return AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(mActivity, adWidth);
            } else {
                return AdSize.getInlineAdaptiveBannerAdSize(adWidth, MAX_SMALL_INLINE_BANNER_HEIGHT);
            }
        }
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(mActivity, adWidth);

    }

    private AdRequest getAdRequestForCollapsibleBanner(String gravity) {
        AdRequest.Builder builder = new AdRequest.Builder();
        Bundle admobExtras = new Bundle();
        admobExtras.putString("collapsible", gravity);
        builder.addNetworkExtrasBundle(AdMobAdapter.class, admobExtras);
        return builder.build();
    }

    private void loadCollapsibleBanner(final Activity mActivity, String id, String gravity, final FrameLayout adContainer,
                                       final ShimmerFrameLayout containerShimmer, final AdCallback callback) {

        containerShimmer.setVisibility(View.VISIBLE);
        containerShimmer.startShimmer();
        try {
            AdView adView = new AdView(mActivity);
            adView.setAdUnitId(id);
            adContainer.addView(adView);
            AdSize adSize = getAdSize(mActivity, false, "");
            containerShimmer.getLayoutParams().height = (int) (adSize.getHeight() * Resources.getSystem().getDisplayMetrics().density + 0.5f);
            adView.setAdSize(adSize);
            adView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            adView.loadAd(getAdRequestForCollapsibleBanner(gravity));
            adView.setOnPaidEventListener(adValue -> {
                logFromFacebook(mActivity,adValue, Objects.requireNonNull(getAdIdsToNameMap().get(id)),"collapsible banner");
                AppMetrica.reportExternalAdRevenue(adValue, adView);
                Log.d(TAG, "on paid collapsible banner : "+Objects.requireNonNull(getAdIdsToNameMap().get(id)));
            });
            adView.setAdListener(new AdListener() {

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    super.onAdFailedToLoad(loadAdError);
                    containerShimmer.stopShimmer();
                    adContainer.setVisibility(View.GONE);
                    containerShimmer.setVisibility(View.GONE);
                    if (callback != null) {
                        callback.onAdFailedToLoad(loadAdError);
                    }
                }

                @Override
                public void onAdLoaded() {
                    containerShimmer.stopShimmer();
                    containerShimmer.setVisibility(View.GONE);
                    adContainer.setVisibility(View.VISIBLE);
                    if (callback != null) {
                        callback.onAdLoaded();
                    }
                }

                @Override
                public void onAdClicked() {
                    super.onAdClicked();
                    if (callback != null) {
                        callback.onAdClicked();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void loadNormalBanner(final Activity mActivity, String id) {
        final FrameLayout adContainer = mActivity.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = mActivity.findViewById(R.id.shimmer_container_banner);
        loadBanner(mActivity, id, adContainer, containerShimmer);
    }

    private AdSize getAdSize(Activity mActivity) {
        Display display = mActivity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;
        int adWidth = (int) (widthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(mActivity, adWidth);
    }

    private void loadBanner(final Activity mActivity, String id, final FrameLayout adContainer, final ShimmerFrameLayout containerShimmer) {
        containerShimmer.setVisibility(View.VISIBLE);
        containerShimmer.startShimmer();
        try {
            AdView adView = new AdView(mActivity);
            adView.setAdUnitId(id);
            adContainer.addView(adView);
            AdSize adSize = getAdSize(mActivity);
            containerShimmer.getLayoutParams().height = (int) (adSize.getHeight() * Resources.getSystem().getDisplayMetrics().density + 0.5f);
            adView.setAdSize(adSize);
            adView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            adView.loadAd(getAdRequest());
            adView.setOnPaidEventListener(adValue -> {
                logFromFacebook(mActivity,adValue, Objects.requireNonNull(getAdIdsToNameMap().get(id)),"banner");
                AppMetrica.reportExternalAdRevenue(adValue, adView.getAdUnitId());
                Log.d(TAG, "on paid banner : "+Objects.requireNonNull(getAdIdsToNameMap().get(id)));
            });
            adView.setAdListener(new AdListener() {

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    super.onAdFailedToLoad(loadAdError);
                    containerShimmer.stopShimmer();
                    adContainer.setVisibility(View.GONE);
                    containerShimmer.setVisibility(View.GONE);
                }

                @Override
                public void onAdLoaded() {
                    containerShimmer.stopShimmer();
                    containerShimmer.setVisibility(View.GONE);
                    adContainer.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAdClicked() {
                    super.onAdClicked();
                }
            });


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getRewardAds(Activity activity, String id, RewardCallback callback) {
        RewardedAd.load(activity, id, getAdRequest(), new RewardedAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                callback.onRewardedAdFailedToLoad(loadAdError.getCode());
            }

            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                callback.onRewardLoaded(ad);
            }
        });
    }

    public void showRewardAds(Activity activity, RewardedAd rewardAds, RewardCallback callback) {
        if (rewardAds == null) {
            callback.onRewardedAdFailedToShow(999);
            return;
        }
        rewardAds.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                callback.onRewardedAdClosed();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                callback.onRewardedAdFailedToShow(adError.getCode());
            }

            @Override
            public void onAdImpression() {
                // Called when an impression is recorded for an ad.
            }

            @Override
            public void onAdShowedFullScreenContent() {
                callback.onRewardShown();
                // Called when ad is shown.
            }
        });
        rewardAds.show(activity, rewardItem -> {
            callback.onUserEarnedReward();
        });
    }

    public void logFromFacebook(Context context, @NonNull AdValue adValue, @NonNull String adFormat, @NonNull String ad_Source_id) {
        long microsValue = adValue.getValueMicros();

        try {
            BigDecimal valueMicros = new BigDecimal(microsValue);
            BigDecimal valueInCurrency = valueMicros.divide(new BigDecimal(1_000), 3, RoundingMode.HALF_UP);

            Bundle params = new Bundle();
            params.putDouble("_ValueToSum", valueInCurrency.doubleValue());
            params.putDouble("value", valueInCurrency.doubleValue());
            params.putInt("precision", adValue.getPrecisionType());
            params.putString("currency", adValue.getCurrencyCode());
            params.putString("adFormat", adFormat);
            params.putString("adSourceId", ad_Source_id);
            params.putString(AppEventsConstants.EVENT_PARAM_CURRENCY, "USD");

            // Log doanh thu lên Facebook
            AppEventsLogger facebookLogger = AppEventsLogger.newLogger(context);
            facebookLogger.logEvent(AppEventsConstants.EVENT_NAME_AD_IMPRESSION, valueInCurrency.doubleValue(), params);
            facebookLogger.logPurchase(
                    valueInCurrency,
                    Currency.getInstance(adValue.getCurrencyCode()),
                    params
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
