package com.rumtel.ad.helper.splash

import android.app.Activity
import android.support.annotation.NonNull
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import com.baidu.mobads.SplashAd
import com.baidu.mobads.SplashAdListener
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.TTAdNative
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.TTSplashAd
import com.qq.e.ads.splash.SplashAD
import com.qq.e.ads.splash.SplashADListener
import com.qq.e.comm.util.AdError
import com.rumtel.ad.R
import com.rumtel.ad.TogetherAd
import com.rumtel.ad.helper.AdBase
import com.rumtel.ad.other.AdNameType
import com.rumtel.ad.other.AdRandomUtil
import com.rumtel.ad.other.logd
import com.rumtel.ad.other.loge
import java.lang.ref.WeakReference
import java.util.*

/* 
 * (●ﾟωﾟ●) 开屏的广告
 * 
 * Created by Matthew_Chen on 2018/12/24.
 */
object TogetherAdSplash : AdBase {

    private var timer: Timer? = null
    private var overTimerTask: OverTimerTask? = null

    @Volatile
    private var stop = false

    /**
     * 显示开屏广告
     *
     * @param splashConfigStr "baidu:2,gdt:8"
     * @param adsParentLayout 容器
     * @param adListener      监听
     */
    fun showAdFull(
        @NonNull activity: Activity,
        splashConfigStr: String?,
        @NonNull adConstStr: String,
        @NonNull adsParentLayout: ViewGroup,
        @NonNull adListener: AdListenerSplashFull
    ) {
        stop = false
        startTimerTask(activity, adListener)

        val randomAdName = AdRandomUtil.getRandomAdName(splashConfigStr)
        when (randomAdName) {
            AdNameType.BAIDU -> showAdFullBaiduMob(activity, splashConfigStr, adConstStr, adsParentLayout, adListener)
            AdNameType.GDT -> showAdFullGDT(activity, splashConfigStr, adConstStr, adsParentLayout, adListener)
            AdNameType.CSJ -> showAdFullCsj(activity, splashConfigStr, adConstStr, adsParentLayout, adListener)
            else -> {
                if (stop) {
                    return
                }
                cancelTimerTask()

                adListener.onAdFailed(activity.getString(R.string.all_ad_error))
                loge(activity.getString(R.string.all_ad_error))
            }
        }
    }

    /**
     * 腾讯广点通
     */
    private fun showAdFullGDT(
        @NonNull activity: Activity,
        splashConfigStr: String?,
        @NonNull adConstStr: String,
        @NonNull adsParentLayout: ViewGroup,
        @NonNull adListener: AdListenerSplashFull
    ) {
        adListener.onStartRequest(AdNameType.GDT.type)

        SplashAD(
            activity,
            adsParentLayout,
            TogetherAd.appIdGDT,
            TogetherAd.idMapGDT[adConstStr],
            object : SplashADListener {
                override fun onADDismissed() {
                    adListener.onAdDismissed()
                    logd("${AdNameType.GDT.type}: ${activity.getString(R.string.dismiss)}")
                }

                override fun onNoAD(adError: AdError) {
                    if (stop) {
                        return
                    }
                    cancelTimerTask()

                    val newConfigPreMovie = splashConfigStr?.replace(AdNameType.GDT.type, AdNameType.NO.type)
                    showAdFull(
                        activity,
                        newConfigPreMovie,
                        adConstStr,
                        adsParentLayout,
                        adListener
                    )
                    loge("${AdNameType.GDT.type}: ${adError.errorMsg}")
                }

                override fun onADPresent() {
                    if (stop) {
                        return
                    }
                    cancelTimerTask()

                    adListener.onAdPrepared(AdNameType.GDT.type)
                    logd("${AdNameType.GDT.type}: ${activity.getString(R.string.prepared)}")
                }

                override fun onADClicked() {
                    adListener.onAdClick(AdNameType.GDT.type)
                    logd("${AdNameType.GDT.type}: ${activity.getString(R.string.clicked)}")
                }

                override fun onADTick(l: Long) {
                    logd("${AdNameType.GDT.type}: 倒计时: $l")
                }

                override fun onADExposure() {
                    logd("${AdNameType.GDT.type}: ${activity.getString(R.string.exposure)}")
                }
            })
    }

    /**
     * 百度Mob
     */
    private fun showAdFullBaiduMob(
        @NonNull activity: Activity,
        splashConfigStr: String?,
        @NonNull adConstStr: String,
        @NonNull adsParentLayout: ViewGroup,
        @NonNull adListener: AdListenerSplashFull
    ) {
        adListener.onStartRequest(AdNameType.BAIDU.type)

        SplashAd(activity, adsParentLayout, object : SplashAdListener {
            override fun onAdPresent() {
                if (stop) {
                    return
                }
                cancelTimerTask()

                adListener.onAdPrepared(AdNameType.BAIDU.type)
                logd("${AdNameType.BAIDU.type}: ${activity.getString(R.string.prepared)}")
            }

            override fun onAdDismissed() {
                logd("${AdNameType.BAIDU.type}: ${activity.getString(R.string.dismiss)}")
                adListener.onAdDismissed()
            }

            override fun onAdFailed(s: String) {
                loge("${AdNameType.BAIDU.type}: $s")
                val newConfigPreMovie = splashConfigStr?.replace(AdNameType.BAIDU.type, AdNameType.NO.type)
                showAdFull(activity, newConfigPreMovie, adConstStr, adsParentLayout, adListener)
            }

            override fun onAdClick() {
                logd("${AdNameType.BAIDU.type}: ${activity.getString(R.string.clicked)}")
                adListener.onAdClick(AdNameType.BAIDU.type)
            }

        }, TogetherAd.idMapBaidu[adConstStr], true)
    }

