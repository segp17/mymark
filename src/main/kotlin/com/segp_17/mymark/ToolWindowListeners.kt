package com.segp_17.mymark

import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileMoveEvent
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import io.ktor.util.reflect.*


// Listens for the MyMark window being opened and updates the currently focused file
class MyMarkToolWindowManagerListener(val project: Project): ToolWindowManagerListener {
    // On register
    override fun toolWindowsRegistered(ids: MutableList<String>, toolWindowManager: ToolWindowManager) {
        super.toolWindowsRegistered(ids, toolWindowManager)
    }

    // On shown, update current file
    override fun toolWindowShown(toolWindow: ToolWindow) {
        // not needed since we update whenever switching files
//        if ("MyMark" == toolWindow.id) {
//            val content = getContent(toolWindow)
//            content.getCurrentFile()
//        }
    }

    private fun getContent(toolWindow: ToolWindow): MyMarkToolWindowFactory.MyMarkToolWindowContent {
            return toolWindow.contentManager.contents[0].component as MyMarkToolWindowFactory.MyMarkToolWindowContent
    }
}

// Listens for the user switching file, to update current file
class MyMarkFileEditorManagerListener(): FileEditorManagerListener {
    override fun selectionChanged(event: FileEditorManagerEvent) {
        val toolWindowManager = ToolWindowManager.getInstance(event.manager.project)
        val toolWindow = toolWindowManager.getToolWindow("MyMark")
        if (toolWindow != null) {
            val content = getContent(toolWindow)
            content.getCurrentFile()
        }
    }

    private fun getContent(toolWindow: ToolWindow): MyMarkToolWindowFactory.MyMarkToolWindowContent {
        return toolWindow.contentManager.contents[0].component as MyMarkToolWindowFactory.MyMarkToolWindowContent
    }
}

// Listens for file rename/creation/deletion/move/copy to update list of files
class MyMarkBulkFileListener(private val project: Project) :BulkFileListener{
    override fun after(events: MutableList<out VFileEvent>) {
        for (event in events) {
            if (event.instanceOf(VFileCopyEvent::class) || event.instanceOf(VFileDeleteEvent::class)
                || event.instanceOf(VFileCreateEvent::class) || event.instanceOf(VirtualFileMoveEvent::class) || event.instanceOf(VFilePropertyChangeEvent::class)) {
                updateAllFiles()
            }
        }
    }

    // Updates files in tool window
    private fun updateAllFiles() {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("MyMark")
        if (toolWindow != null) {
            val content = getContent(toolWindow)
            content.refreshFileTree()
        }
    }

    private fun getContent(toolWindow: ToolWindow): MyMarkToolWindowFactory.MyMarkToolWindowContent {
        return toolWindow.contentManager.contents[0].component as MyMarkToolWindowFactory.MyMarkToolWindowContent
    }
}
