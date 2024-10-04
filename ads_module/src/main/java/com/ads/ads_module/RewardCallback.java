package com.ads.ads_module;

import com.google.android.gms.ads.rewarded.RewardedAd;

public interface RewardCallback {
    void onUserEarnedReward();

    void onRewardedAdClosed();

    void onRewardedAdFailedToShow(int codeError);

    void onRewardedAdFailedToLoad(int codeError);

    void onRewardLoaded(RewardedAd rewardedAd);
    void onRewardShown();
}