    /**
     * 穿山甲
     */
    private fun showAdFullCsj(
        @NonNull activity: Activity,
        splashConfigStr: String?,
        @NonNull adConstStr: String,
        @NonNull adsParentLayout: ViewGroup,
        @NonNull adListener: AdListenerSplashFull
    ) {
        adListener.onStartRequest(AdNameType.CSJ.type)
        val dm = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(dm)
        //step3:创建开屏广告请求参数AdSlot,具体参数含义参考文档
        val adSlot = AdSlot.Builder()
            .setCodeId(TogetherAd.idMapCsj[adConstStr])
            .setSupportDeepLink(true)
            .setImageAcceptedSize(dm.widthPixels, dm.heightPixels)
            .build()
        TTAdSdk.getAdManager().createAdNative(activity).loadSplashAd(adSlot, object : TTAdNative.SplashAdListener {
            override fun onSplashAdLoad(splashAd: TTSplashAd?) {
                if (splashAd == null) {
                    loge("${AdNameType.CSJ.type}: 广告是 null")
                    val newSplashConfigStr = splashConfigStr?.replace(AdNameType.CSJ.type, AdNameType.NO.type)
                    showAdFull(activity, newSplashConfigStr, adConstStr, adsParentLayout, adListener)
                    return
                }

                if (stop) {
                    return
                }
                cancelTimerTask()

                adListener.onAdPrepared(AdNameType.CSJ.type)
                logd("${AdNameType.CSJ.type}: ${activity.getString(R.string.prepared)}")

                adsParentLayout.removeAllViews()
                adsParentLayout.addView(splashAd.splashView)

                splashAd.setSplashInteractionListener(object : TTSplashAd.AdInteractionListener {
                    override fun onAdClicked(view: View?, p1: Int) {
                        logd("${AdNameType.CSJ.type}: ${activity.getString(R.string.clicked)}")
                        adListener.onAdClick(AdNameType.CSJ.type)
                    }

                    override fun onAdSkip() {
                        logd("${AdNameType.CSJ.type}: ${activity.getString(R.string.dismiss)}")
                        adListener.onAdDismissed()
                    }

                    override fun onAdShow(p0: View?, p1: Int) {
                        logd("${AdNameType.CSJ.type}: ${activity.getString(R.string.exposure)}")
                    }

                    override fun onAdTimeOver() {
                        logd("${AdNameType.CSJ.type}: ${activity.getString(R.string.dismiss)}")
                        adListener.onAdDismissed()
                    }
                })
            }

            override fun onTimeout() {
                if (stop) {
                    return
                }
                cancelTimerTask()

                loge("${AdNameType.CSJ.type}: ${activity.getString(R.string.timeout)}")
                val newSplashConfigStr = splashConfigStr?.replace(AdNameType.CSJ.type, AdNameType.NO.type)
                showAdFull(activity, newSplashConfigStr, adConstStr, adsParentLayout, adListener)
            }

            override fun onError(errorCode: Int, errorMsg: String?) {
                if (stop) {
                    return
                }
                cancelTimerTask()

                loge("${AdNameType.CSJ.type}: $errorCode : $errorMsg")
                val newSplashConfigStr = splashConfigStr?.replace(AdNameType.CSJ.type, AdNameType.NO.type)
                showAdFull(activity, newSplashConfigStr, adConstStr, adsParentLayout, adListener)
            }
        }, 2500)//超时时间，demo 为 2000
    }

    /**
     * 监听器
     */
    interface AdListenerSplashFull {
        fun onStartRequest(channel: String)

        fun onAdClick(channel: String)

        fun onAdFailed(failedMsg: String?)

        fun onAdDismissed()

        fun onAdPrepared(channel: String)
    }

    /**
     * 取消超时任务
     */
    private fun cancelTimerTask() {
        stop = false
        timer?.cancel()
        overTimerTask?.cancel()
    }

    /**
     * 开始超时任务
     */
    private fun startTimerTask(activity: Activity, listener: AdListenerSplashFull) {
        cancelTimerTask()
        timer = Timer()
        overTimerTask = OverTimerTask(activity, listener)
        timer?.schedule(overTimerTask, TogetherAd.timeOutMillis)
    }

    /**
     * 超时任务
     */
    private class OverTimerTask(activity: Activity, listener: AdListenerSplashFull) : TimerTask() {

        private val weakReference: WeakReference<AdListenerSplashFull>?
        private val weakRefContext: WeakReference<Activity>?

        init {
            weakReference = WeakReference(listener)
            weakRefContext = WeakReference(activity)
        }

        override fun run() {
            stop = true
            weakRefContext?.get()?.runOnUiThread {
                weakReference?.get()?.onAdFailed(weakRefContext.get()?.getString(R.string.timeout))
                loge(weakRefContext.get()?.getString(R.string.timeout))
                timer = null
                overTimerTask = null
            }
        }
    }

}