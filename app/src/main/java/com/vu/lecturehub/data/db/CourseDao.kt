package com.vu.lecturehub.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vu.lecturehub.data.model.Course
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {

    @Query("SELECT * FROM courses")
    fun getAllCourses(): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE isBookmarked = 1")
    fun getBookmarkedCourses(): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE lastWatchedTimestamp > 0 ORDER BY lastWatchedTimestamp DESC")
    fun getRecentlyWatchedCourses(): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE playlistId = :id LIMIT 1")
    suspend fun getCourseById(id: String): Course?

    @Query("SELECT COUNT(*) FROM courses")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(courses: List<Course>)

    @Update
    suspend fun update(course: Course)

    @Query("UPDATE courses SET isBookmarked = :bookmarked WHERE playlistId = :id")
    suspend fun setBookmarked(id: String, bookmarked: Boolean)

    @Query("UPDATE courses SET lastWatchedTimestamp = :timestamp, lastWatchedLectureIndex = :lectureIndex WHERE playlistId = :id")
    suspend fun updateWatchProgress(id: String, lectureIndex: Int, timestamp: Long)
}
