package com.vu.lecturehub

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.vu.lecturehub.data.model.Course
import com.vu.lecturehub.data.model.Lecture
import com.vu.lecturehub.data.repository.CourseRepository
import com.vu.lecturehub.databinding.ActivityMainBinding
import androidx.viewpager2.widget.ViewPager2
import com.vu.lecturehub.ui.MainPagerAdapter
import com.vu.lecturehub.ui.MainViewModel
import com.vu.lecturehub.ui.adapters.LectureAdapter
import com.vu.lecturehub.ui.adapters.PlayerHeaderAdapter
import com.vu.lecturehub.util.PlaybackManager
import com.vu.lecturehub.util.ThemeManager
import com.vu.lecturehub.util.WatchHistoryManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    val viewModel: MainViewModel by viewModels()

    private lateinit var pagerAdapter: MainPagerAdapter

    // Player Components
    private lateinit var repository: CourseRepository
    private lateinit var playerBehavior: BottomSheetBehavior<View>
    private var currentCourse: Course? = null
    private var currentLecture: Lecture? = null
    private var currentLectures: List<Lecture> = emptyList()
    private var playerHeaderAdapter: PlayerHeaderAdapter? = null
    private var queueAdapter: LectureAdapter? = null
    private var currentLoadedVideoId: String? = null
    private var isPlaying: Boolean = false

    // Fullscreen View variables
    private var fullscreenCustomView: View? = null
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null
    private var playerChromeClient: WebChromeClient? = null

    private val autoDismissOverlayRunnable = Runnable {
        hidePlayerOverlay()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = CourseRepository(this)

        setupViewPager()
        setupNavigation()
        setupPlayerBottomSheet()
        setupPlayerWebView()
        setupBackPressHandling()

        handleIntentPlayback(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentPlayback(intent)
    }

    private fun handleIntentPlayback(intent: Intent?) {
        if (intent == null) return
        @Suppress("DEPRECATION")
        val course = intent.getSerializableExtra("EXTRA_PLAY_COURSE") as? Course
        @Suppress("DEPRECATION")
        val lecture = intent.getSerializableExtra("EXTRA_PLAY_LECTURE") as? Lecture
        if (course != null) {
            playCourse(course, lecture)
        }
    }

    private fun setupViewPager() {
        pagerAdapter = MainPagerAdapter(this)
        binding.viewPager.apply {
            adapter = pagerAdapter
            offscreenPageLimit = 2 // Keeps all 3 tabs pre-rendered in memory for instant, zero-hitch transitions!
            isUserInputEnabled = true // Enables smooth swipe gestures between tabs
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    val navId = when (position) {
                        MainPagerAdapter.TAB_COURSES -> R.id.nav_courses
                        MainPagerAdapter.TAB_SAVED -> R.id.nav_saved
                        else -> R.id.nav_home
                    }
                    if (binding.bottomNavigation.selectedItemId != navId) {
                        binding.bottomNavigation.selectedItemId = navId
                    }
                }
            })
        }
    }

    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val targetPosition = when (item.itemId) {
                R.id.nav_home -> MainPagerAdapter.TAB_HOME
                R.id.nav_courses -> MainPagerAdapter.TAB_COURSES
                R.id.nav_saved -> MainPagerAdapter.TAB_SAVED
                else -> -1
            }
            if (targetPosition != -1 && binding.viewPager.currentItem != targetPosition) {
                binding.viewPager.setCurrentItem(targetPosition, true) // Smooth hardware-accelerated transition!
            }
            true
        }
    }

    fun switchTab(menuItemId: Int) {
        val position = when (menuItemId) {
            R.id.nav_courses -> MainPagerAdapter.TAB_COURSES
            R.id.nav_saved -> MainPagerAdapter.TAB_SAVED
            else -> MainPagerAdapter.TAB_HOME
        }
        binding.viewPager.setCurrentItem(position, true)
    }

    // =========================================================================
    // SLIDING PLAYER BOTTOM SHEET & CONTINUOUS MULTI-TAB PLAYBACK
    // =========================================================================

    private fun setupPlayerBottomSheet() {
        val sheetView = binding.slidingPlayer.playerSheetRoot
        playerBehavior = BottomSheetBehavior.from(sheetView)
        playerBehavior.isHideable = true
        playerBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        val density = resources.displayMetrics.density
        val miniPlayerHeight = (64 * density).toInt()
        val bottomNavHeight = (80 * density).toInt()

        playerBehavior.peekHeight = miniPlayerHeight + bottomNavHeight

        playerBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                val offset = slideOffset.coerceIn(0f, 1f)

                // Slide bottom nav down smoothly as sheet expands
                binding.bottomNavigation.translationY = bottomNavHeight * offset

                // Fade mini player bar out as player expands
                val miniAlpha = (1f - offset * 3f).coerceIn(0f, 1f)
                binding.slidingPlayer.miniPlayerBar.alpha = miniAlpha
                binding.slidingPlayer.miniPlayerBar.visibility = if (miniAlpha > 0.05f) View.VISIBLE else View.GONE

                // Fade expanded player view in
                val expandedAlpha = (offset * 2f - 0.5f).coerceIn(0f, 1f)
                binding.slidingPlayer.expandedPlayerLayout.alpha = expandedAlpha
                binding.slidingPlayer.expandedPlayerLayout.visibility = if (expandedAlpha > 0.05f) View.VISIBLE else View.GONE
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        playerBehavior.isHideable = false
                        binding.bottomNavigation.translationY = bottomNavHeight.toFloat()
                        binding.slidingPlayer.miniPlayerBar.visibility = View.GONE
                        binding.slidingPlayer.expandedPlayerLayout.visibility = View.VISIBLE
                        binding.slidingPlayer.expandedPlayerLayout.alpha = 1f
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        playerBehavior.isHideable = false
                        binding.bottomNavigation.translationY = 0f
                        binding.slidingPlayer.miniPlayerBar.visibility = View.VISIBLE
                        binding.slidingPlayer.miniPlayerBar.alpha = 1f
                        binding.slidingPlayer.expandedPlayerLayout.visibility = View.GONE
                        binding.slidingPlayer.expandedPlayerLayout.alpha = 0f
                    }
                    BottomSheetBehavior.STATE_HIDDEN -> {
                        binding.bottomNavigation.translationY = 0f
                        pauseVideo()
                        binding.slidingPlayer.playerWebView.loadUrl("about:blank")
                        currentLoadedVideoId = null
                        currentCourse = null
                        currentLecture = null
                    }
                    else -> {}
                }
            }
        })

        // Tap anywhere on mini player bar to expand into full player
        val expandListener = View.OnClickListener {
            playerBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
        binding.slidingPlayer.miniPlayerBar.setOnClickListener(expandListener)
        binding.slidingPlayer.ivMiniThumbnail.setOnClickListener(expandListener)
        binding.slidingPlayer.tvMiniTitle.setOnClickListener(expandListener)
        binding.slidingPlayer.tvMiniSubtitle.setOnClickListener(expandListener)

        // Tap collapse chevron (down arrow) to collapse into mini player
        binding.slidingPlayer.btnCollapsePlayer.setOnClickListener {
            playerBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        // Mini player play/pause toggle
        binding.slidingPlayer.btnMiniPlayPause.setOnClickListener {
            if (isPlaying) {
                pauseVideo()
            } else {
                resumeVideo()
            }
        }

        // Mini player close button
        binding.slidingPlayer.btnMiniClose.setOnClickListener {
            pauseVideo()
            playerBehavior.isHideable = true
            playerBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    private fun setupBackPressHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (fullscreenCustomView != null) {
                    playerChromeClient?.onHideCustomView()
                } else if (playerBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                    playerBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                } else if (binding.viewPager.currentItem != MainPagerAdapter.TAB_HOME) {
                    binding.viewPager.setCurrentItem(MainPagerAdapter.TAB_HOME, true)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    // =========================================================================
    // YOUTUBE WEBVIEW PLAYER & HTML5 FULLSCREEN
    // =========================================================================

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupPlayerWebView() {
        val webView = binding.slidingPlayer.playerWebView

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
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

        playerChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (view == null) return
                fullscreenCustomView = view
                fullscreenCallback = callback

                binding.fullscreenContainer.addView(view)
                binding.fullscreenContainer.visibility = View.VISIBLE
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                hideSystemUI()
            }

            override fun onHideCustomView() {
                if (fullscreenCustomView != null) {
                    binding.fullscreenContainer.removeView(fullscreenCustomView)
                    binding.fullscreenContainer.visibility = View.GONE
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    showSystemUI()
                    fullscreenCallback?.onCustomViewHidden()
                    fullscreenCustomView = null
                    fullscreenCallback = null
                }
            }
        }

        webView.webChromeClient = playerChromeClient

        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun onPlaybackStarted() {
                runOnUiThread {
                    hidePlayerOverlay()
                    isPlaying = true
                    binding.slidingPlayer.btnMiniPlayPause.setImageResource(R.drawable.ic_pause)
                }
            }

            @JavascriptInterface
            fun onPlayerStateChange(state: Int) {
                runOnUiThread {
                    if (state == 1) { // PLAYING
                        isPlaying = true
                        binding.slidingPlayer.btnMiniPlayPause.setImageResource(R.drawable.ic_pause)
                    } else if (state == 2) { // PAUSED
                        isPlaying = false
                        binding.slidingPlayer.btnMiniPlayPause.setImageResource(R.drawable.ic_play)
                    }
                }
            }

            @JavascriptInterface
            fun onTimeUpdate(currentTimeSeconds: Float, durationSeconds: Float) {
                runOnUiThread {
                    if (durationSeconds > 0) {
                        val progress = ((currentTimeSeconds / durationSeconds) * 1000).toInt()
                        binding.slidingPlayer.pbMiniProgress.progress = progress
                    }
                    val course = currentCourse
                    val lecture = currentLecture
                    if (course != null && lecture != null) {
                        PlaybackManager.savePosition(
                            this@MainActivity,
                            course.playlistId,
                            lecture.lectureIndex,
                            currentTimeSeconds,
                            durationSeconds
                        )
                    }
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

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
    }

    private fun showSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    fun playCourse(course: Course, lecture: Lecture? = null) {
        currentCourse = course
        val lectureIndex = if (lecture != null) {
            lecture.lectureIndex
        } else {
            val savedLec = PlaybackManager.getLastLectureIndex(this, course.playlistId)
            if (savedLec > 0) savedLec else if (course.lastWatchedLectureIndex > 0) course.lastWatchedLectureIndex else 1
        }

        currentLecture = lecture ?: Lecture(
            playlistId = course.playlistId,
            lectureIndex = lectureIndex,
            title = "Lecture ${String.format("%02d", lectureIndex)} - ${course.title}",
            videoId = if (lectureIndex == 1) course.firstVideoId else null,
            thumbnailUrl = course.thumbnailUrl
        )

        // Update Mini Player UI
        updateMiniPlayerInfo(course, currentLecture!!)

        // Show player bottom sheet expanded
        playerBehavior.isHideable = false
        playerBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        // Show instant cached thumbnail overlay with loading spinner
        val thumbUrl = if (!currentLecture!!.videoId.isNullOrEmpty()) {
            "https://i.ytimg.com/vi/${currentLecture!!.videoId}/hqdefault.jpg"
        } else {
            currentLecture!!.thumbnailUrl ?: course.thumbnailUrl
        }
        showPlayerOverlay(thumbUrl)

        // Setup Queue RecyclerView
        setupQueueRecyclerView(course, currentLecture!!)

        val activeLec = currentLecture
        if (activeLec != null) {
            WatchHistoryManager.recordVideoWatch(
                context = this,
                playlistId = course.playlistId,
                courseCode = course.courseCode,
                courseTitle = course.title,
                lectureIndex = activeLec.lectureIndex,
                lectureTitle = activeLec.title,
                videoId = activeLec.videoId ?: course.firstVideoId,
                thumbnailUrl = activeLec.thumbnailUrl ?: course.thumbnailUrl
            )
        }

        // Check for saved timestamp resume position
        val savedPosition = PlaybackManager.getPosition(this, course.playlistId, currentLecture!!.lectureIndex)

        val targetVideoId = if (!currentLecture!!.videoId.isNullOrEmpty()) {
            currentLecture!!.videoId
        } else if (currentLecture!!.lectureIndex == 1 && !course.firstVideoId.isNullOrEmpty()) {
            course.firstVideoId
        } else {
            null
        }

        loadPlayerHtml(targetVideoId, course.playlistId, currentLecture!!.lectureIndex - 1, savedPosition)

        loadRealLectures(course)
    }

    private fun updateMiniPlayerInfo(course: Course, lecture: Lecture) {
        binding.slidingPlayer.tvMiniTitle.text = lecture.title
        binding.slidingPlayer.tvMiniSubtitle.text = "${course.courseCode ?: course.title} • Virtual University"
        val thumb = if (!lecture.videoId.isNullOrEmpty()) {
            "https://i.ytimg.com/vi/${lecture.videoId}/hqdefault.jpg"
        } else {
            lecture.thumbnailUrl ?: course.thumbnailUrl
        }
        binding.slidingPlayer.ivMiniThumbnail.load(thumb) {
            crossfade(true)
        }
        binding.slidingPlayer.tvExpandedCourseTitle.text = "${course.courseCode ?: ""} • ${course.title}"
    }

    private fun showPlayerOverlay(thumbnailUrl: String?) {
        val iv = binding.slidingPlayer.ivPlayerThumbnail
        val pb = binding.slidingPlayer.pbPlayerLoader
        iv.animate().cancel()
        iv.alpha = 1f
        iv.visibility = View.VISIBLE
        pb.visibility = View.VISIBLE

        if (!thumbnailUrl.isNullOrEmpty()) {
            iv.load(thumbnailUrl) {
                crossfade(true)
            }
        }

        iv.removeCallbacks(autoDismissOverlayRunnable)
        iv.postDelayed(autoDismissOverlayRunnable, 6000)
    }

    private fun hidePlayerOverlay() {
        val iv = binding.slidingPlayer.ivPlayerThumbnail
        val pb = binding.slidingPlayer.pbPlayerLoader
        iv.removeCallbacks(autoDismissOverlayRunnable)
        pb.visibility = View.GONE
        if (iv.visibility == View.VISIBLE) {
            iv.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    iv.visibility = View.GONE
                    iv.alpha = 1f
                }
                .start()
        }
    }

    private fun setupQueueRecyclerView(course: Course, lecture: Lecture) {
        playerHeaderAdapter = PlayerHeaderAdapter(course, lecture) {
            openCurrentOnYoutube()
        }

        currentLectures = repository.generateLectures(course)
        queueAdapter = LectureAdapter(currentLectures) { selectedLecture ->
            switchLecture(selectedLecture)
        }

        val concatAdapter = ConcatAdapter(playerHeaderAdapter, queueAdapter)

        binding.slidingPlayer.rvPlayerContent.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = concatAdapter
            setHasFixedSize(true)
        }

        lifecycleScope.launch {
            repository.updateWatchProgress(course.playlistId, lecture.lectureIndex)
        }
    }

    private fun switchLecture(lecture: Lecture) {
        currentLecture = lecture
        playerHeaderAdapter?.updateLecture(lecture)
        updateMiniPlayerInfo(currentCourse!!, lecture)

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

        val savedPosition = PlaybackManager.getPosition(this, plId, lecture.lectureIndex)
        binding.slidingPlayer.playerWebView.evaluateJavascript("playLecture('$vid', '$plId', $idx, $savedPosition);", null)
    }

    private fun playNextLecture() {
        val nextIndex = (currentLecture?.lectureIndex ?: 1) + 1
        val nextLecture = currentLectures.find { it.lectureIndex == nextIndex }
        if (nextLecture != null) {
            switchLecture(nextLecture)
        }
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

    private fun loadPlayerHtml(videoId: String?, playlistId: String, index: Int, startSeconds: Float) {
        currentLoadedVideoId = videoId
        val appOrigin = "https://$packageName"
        val startSecInt = if (startSeconds > 3) startSeconds.toInt() else 0

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
                    'start': $startSecInt,
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
                    'start': $startSecInt,
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
                    var timeTrackingInterval = null;
                    var initialResumeSeconds = $startSeconds;

                    function onYouTubeIframeAPIReady() {
                        try {
                            $playerInitScript
                        } catch(e) {
                            fallbackDirect();
                        }
                    }

                    function onPlayerReady(event) {
                        try {
                            if (initialResumeSeconds > 3) {
                                event.target.seekTo(initialResumeSeconds, true);
                            }
                            event.target.playVideo();
                        } catch(e) {}
                    }

                    function onPlayerStateChange(event) {
                        if (window.AndroidBridge && typeof window.AndroidBridge.onPlayerStateChange === 'function') {
                            window.AndroidBridge.onPlayerStateChange(event.data);
                        }
                        if (event.data === 1 || event.data === 3) { // PLAYING or BUFFERING
                            startTimeTracking();
                            if (window.AndroidBridge) window.AndroidBridge.onPlaybackStarted();
                        } else if (event.data === 2) { // PAUSED
                            stopTimeTracking();
                            reportTime();
                        } else if (event.data === 0) { // ENDED
                            stopTimeTracking();
                            if (window.AndroidBridge) window.AndroidBridge.onVideoEnded();
                        }
                    }

                    function startTimeTracking() {
                        stopTimeTracking();
                        timeTrackingInterval = setInterval(reportTime, 1500);
                    }

                    function stopTimeTracking() {
                        if (timeTrackingInterval) {
                            clearInterval(timeTrackingInterval);
                            timeTrackingInterval = null;
                        }
                    }

                    function reportTime() {
                        try {
                            if (player && typeof player.getCurrentTime === 'function' && window.AndroidBridge) {
                                var cur = player.getCurrentTime();
                                var dur = player.getDuration();
                                if (cur >= 0 && dur > 0) {
                                    window.AndroidBridge.onTimeUpdate(cur, dur);
                                }
                            }
                        } catch(e) {}
                    }

                    function onPlayerError(event) {
                        fallbackDirect();
                    }

                    function fallbackDirect() {
                        var v = '${videoId ?: ""}';
                        var pl = '$playlistId';
                        var idx = $index;
                        var st = Math.floor(initialResumeSeconds);
                        var startParam = (st > 3) ? '&start=' + st : '';
                        if (v !== '') {
                            document.body.innerHTML = '<iframe id="player" src="https://www.youtube-nocookie.com/embed/' + v + '?autoplay=1&playsinline=1&rel=0&controls=1' + startParam + '" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; fullscreen" allowfullscreen></iframe>';
                        } else if (pl !== '') {
                            document.body.innerHTML = '<iframe id="player" src="https://www.youtube-nocookie.com/embed?listType=playlist&list=' + pl + '&index=' + idx + '&autoplay=1&playsinline=1&rel=0&controls=1' + startParam + '" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; fullscreen" allowfullscreen></iframe>';
                        }
                        setTimeout(function() {
                            if (window.AndroidBridge) window.AndroidBridge.onPlaybackStarted();
                        }, 1000);
                    }

                    function playLecture(vid, plId, idx, startSec) {
                        var st = startSec || 0;
                        try {
                            if (vid && vid !== '') {
                                if (player && typeof player.loadVideoById === 'function') {
                                    player.loadVideoById({
                                        videoId: vid,
                                        startSeconds: st
                                    });
                                } else {
                                    var sp = (st > 3) ? '&start=' + Math.floor(st) : '';
                                    document.body.innerHTML = '<iframe id="player" src="https://www.youtube-nocookie.com/embed/' + vid + '?autoplay=1&playsinline=1&rel=0&controls=1' + sp + '" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; fullscreen" allowfullscreen></iframe>';
                                    setTimeout(function() { if (window.AndroidBridge) window.AndroidBridge.onPlaybackStarted(); }, 1000);
                                }
                            } else if (plId && plId !== '') {
                                if (player && typeof player.loadPlaylist === 'function') {
                                    player.loadPlaylist({
                                        list: plId,
                                        listType: 'playlist',
                                        index: Math.max(0, idx),
                                        startSeconds: st
                                    });
                                } else {
                                    var sp = (st > 3) ? '&start=' + Math.floor(st) : '';
                                    document.body.innerHTML = '<iframe id="player" src="https://www.youtube-nocookie.com/embed?listType=playlist&list=' + plId + '&index=' + idx + '&autoplay=1&playsinline=1&rel=0&controls=1' + sp + '" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; fullscreen" allowfullscreen></iframe>';
                                    setTimeout(function() { if (window.AndroidBridge) window.AndroidBridge.onPlaybackStarted(); }, 1000);
                                }
                            }
                        } catch(e) {
                            if (vid && vid !== '') {
                                document.body.innerHTML = '<iframe id="player" src="https://www.youtube-nocookie.com/embed/' + vid + '?autoplay=1&playsinline=1&rel=0&controls=1" frameborder="0" allowfullscreen></iframe>';
                            }
                        }
                    }

                    function pauseVideo() {
                        if (player && typeof player.pauseVideo === 'function') {
                            player.pauseVideo();
                        }
                    }

                    function resumeVideo() {
                        if (player && typeof player.playVideo === 'function') {
                            player.playVideo();
                        }
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        binding.slidingPlayer.playerWebView.loadDataWithBaseURL(
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
                queueAdapter?.updateLectures(real)
                val matching = real.find { it.lectureIndex == currentLecture?.lectureIndex }
                if (matching != null && currentLecture?.videoId.isNullOrEmpty()) {
                    currentLecture = matching
                    playerHeaderAdapter?.updateLecture(matching)
                    updateMiniPlayerInfo(course, matching)
                    WatchHistoryManager.recordVideoWatch(
                        context = this@MainActivity,
                        playlistId = course.playlistId,
                        courseCode = course.courseCode,
                        courseTitle = course.title,
                        lectureIndex = matching.lectureIndex,
                        lectureTitle = matching.title,
                        videoId = matching.videoId ?: course.firstVideoId,
                        thumbnailUrl = matching.thumbnailUrl ?: course.thumbnailUrl
                    )
                }
            }
        }
    }

    private fun pauseVideo() {
        isPlaying = false
        binding.slidingPlayer.btnMiniPlayPause.setImageResource(R.drawable.ic_play)
        binding.slidingPlayer.playerWebView.evaluateJavascript("pauseVideo();", null)
    }

    private fun resumeVideo() {
        isPlaying = true
        binding.slidingPlayer.btnMiniPlayPause.setImageResource(R.drawable.ic_pause)
        binding.slidingPlayer.playerWebView.evaluateJavascript("resumeVideo();", null)
    }

    override fun onPause() {
        super.onPause()
        binding.slidingPlayer.playerWebView.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.slidingPlayer.playerWebView.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.slidingPlayer.ivPlayerThumbnail.removeCallbacks(autoDismissOverlayRunnable)
        binding.slidingPlayer.playerWebView.destroy()
    }
}
