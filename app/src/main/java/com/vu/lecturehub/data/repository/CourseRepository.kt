package com.vu.lecturehub.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.vu.lecturehub.data.db.AppDatabase
import com.vu.lecturehub.data.model.Course
import com.vu.lecturehub.data.model.Department
import com.vu.lecturehub.data.model.Lecture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class CourseRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val courseDao = db.courseDao()

    val allCourses: Flow<List<Course>> = courseDao.getAllCourses()
    val bookmarkedCourses: Flow<List<Course>> = courseDao.getBookmarkedCourses()
    val recentlyWatchedCourses: Flow<List<Course>> = courseDao.getRecentlyWatchedCourses()

    suspend fun initializeDatabaseIfNeeded() = withContext(Dispatchers.IO) {
        val count = courseDao.getCount()
        if (count == 0) {
            val coursesFromAssets = loadCoursesFromAssets()
            if (coursesFromAssets.isNotEmpty()) {
                courseDao.insertAll(coursesFromAssets)
            }
        }
    }

    private fun loadCoursesFromAssets(): List<Course> {
        return try {
            context.assets.open("courses.json").use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val type = object : TypeToken<List<Course>>() {}.type
                    Gson().fromJson(reader, type) ?: emptyList()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun toggleBookmark(course: Course) = withContext(Dispatchers.IO) {
        val newStatus = !course.isBookmarked
        courseDao.setBookmarked(course.playlistId, newStatus)
    }

    suspend fun updateWatchProgress(playlistId: String, lectureIndex: Int) = withContext(Dispatchers.IO) {
        courseDao.updateWatchProgress(playlistId, lectureIndex, System.currentTimeMillis())
    }

    companion object {
        private val playlistCache = java.util.concurrent.ConcurrentHashMap<String, List<Lecture>>()

        fun getCachedLectures(playlistId: String): List<Lecture>? = playlistCache[playlistId]
    }

    suspend fun getCourseById(playlistId: String): Course? = withContext(Dispatchers.IO) {
        courseDao.getCourseById(playlistId)
    }

    fun generateLectures(course: Course): List<Lecture> {
        val cached = playlistCache[course.playlistId]
        if (!cached.isNullOrEmpty()) {
            return cached
        }
        val count = if (course.videoCount > 0) course.videoCount else 30
        val generated = (1..count).map { idx ->
            val formattedIdx = String.format("%02d", idx)
            Lecture(
                playlistId = course.playlistId,
                lectureIndex = idx,
                title = "Lecture $formattedIdx - ${course.courseCode ?: course.title}",
                videoId = if (idx == 1) course.firstVideoId else null,
                thumbnailUrl = course.thumbnailUrl,
                isCompleted = idx < course.lastWatchedLectureIndex
            )
        }
        playlistCache.putIfAbsent(course.playlistId, generated)
        return generated
    }

    suspend fun fetchPlaylistVideos(course: Course): List<Lecture> = withContext(Dispatchers.IO) {
        val cached = playlistCache[course.playlistId]
        if (!cached.isNullOrEmpty() && cached.any { it.lectureIndex > 1 && it.videoId != null }) {
            return@withContext cached
        }
        try {
            val url = URL("https://www.youtube.com/youtubei/v1/browse?key=AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            conn.doOutput = true

            val requestBody = """
                {
                    "context": {
                        "client": {
                            "clientName": "WEB",
                            "clientVersion": "2.20260921.01.00",
                            "hl": "en",
                            "gl": "US"
                        }
                    },
                    "browseId": "VL${course.playlistId}"
                }
            """.trimIndent()

            conn.outputStream.use { os ->
                os.write(requestBody.toByteArray())
            }

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JsonParser.parseString(responseText).asJsonObject
                val tabs = jsonObject.getAsJsonObject("contents")
                    ?.getAsJsonObject("twoColumnBrowseResultsRenderer")
                    ?.getAsJsonArray("tabs")
                val contents = tabs?.get(0)?.asJsonObject
                    ?.getAsJsonObject("tabRenderer")
                    ?.getAsJsonObject("content")
                    ?.getAsJsonObject("sectionListRenderer")
                    ?.getAsJsonArray("contents")
                val items = contents?.get(0)?.asJsonObject
                    ?.getAsJsonObject("itemSectionRenderer")
                    ?.getAsJsonArray("contents")

                val lectures = mutableListOf<Lecture>()
                var idx = 1
                if (items != null) {
                    for (elem in items) {
                        val itemObj = elem.asJsonObject
                        var videoId: String? = null
                        var title: String? = null

                        if (itemObj.has("lockupViewModel")) {
                            val lockup = itemObj.getAsJsonObject("lockupViewModel")
                            videoId = lockup.get("contentId")?.asString
                            title = lockup.getAsJsonObject("metadata")
                                ?.getAsJsonObject("lockupMetadataViewModel")
                                ?.getAsJsonObject("title")
                                ?.get("content")?.asString
                        } else if (itemObj.has("playlistVideoRenderer")) {
                            val plVideo = itemObj.getAsJsonObject("playlistVideoRenderer")
                            videoId = plVideo.get("videoId")?.asString
                            title = plVideo.getAsJsonObject("title")
                                ?.getAsJsonArray("runs")?.get(0)?.asJsonObject
                                ?.get("text")?.asString
                        }

                        if (!videoId.isNullOrEmpty()) {
                            val formattedNum = String.format("%02d", idx)
                            lectures.add(
                                Lecture(
                                    playlistId = course.playlistId,
                                    lectureIndex = idx,
                                    title = title ?: "Lecture $formattedNum - ${course.title}",
                                    videoId = videoId,
                                    thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                                    isCompleted = idx < course.lastWatchedLectureIndex
                                )
                            )
                            idx++
                        }
                    }
                }

                if (lectures.isNotEmpty()) {
                    playlistCache[course.playlistId] = lectures
                    return@withContext lectures
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext generateLectures(course)
    }

    fun extractDepartments(courses: List<Course>): List<Department> {
        val counts = mutableMapOf<String, Int>()
        for (c in courses) {
            counts[c.department] = (counts[c.department] ?: 0) + 1
        }
        val list = counts.map { (dept, cnt) ->
            Department(name = dept, courseCount = cnt, isSelected = false)
        }.sortedByDescending { it.courseCount }

        return listOf(Department("All", courses.size, isSelected = true)) + list
    }
}
