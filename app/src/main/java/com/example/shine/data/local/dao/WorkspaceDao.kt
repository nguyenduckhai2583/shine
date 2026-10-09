package com.example.shine.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.shine.data.local.entity.WorkspaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {

    @Query("SELECT * FROM workspaces")
    fun observeWorkspaces(): Flow<List<WorkspaceEntity>>

    @Query("SELECT * FROM workspaces")
    suspend fun getWorkspaces(): List<WorkspaceEntity>

    @Query("SELECT * FROM workspaces WHERE id = :id")
    suspend fun getWorkspace(id: String): WorkspaceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkspaces(workspaces: List<WorkspaceEntity>)

    @Query("DELETE FROM workspaces")
    suspend fun clear()

    @Transaction
    suspend fun replaceWorkspaces(workspaces: List<WorkspaceEntity>) {
        clear()
        upsertWorkspaces(workspaces)
    }
}
