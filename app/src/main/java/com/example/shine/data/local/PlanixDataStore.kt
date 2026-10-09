package com.example.shine.data.local

import com.example.shine.domain.model.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanixDataStore @Inject constructor() {

    private val _projectsState = MutableStateFlow<List<Project>>(emptyList())
    val projectsFlow: Flow<List<Project>> = _projectsState.asStateFlow()

    fun getProjects(): List<Project> = _projectsState.value

    fun replaceProjects(projects: List<Project>) {
        _projectsState.value = projects
    }

    fun clear() {
        _projectsState.value = emptyList()
    }
}
