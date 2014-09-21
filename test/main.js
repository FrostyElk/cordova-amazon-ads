// Test of Amazon plugin
/* jshint -W117 */

document.addEventListener("deviceready", function () {
	Main();
});

function Main() {

	if (typeof cordova !== 'undefined') {
		console.log("Amazon Test running in Cordova");
	} else {
		console.log("Amazon Test Not Running in Cordova");
	}

	if (typeof AmazonAds !== 'undefined') {
		console.log("Amazon Ads available");
	} else {
		console.log("Amazon Ads Not available");
	}

}

function doCreateInterstitialAd() {
	var options = {
		'interstitialAdId': "d7ba9527e7da407fa42f6ee76a1f1ac9",
		'isTesting': true
	};

	AmazonAds.createInterstitialAd(options,
		function () {
			console.log("createInterstitialAd successful");
		},

		function () {
			console.warn("createInterstitialAd failed");
		}
	);

}


function doShowInterstitialAd() {

	AmazonAds.showInterstitialAd(
		function () {
			console.log("showInterstitialAd successful");
		},

		function () {
			console.warn("showInterstitialAd failed");
		}
	);

}


document.getElementById('createInterstitialAd').onclick = function () {
	doCreateInterstitialAd();
};

document.getElementById('showInterstitialAd').onclick = function () {
	doShowInterstitialAd();
};


/**
Events
*/

document.addEventListener("onReceiveInterstitialAd", function () {
	console.log("Amazon Ads Event onReceiveInterstitialAd");
});


document.addEventListener("onFailedToReceiveInterstitialAd", function (result) {
	console.log("Amazon Ads Event onFailedToReceiveInterstitialAd. Error: " + result.error + " , Reason: " + result.reason );
});


document.addEventListener("onDismissInterstitialAd", function () {
	console.log("Amazon Ads Event onDismissInterstitialAd");
});