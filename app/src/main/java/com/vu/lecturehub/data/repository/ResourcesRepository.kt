package com.vu.lecturehub.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vu.lecturehub.data.model.Course
import com.vu.lecturehub.data.model.HandoutCourse
import com.vu.lecturehub.data.model.ResourceLink
import java.io.InputStreamReader

object ResourcesRepository {

    private var cachedHandouts: List<HandoutCourse>? = null
    private var cachedDepartments: List<String>? = null

    val links: List<ResourceLink> = listOf(
        ResourceLink("Virtual University LMS (VULMS)", "https://vulms.vu.edu.pk"),
        ResourceLink("VU Official Website", "https://www.vu.edu.pk"),
        ResourceLink("VU Digital Library", "https://library.vu.edu.pk"),
        ResourceLink("VU Date Sheet System", "https://datesheet.vu.edu.pk"),
        ResourceLink("Official YouTube Channel", "https://www.youtube.com/@VirtualUniversityofPakistan"),
        ResourceLink("Student Notice Board", "https://www.vu.edu.pk/NewsNotice.aspx"),
        ResourceLink("VU Admissions Portal", "https://www.vu.edu.pk/apply"),
        ResourceLink("VU Past Papers & Solutions", "https://www.google.com/search?q=Virtual+University+past+papers+filetype:pdf")
    )

    fun getHandouts(context: Context): List<HandoutCourse> {
        if (cachedHandouts != null) return cachedHandouts!!
        val list = loadHandoutsFromAssets(context)
        cachedHandouts = list
        return list
    }

    fun getDepartments(context: Context): List<String> {
        if (cachedDepartments != null) return cachedDepartments!!
        val allHandouts = getHandouts(context)
        val depts = allHandouts.map { it.department }.distinct().sorted()
        val result = listOf("All") + depts
        cachedDepartments = result
        return result
    }

    fun getHandoutsByDepartment(context: Context, department: String): List<HandoutCourse> {
        val all = getHandouts(context)
        return if (department.equals("All", ignoreCase = true)) {
            all
        } else {
            all.filter { it.department.equals(department, ignoreCase = true) }
        }
    }

    fun searchHandouts(context: Context, query: String): List<HandoutCourse> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return getHandouts(context).filter {
            it.courseCode.lowercase().contains(q) ||
            it.title.lowercase().contains(q) ||
            it.department.lowercase().contains(q)
        }
    }

    fun searchLinks(query: String): List<ResourceLink> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return links.filter { it.title.lowercase().contains(q) }
    }

    fun openHandoutInBrowser(context: Context, courseCode: String, courseTitle: String = "") {
        val cleanTitle = courseTitle.replace(Regex("^$courseCode\\s*[-–—:]?\\s*", RegexOption.IGNORE_CASE), "").trim()
        val query = if (cleanTitle.isNotEmpty()) {
            "$courseCode $cleanTitle VU handouts filetype:pdf"
        } else {
            "$courseCode VU handouts filetype:pdf"
        }
        val url = "https://www.google.com/search?q=" + Uri.encode(query)
        openUrlInBrowser(context, url)
    }

    fun openUrlInApp(context: Context, url: String, title: String? = null) {
        try {
            val intent = Intent(context, com.vu.lecturehub.ui.webview.WebViewActivity::class.java).apply {
                putExtra(com.vu.lecturehub.ui.webview.WebViewActivity.EXTRA_URL, url)
                putExtra(com.vu.lecturehub.ui.webview.WebViewActivity.EXTRA_TITLE, title)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            openUrlInBrowser(context, url)
        }
    }

    fun openUrlInBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadHandoutsFromAssets(context: Context): List<HandoutCourse> {
        val courses = try {
            context.assets.open("courses.json").use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val type = object : TypeToken<List<Course>>() {}.type
                    Gson().fromJson<List<Course>>(reader, type) ?: emptyList()
                }
            }
        } catch (e: Exception) {
            emptyList()
        }

        val codeRegex = Regex("^([A-Za-z]{2,5}\\d{3,4})")
        val cleanTitleRegex = Regex("^([A-Za-z]{2,5}\\d{3,4})\\s*[-–—:]?\\s*")

        val result = mutableListOf<HandoutCourse>()
        val seenCodes = mutableSetOf<String>()

        for (c in courses) {
            val code = when {
                !c.courseCode.isNullOrBlank() -> c.courseCode.trim()
                codeRegex.containsMatchIn(c.title) -> codeRegex.find(c.title)?.value ?: ""
                else -> ""
            }

            if (code.isNotEmpty() && !seenCodes.contains(code)) {
                seenCodes.add(code)
                val cleanTitle = cleanTitleRegex.replace(c.title, "").trim().ifEmpty { c.title }
                val normalizedDept = normalizeDepartment(c.department)
                result.add(
                    HandoutCourse(
                        courseCode = code,
                        title = cleanTitle,
                        department = normalizedDept
                    )
                )
            }
        }

        return result.sortedBy { it.courseCode }
    }

    private fun normalizeDepartment(dept: String): String {
        val trimmed = dept.trim()
        return when {
            trimmed.contains("BIF", ignoreCase = true) -> "Bioinformatics"
            trimmed.contains("ECE", ignoreCase = true) -> "Electrical Engineering"
            trimmed.contains("EDUA", ignoreCase = true) -> "Education"
            trimmed.contains("ETH", ignoreCase = true) -> "Ethics"
            trimmed.contains("GSC", ignoreCase = true) -> "General Science"
            trimmed.contains("MB Department", ignoreCase = true) -> "Molecular Biology"
            trimmed.contains("MCD", ignoreCase = true) -> "Mass Communication"
            trimmed.contains("MGTE", ignoreCase = true) -> "Management"
            trimmed.contains("MIC", ignoreCase = true) -> "Microbiology"
            trimmed.contains("PSC", ignoreCase = true) -> "Pakistan Studies"
            trimmed.contains("PSYP", ignoreCase = true) -> "Psychology"
            trimmed.equals("GEN Department", ignoreCase = true) ||
            trimmed.equals("General / Other", ignoreCase = true) ||
            trimmed.equals("VU Department", ignoreCase = true) -> "General"
            else -> trimmed
        }
    }
}
