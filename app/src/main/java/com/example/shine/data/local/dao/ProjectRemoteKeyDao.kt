package com.example.shine.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.shine.data.local.entity.ProjectRemoteKeyEntity

@Dao
interface ProjectRemoteKeyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(remoteKeys: List<ProjectRemoteKeyEntity>)

    @Query("SELECT * FROM project_remote_keys WHERE projectId = :projectId")
    suspend fun getRemoteKeyByProjectId(projectId: String): ProjectRemoteKeyEntity?

    @Query("DELETE FROM project_remote_keys")
    suspend fun clearRemoteKeys()
}
