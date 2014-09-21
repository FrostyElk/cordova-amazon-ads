/**
Copyright 2014 Frosty Elk AB

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package se.frostyelk.cordova.amazon.ads;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.amazon.device.ads.Ad;
import com.amazon.device.ads.AdError;
import com.amazon.device.ads.AdProperties;
import com.amazon.device.ads.AdRegistration;
import com.amazon.device.ads.DefaultAdListener;
import com.amazon.device.ads.InterstitialAd;

/**
 * This class represents the native implementation for the Amazon Ads Cordova
 * plugin. This plugin can be used to request Amazon Ads ads natively via the
 * Amazon Ads SDK.
 */
public class AmazonAds extends CordovaPlugin {
	private static final String LOGTAG = "AmazonAds";

	private static final String OPTION_INTERSTITIAL_AD_ID = "interstitialAdId";
	private static final String OPTION_IS_TESTING = "isTesting";
	private static final String OPTION_DEBUG = "debug";

	private static final String ACTION_CREATE_INTERSTITIAL_AD = "createInterstitialAd";
	private static final String ACTION_SHOW_INTERSTITIAL_AD = "showInterstitialAd";

	private InterstitialAd interstitialAd;

	private String interstitialAdId;
	private boolean isTesting = true;

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);
		Log.i(LOGTAG, "Amazon Ads initialize");
	}

	/**
	 * This is the main method for the Amazon Ads plugin. All API calls go
	 * through here. This method determines the action, and executes the
	 * appropriate call.
	 *
	 * @param action
	 *            The action that the plugin should execute.
	 * @param inputs
	 *            The input parameters for the action.
	 * @param callbackContext
	 *            The callback context.
	 * @return A PluginResult representing the result of the provided action. A
	 *         status of INVALID_ACTION is returned if the action is not
	 *         recognized.
	 */
	@Override
	public boolean execute(String action, JSONArray inputs, CallbackContext callbackContext) throws JSONException {
		PluginResult result = null;

		if (ACTION_CREATE_INTERSTITIAL_AD.equals(action)) {
			JSONObject options = inputs.optJSONObject(0);
			result = executeCreateInterstitialAd(options, callbackContext);
		} else if (ACTION_SHOW_INTERSTITIAL_AD.equals(action)) {
			result = executeShowInterstitialAd(callbackContext);
		} else {
			result = new PluginResult(Status.INVALID_ACTION);
		}

		if (result != null)
			callbackContext.sendPluginResult(result);

		return true;
	}

	private PluginResult executeCreateInterstitialAd(JSONObject options, final CallbackContext callbackContext) {
		if (options.has(OPTION_INTERSTITIAL_AD_ID)) {
			interstitialAdId = options.optString(OPTION_INTERSTITIAL_AD_ID);
		} else {
			return new PluginResult(Status.ERROR, "Option " + OPTION_INTERSTITIAL_AD_ID + " is missing");
		}

		if (options.has(OPTION_IS_TESTING)) {
			try {
				isTesting = options.getBoolean(OPTION_IS_TESTING);
			} catch (JSONException e) {
				return new PluginResult(Status.ERROR, "Value of option " + OPTION_IS_TESTING + " is wrong");
			}
		}

		if (options.has(OPTION_DEBUG)) {
			try {
				AdRegistration.enableLogging(options.getBoolean(OPTION_DEBUG));
			} catch (JSONException e) {
				return new PluginResult(Status.ERROR, "Value of option " + OPTION_DEBUG + " is wrong");
			}
		} else {
			AdRegistration.enableLogging(false);
		}

		if (callbackContext == null) {
			return new PluginResult(Status.ERROR, "Callback function is missing");
		}

		cordova.getThreadPool().execute(new Runnable() {
			@Override
			public void run() {

				if (interstitialAd == null) {
					interstitialAd = new InterstitialAd(cordova.getActivity());
					interstitialAd.setListener(new AmazonInterstitialAdListener());
				}

				AdRegistration.enableTesting(isTesting);

				try {
					AdRegistration.setAppKey(interstitialAdId);
				} catch (final IllegalArgumentException e) {
					callbackContext.error("Value of " + OPTION_INTERSTITIAL_AD_ID + " is wrong");
					return;
				}

				if (interstitialAd.loadAd()) {
					if (callbackContext != null) {
						callbackContext.success();
					}
				} else {
					if (callbackContext != null) {
						callbackContext.error("Failed to create Ad");
					}
				}
			}
		});

		return null;
	}

	private PluginResult executeShowInterstitialAd(final CallbackContext callbackContext) {

		if (interstitialAd == null) {
			return new PluginResult(Status.ERROR, "Interstitial Ad not available, call createInterstitialAd first.");
		}

		cordova.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (interstitialAd.showAd()) {
					if (callbackContext != null) {
						callbackContext.success();
					}
				} else {
					if (callbackContext != null) {
						callbackContext.error("Ad not showed");
					}
				}
			}
		});

		return null;
	}

	/**
	 * 
	 * The events that can be received from the Amazon Ads SDK
	 *
	 */
	class AmazonInterstitialAdListener extends DefaultAdListener {

		/**
		 * This event is called once an ad loads successfully.
		 */
		@Override
		public void onAdLoaded(final Ad ad, final AdProperties adProperties) {
			webView.loadUrl("javascript:cordova.fireDocumentEvent('onReceiveInterstitialAd');");
		}

		/**
		 * This event is called if an ad fails to load.
		 */
		@Override
		public void onAdFailedToLoad(final Ad ad, final AdError error) {
			webView.loadUrl("javascript:cordova.fireDocumentEvent('onFailedToReceiveInterstitialAd', { 'error':" + "\""
					+ error.getCode() + "\"" + ", 'reason':" + "\"" + error.getMessage() + "\"" + "});");
		}

		/**
		 * This event is called after the ad is dismissed.
		 */
		@Override
		public void onAdDismissed(final Ad ad) {
			webView.loadUrl("javascript:cordova.fireDocumentEvent('onDismissInterstitialAd');");
		}
	}

}
