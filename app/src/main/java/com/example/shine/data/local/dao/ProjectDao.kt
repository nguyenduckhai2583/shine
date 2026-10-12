package com.example.shine.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.shine.data.local.entity.ProjectEntity

@Dao
interface ProjectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<ProjectEntity>)

    @Query("SELECT * FROM projects")
    fun getProjectsPagingSource(): PagingSource<Int, ProjectEntity>

    @Query("DELETE FROM projects")
    suspend fun clearProjects()
}
