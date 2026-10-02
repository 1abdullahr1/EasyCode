package com.vu.lecturehub.ui.player

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.vu.lecturehub.R
import com.vu.lecturehub.data.model.Course
import com.vu.lecturehub.data.model.Lecture
import com.vu.lecturehub.data.repository.CourseRepository
import com.vu.lecturehub.databinding.ActivityPlayerBinding
import com.vu.lecturehub.ui.adapters.LectureAdapter
import com.vu.lecturehub.ui.adapters.PlayerHeaderAdapter
import com.vu.lecturehub.util.WatchHistoryManager
import kotlinx.coroutines.launch

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var repository: CourseRepository
    private var currentCourse: Course? = null
    private var currentLecture: Lecture? = null
    private var currentLectures: List<Lecture> = emptyList()
    private lateinit var playerHeaderAdapter: PlayerHeaderAdapter
    private lateinit var queueAdapter: LectureAdapter
    private var currentLoadedVideoId: String? = null

    private val autoDismissOverlayRunnable = Runnable {
        hidePlayerOverlay()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = CourseRepository(this)

        @Suppress("DEPRECATION")
        currentCourse = intent.getSerializableExtra("EXTRA_COURSE") as? Course
        @Suppress("DEPRECATION")
        currentLecture = intent.getSerializableExtra("EXTRA_LECTURE") as? Lecture

        if (currentCourse == null) {
            finish()
            return
        }

        if (currentLecture == null) {
            val lectureIndex = if (currentCourse!!.lastWatchedLectureIndex > 0) currentCourse!!.lastWatchedLectureIndex else 1
            currentLecture = Lecture(
                playlistId = currentCourse!!.playlistId,
                lectureIndex = lectureIndex,
                title = "Lecture ${String.format("%02d", lectureIndex)} - ${currentCourse!!.title}",
                videoId = if (lectureIndex == 1) currentCourse!!.firstVideoId else null,
                thumbnailUrl = currentCourse!!.thumbnailUrl
            )
        }

        // Show instant thumbnail and loader to prevent initial black screen
        val initialThumb = if (!currentLecture?.videoId.isNullOrEmpty()) {
            "https://i.ytimg.com/vi/${currentLecture!!.videoId}/hqdefault.jpg"
        } else {
            currentLecture?.thumbnailUrl ?: currentCourse?.thumbnailUrl
        }
        showPlayerOverlay(initialThumb)

        setupRecyclerView(currentCourse!!, currentLecture!!)
        setupPlayerWebView()
        loadRealLectures(currentCourse!!)

        // Determine initial playback target
        val initialVideoId = if (!currentLecture?.videoId.isNullOrEmpty()) {
            currentLecture!!.videoId
        } else if (currentLecture?.lectureIndex == 1 && !currentCourse?.firstVideoId.isNullOrEmpty()) {
            currentCourse!!.firstVideoId
        } else {
            null
        }

        val playlistId = currentCourse?.playlistId ?: ""
        val initialIndex = (currentLecture?.lectureIndex ?: 1) - 1
        loadPlayer(initialVideoId, playlistId, initialIndex)
    }

    private fun showPlayerOverlay(thumbnailUrl: String?) {
        binding.ivPlayerThumbnail.animate().cancel()
        binding.ivPlayerThumbnail.alpha = 1f
        binding.ivPlayerThumbnail.visibility = View.VISIBLE
        binding.pbPlayerLoader.visibility = View.VISIBLE

        if (!thumbnailUrl.isNullOrEmpty()) {
            binding.ivPlayerThumbnail.load(thumbnailUrl) {
                crossfade(true)
            }
        }

        // Safety auto-dismiss after 6 seconds in case YouTube API events fail or stall
        binding.ivPlayerThumbnail.removeCallbacks(autoDismissOverlayRunnable)
        binding.ivPlayerThumbnail.postDelayed(autoDismissOverlayRunnable, 6000)
    }

    private fun hidePlayerOverlay() {
        binding.ivPlayerThumbnail.removeCallbacks(autoDismissOverlayRunnable)
        binding.pbPlayerLoader.visibility = View.GONE
        if (binding.ivPlayerThumbnail.visibility == View.VISIBLE) {
            binding.ivPlayerThumbnail.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    binding.ivPlayerThumbnail.visibility = View.GONE
                    binding.ivPlayerThumbnail.alpha = 1f
                }
                .start()
        }
    }

    private fun setupRecyclerView(course: Course, lecture: Lecture) {
        playerHeaderAdapter = PlayerHeaderAdapter(course, lecture) {
            openCurrentOnYoutube()
        }

        currentLectures = repository.generateLectures(course)
        queueAdapter = LectureAdapter(currentLectures) { selectedLecture ->
            switchLecture(selectedLecture)
        }

        val concatAdapter = ConcatAdapter(playerHeaderAdapter, queueAdapter)

        binding.rvPlayerContent.apply {
            layoutManager = LinearLayoutManager(this@PlayerActivity)
            adapter = concatAdapter
            setHasFixedSize(true)
        }

        lifecycleScope.launch {
            repository.updateWatchProgress(course.playlistId, lecture.lectureIndex)
        }

        WatchHistoryManager.recordVideoWatch(
            context = this,
            playlistId = course.playlistId,
            courseCode = course.courseCode,
            courseTitle = course.title,
            lectureIndex = lecture.lectureIndex,
            lectureTitle = lecture.title,
            videoId = lecture.videoId ?: course.firstVideoId,
            thumbnailUrl = lecture.thumbnailUrl ?: course.thumbnailUrl
        )
    }

    private fun openCurrentOnYoutube() {
        val vid = currentLecture?.videoId ?: currentCourse?.firstVideoId
        if (!vid.isNullOrEmpty()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$vid"))
            startActivity(intent)
        } else if (!currentCourse?.playlistId.isNullOrEmpty()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/playlist?list=${currentCourse!!.playlistId}"))
            startActivity(intent)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupPlayerWebView() {
        val webView = binding.playerWebView

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false // Enables instant autoplay
            loadWithOverviewMode = true
            useWideViewPort = true
            allowFileAccess = false
            databaseEnabled = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                if (request != null && !request.isForMainFrame) {
                    return false
                }
                val url = request?.url?.toString() ?: return false
                val appOrigin = "https://$packageName"
                if (url.startsWith(appOrigin) || url.contains("youtube.com") || url.contains("googlevideo.com") || url.contains("ytimg.com") || url.contains("youtube-nocookie.com")) {
                    return false
                }
                return try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                    true
                } catch (e: Exception) {
                    false
                }
            }
        }

        webView.webChromeClient = WebChromeClient()

        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun onPlaybackStarted() {
                runOnUiThread {
                    hidePlayerOverlay()
                }
            }

            @JavascriptInterface
            fun onVideoEnded() {
                runOnUiThread {
                    playNextLecture()
                }
            }
        }, "AndroidBridge")
    }

    private fun loadPlayer(videoId: String?, playlistId: String, index: Int) {
        currentLoadedVideoId = videoId
        val appOrigin = "https://$packageName"
        val playerInitScript = if (!videoId.isNullOrEmpty()) {
            """
            player = new YT.Player('player', {
                height: '100%',
                width: '100%',
                videoId: '$videoId',
                playerVars: {
                    'autoplay': 1,
                    'playsinline': 1,
                    'rel': 0,
                    'modestbranding': 1,
                    'controls': 1,
                    'fs': 1,
                    'enablejsapi': 1,
                    'origin': '$appOrigin'
                },
                events: {
                    'onReady': onPlayerReady,
                    'onStateChange': onPlayerStateChange,
                    'onError': onPlayerError
                }
            });
            """.trimIndent()
        } else {
            """
            player = new YT.Player('player', {
                height: '100%',
                width: '100%',
                playerVars: {
                    'listType': 'playlist',
                    'list': '$playlistId',
                    'index': $index,
                    'autoplay': 1,
                    'playsinline': 1,
                    'rel': 0,
                    'modestbranding': 1,
                    'controls': 1,
                    'fs': 1,
                    'enablejsapi': 1,
                    'origin': '$appOrigin'
                },
                events: {
                    'onReady': onPlayerReady,
                    'onStateChange': onPlayerStateChange,
                    'onError': onPlayerError
                }
            });
            """.trimIndent()
        }

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <meta name="referrer" content="strict-origin-when-cross-origin">
                <style>
                    * { margin:0; padding:0; box-sizing:border-box; }
                    html, body { width:100%; height:100%; background:#000; overflow:hidden; }
                    #player { width:100%; height:100%; position:absolute; top:0; left:0; border:0; }
                </style>
                <script defer src="https://www.youtube.com/iframe_api"></script>
            </head>
            <body>
                <div id="player"></div>
                <script>
                    var player;
                    function onYouTubeIframeAPIReady() {
                        try {
                            $playerInitScript
                        } catch(e) {
                            fallbackDirect();
                        }
                    }

                    function onPlayerReady(event) {
                        try {
                            event.target.playVideo();
                        } catch(e) {}
                    }

                    function onPlayerStateChange(event) {
                        if (event.data === 1 || event.data === 3) { // PLAYING or BUFFERING
                            if (window.AndroidBridge) {
                                window.AndroidBridge.onPlaybackStarted();
                            }
                        }
                        if (event.data === 0) { // ENDED
                            if (window.AndroidBridge) {
                                window.AndroidBridge.onVideoEnded();
                            }
                        }
                    }

                    function onPlayerError(event) {
                        fallbackDirect();
                    }

                    function fallbackDirect() {
                        var v = '${videoId ?: ""}';
                        var pl = '$playlistId';
                        var idx = $index;
                        if (v !== '') {
                            document.body.innerHTML = '<iframe id="player" src="https://www.youtube-nocookie.com/embed/' + v + '?autoplay=1&playsinline=1&rel=0&controls=1" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>';
                        } else if (pl !== '') {
                            document.body.innerHTML = '<iframe id="player" src="https://www.youtube-nocookie.com/embed?listType=playlist&list=' + pl + '&index=' + idx + '&autoplay=1&playsinline=1&rel=0&controls=1" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>';
                        }
                        setTimeout(function() {
                            if (window.AndroidBridge) window.AndroidBridge.onPlaybackStarted();
                        }, 1000);
                    }

                    function playLecture(vid, plId, idx) {
                        try {
                            if (vid && vid !== '') {
                                if (player && typeof player.loadVideoById === 'function') {
                                    player.loadVideoById(vid);
                                } else {
                                    document.body.innerHTML = '<iframe id="player" src="https://www.youtube-nocookie.com/embed/' + vid + '?autoplay=1&playsinline=1&rel=0&controls=1" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>';
                                    setTimeout(function() { if (window.AndroidBridge) window.AndroidBridge.onPlaybackStarted(); }, 1000);
                                }
                            } else if (plId && plId !== '') {
                                if (player && typeof player.loadPlaylist === 'function') {
                                    player.loadPlaylist({
                                        list: plId,
                                        listType: 'playlist',
                                        index: Math.max(0, idx)
                                    });
                                } else {
                                    document.body.innerHTML = '<iframe id="player" src="https://www.youtube-nocookie.com/embed?listType=playlist&list=' + plId + '&index=' + idx + '&autoplay=1&playsinline=1&rel=0&controls=1" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>';
                                    setTimeout(function() { if (window.AndroidBridge) window.AndroidBridge.onPlaybackStarted(); }, 1000);
                                }
                            }
                        } catch(e) {
                            if (vid && vid !== '') {
                                document.body.innerHTML = '<iframe id="player" src="https://www.youtube-nocookie.com/embed/' + vid + '?autoplay=1&playsinline=1&rel=0&controls=1" frameborder="0" allowfullscreen></iframe>';
                            }
                        }
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        binding.playerWebView.loadDataWithBaseURL(
            appOrigin,
            html,
            "text/html",
            "UTF-8",
            "https://www.youtube.com"
        )
    }

    private fun loadRealLectures(course: Course) {
        lifecycleScope.launch {
            val real = repository.fetchPlaylistVideos(course)
            if (real.isNotEmpty()) {
                currentLectures = real
                queueAdapter.updateLectures(real)
                val matching = real.find { it.lectureIndex == currentLecture?.lectureIndex }
                if (matching != null && currentLecture?.videoId.isNullOrEmpty()) {
                    currentLecture = matching
                    playerHeaderAdapter.updateLecture(matching)
                }
            }
        }
    }

    private fun switchLecture(lecture: Lecture) {
        currentLecture = lecture
        playerHeaderAdapter.updateLecture(lecture)
        lifecycleScope.launch {
            repository.updateWatchProgress(currentCourse!!.playlistId, lecture.lectureIndex)
        }

        val c = currentCourse
        if (c != null) {
            WatchHistoryManager.recordVideoWatch(
                context = this,
                playlistId = c.playlistId,
                courseCode = c.courseCode,
                courseTitle = c.title,
                lectureIndex = lecture.lectureIndex,
                lectureTitle = lecture.title,
                videoId = lecture.videoId ?: c.firstVideoId,
                thumbnailUrl = lecture.thumbnailUrl ?: c.thumbnailUrl
            )
        }

        val thumb = if (!lecture.videoId.isNullOrEmpty()) {
            "https://i.ytimg.com/vi/${lecture.videoId}/hqdefault.jpg"
        } else {
            lecture.thumbnailUrl ?: currentCourse?.thumbnailUrl
        }
        showPlayerOverlay(thumb)

        val vid = lecture.videoId ?: ""
        val plId = currentCourse?.playlistId ?: ""
        val idx = lecture.lectureIndex - 1
        currentLoadedVideoId = if (vid.isNotEmpty()) vid else null

        binding.playerWebView.evaluateJavascript("playLecture('$vid', '$plId', $idx);", null)
    }

    private fun playNextLecture() {
        val nextIndex = (currentLecture?.lectureIndex ?: 1) + 1
        val nextLecture = currentLectures.find { it.lectureIndex == nextIndex }
        if (nextLecture != null) {
            switchLecture(nextLecture)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.playerContainer.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        } else {
            binding.playerContainer.layoutParams.height = (220 * resources.displayMetrics.density).toInt()
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onPause() {
        super.onPause()
        binding.playerWebView.onPause()
        binding.playerWebView.evaluateJavascript("if (player && typeof player.pauseVideo === 'function') { player.pauseVideo(); }", null)
    }

    override fun onResume() {
        super.onResume()
        binding.playerWebView.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.ivPlayerThumbnail.removeCallbacks(autoDismissOverlayRunnable)
        binding.playerWebView.destroy()
    }
}
