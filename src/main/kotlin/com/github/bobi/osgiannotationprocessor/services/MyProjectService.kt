package com.github.bobi.osgiannotationprocessor.services

import com.intellij.openapi.project.Project
import com.github.bobi.osgiannotationprocessor.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
