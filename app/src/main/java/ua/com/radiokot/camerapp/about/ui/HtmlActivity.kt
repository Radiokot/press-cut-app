package ua.com.radiokot.camerapp.about.ui

import android.content.Intent
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class HtmlActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.light(0, 0),
        )
        super.onCreate(savedInstanceState)

        setContentView(
            WebView(this).apply {
                loadUrl("file://${intent.getStringExtra(ANDROID_ASSET_PATH_EXTRA)}")

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean {
                        val url = request.url

                        if (url.scheme == "http" || url.scheme == "https") {
                            val intent = Intent(Intent.ACTION_VIEW, url)
                            val chooser = Intent.createChooser(intent, "Open with")
                            startActivity(chooser)
                            return true
                        }

                        return false
                    }
                }
            }
        )
    }

    companion object {
        private const val ANDROID_ASSET_PATH_EXTRA = "android_asset"

        /**
         * @param androidAssetPath asset HTML path that starts with `/android_asset`
         */
        fun getBundle(
            androidAssetPath: String,
        ) = Bundle().apply {
            putString(ANDROID_ASSET_PATH_EXTRA, androidAssetPath)
        }
    }
}
