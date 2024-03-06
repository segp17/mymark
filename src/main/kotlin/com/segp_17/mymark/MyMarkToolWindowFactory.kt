package com.segp_17.mymark

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vfs.*
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckboxTree.CheckboxTreeCellRenderer
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.awt.*
import java.awt.event.ActionListener
import java.io.File
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode


class MyMarkToolWindowFactory : ToolWindowFactory, DumbAware {
    // Create the tool window content
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowContent = MyMarkToolWindowContent(toolWindow, project)
        val content = ContentFactory.getInstance().createContent(toolWindowContent, "", false)
        toolWindow.contentManager.addContent(content)

        // Register ToolWindowManagerListener
        val messageBus = project.messageBus
        messageBus.connect().subscribe(ToolWindowManagerListener.TOPIC,MyMarkToolWindowManagerListener(project))

        // Register FileManagerEditorListener
        messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, MyMarkFileEditorManagerListener())

        // Register VirtualFileListener
        messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, MyMarkBulkFileListener(project))
    }

    class MyMarkToolWindowContent(toolWindow: ToolWindow, val project: Project): SimpleToolWindowPanel(true,true) {
        // Intialise JSwing components
        val chatTextArea = JTextArea()
        private val scrollPane = JBScrollPane(chatTextArea)
        val messageTextField = JTextField()
        val sendButton = JButton("Send")
        private lateinit var fileTree: CheckboxTree
        private lateinit var treeScrollPane: JBScrollPane
        val moduleDropdown: ComboBox<String>
        val exerciseDropdown: ComboBox<String>
        private val reloadButton = JButton("Reload")
        private var foundFiles = false


        private var topPanel = JPanel(BorderLayout())
        private val bottomPanel = JPanel(BorderLayout())
        private val rowPanel = JPanel(FlowLayout())

        // Create API Service
        private val baseUrl = "http://146.169.43.198:8080/"
        private val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        var apiService = retrofit.create<ApiService>()

        // Messages stores conversation
        val messages = ArrayList<Message>()

        // Current file in focus
        private var currentFile: String? = null

        // Map of filepaths to nodes
        private val nodeMap: MutableMap<String,FileCheckedTreeNode> = HashMap()

        // Arrays of module and exercise names
        var modules: Array<String> = arrayOf("Loading...")
        var exercises: Array<String> = arrayOf("Loading...")

        private var waitingForResponse = false

        init {


            getCurrentFile() // Gets current file

            setupTree() // Setup tree

            reloadModulesAndExercises()

            // Create lateinit components
            moduleDropdown = ComboBox(modules)
            exerciseDropdown = ComboBox(exercises)

            moduleDropdown.addActionListener { // Code to be executed when the selected option changes
                exercises = arrayOf("Loading...")
                val newDropdown = ComboBox(exercises)
                exerciseDropdown.model = newDropdown.model
                updateExercises()
            }

            // Register listeners

            sendButton.addActionListener(ActionListener {
                    sendMessage()
            })
            reloadButton.addActionListener {
                reloadModulesAndExercises()
            }


            messageTextField.addActionListener(ActionListener { // This code will be executed when Enter is pressed in the text field
                val enteredText: String = messageTextField.getText()
                if (sendButton.isEnabled) {
                    sendMessage()
                }
            })

            // Edit component properties

            chatTextArea.isEditable = false
            chatTextArea.setLineWrap(true); // Enable line wrap
            chatTextArea.setWrapStyleWord(true); // Wrap at word boundaries
            scrollPane.preferredSize = Dimension(100,200)
            sendButton.isEnabled = false;

            layout = GridBagLayout()

            add(scrollPane,gridBagConstraints(GridBagConstraints.BOTH,1.0,0.5,0,0,1,3))
            add(treeScrollPane,gridBagConstraints(GridBagConstraints.HORIZONTAL,1.0,0.3,0,1,1,3))
            add(moduleDropdown, gridBagConstraints(GridBagConstraints.NONE, 1.0,0.1,0,2,1,1))
            add(exerciseDropdown, gridBagConstraints(GridBagConstraints.NONE, 1.0,0.1,1,2,1,1))
            add(reloadButton, gridBagConstraints(GridBagConstraints.NONE, 1.0,0.1,2,2,1,1))
            add(messageTextField,gridBagConstraints(GridBagConstraints.HORIZONTAL,1.0,0.1,0,3,1,2))
            add(sendButton,gridBagConstraints(GridBagConstraints.NONE,1.0,0.1,2,3,1,1))
        }

        // Setup file tree
        private fun setupTree() {
            val root = buildTreeNew()
            if (root != null) {
                fileTree = CheckboxTree(FileCheckboxTreeCellRenderer(), root)
                fileTree.isRootVisible = true

                // Record when file is checked/unchecked
//            fileTree.addCheckboxTreeListener(object : CheckboxTreeListener {
//                override fun nodeStateChanged(node: CheckedTreeNode) {
//                }
//            })
                fileTree.visibleRowCount = 10
                treeScrollPane = JBScrollPane(fileTree)
            }
        }

        private fun gridBagConstraints(fill: Int, weightx: Double, weighty: Double, gridx: Int, gridy: Int, gridheight: Int, gridwidth: Int): GridBagConstraints {
            val gbc = GridBagConstraints()
            gbc.fill = fill
            gbc.weightx = weightx
            gbc.weighty = weighty
            gbc.gridx = gridx
            gbc.gridy = gridy
            gbc.gridheight = gridheight
            gbc.gridwidth = gridwidth
            return gbc
        }

        // Make requests to update modules
        fun updateModules() {

            val call = apiService.fetchModules()
            call.enqueue(object : Callback<Modules> {
                override fun onResponse(call: Call<Modules>, response: Response<Modules>) {
                    if (response.isSuccessful && response.body()!!.modules.isNotEmpty()) {
                        modules = response.body()!!.modules
                        val newModuleDropdown = ComboBox(modules)
                        moduleDropdown.model = newModuleDropdown.model
                        moduleDropdown.selectedItem = modules[0]
                        updateExercises()
                    } else {
                        modules = arrayOf("Connection Error")
                        exercises = arrayOf("Connection Error")
                        sendButton.isEnabled = false
                        val newExerciseDropdown = ComboBox(exercises)
                        exerciseDropdown.model = newExerciseDropdown.model
                        val newModuleDropdown = ComboBox(modules)
                        moduleDropdown.model = newModuleDropdown.model
                    }
                }

                override fun onFailure(call: Call<Modules>, t: Throwable) {
                    modules = arrayOf("Connection Error")
                    exercises = arrayOf("Connection Error")
                    sendButton.isEnabled = false
                    val newExerciseDropdown = ComboBox(exercises)
                    exerciseDropdown.model = newExerciseDropdown.model
                    val newModuleDropdown = ComboBox(modules)
                    moduleDropdown.model = newModuleDropdown.model
                }
            })
        }

        // Get initial exercises from first module in list
        fun updateExercises(){
            if (modules.isNotEmpty() && (modules[0] != "Connection Error")) {
                        val call = apiService.fetchExercises(moduleDropdown.selectedItem as String)
                        call.enqueue(object : Callback<Exercises> {
                            override fun onResponse(call: Call<Exercises>, response: Response<Exercises>) {
                                if (response.isSuccessful) {
                                    exercises = response.body()!!.exercises
                                    sendButton.isEnabled = exercises.isNotEmpty() && !waitingForResponse
                                } else if (!response.isSuccessful){
                                    exercises = arrayOf("Connection Error")
                                    sendButton.isEnabled = false
                                }
                                val newExerciseDropdown = ComboBox(exercises)
                                exerciseDropdown.model = newExerciseDropdown.model
                                exerciseDropdown.selectedItem = if (response.isSuccessful && response.body()!!.exercises.isNotEmpty()) {
                                    exercises[0]
                                } else {
                                    null
                                }
                                // disable/enable send button
                            }
                            override fun onFailure(call: Call<Exercises>, t: Throwable) {
                                exercises = arrayOf("Connection Error")
                                val newExerciseDropdown = ComboBox(exercises)
                                exerciseDropdown.model = newExerciseDropdown.model
                                sendButton.isEnabled = false
                            }
                        })
            } else if (modules.isNotEmpty()){
                exercises = arrayOf("Connection Error")
                val newExerciseDropdown = ComboBox(exercises)
                exerciseDropdown.model = newExerciseDropdown.model
            } else {
                exercises = arrayOf()
                val newExerciseDropdown = ComboBox(exercises)
                exerciseDropdown.model = newExerciseDropdown.model
            }
        }

        private fun reloadModulesAndExercises() {
            updateModules()
        }

        // Sends message to backend server
        fun sendMessage() {
            val message = messageTextField.text
            if (message.isNotEmpty() && moduleDropdown.selectedItem != null && exerciseDropdown.selectedItem != null) {
                chatTextArea.append("User: $message\n\n")
                messageTextField.text = ""
                sendButton.isEnabled = false
            } else {
                return
            }

            val codeContents = getCodeContents()
            val newMessage = Message("user", message)
            messages.add(newMessage)

            val question = QuestionData(codeContents, messages)

            val call = apiService.askQuestion(question, exerciseDropdown.selectedItem as String, moduleDropdown.selectedItem as String)
            waitingForResponse = true
            call.enqueue(object : Callback<Answer> {
                override fun onResponse(call: Call<Answer>, response: Response<Answer>) {
                    if (response.isSuccessful) {
                        messages.add(Message("assistant", response.body()!!.answer))
                        chatTextArea.append("MyMark: ${response.body()!!.answer}\n\n")
                    } else {
                        messages.remove(newMessage)
                        chatTextArea.append("MyMark: An ERROR occurred, please try again\n\n")
                    }
                    waitingForResponse = false
                    sendButton.isEnabled = true
                }

                override fun onFailure(call: Call<Answer>, t: Throwable) {
                    messages.remove(newMessage)
                    chatTextArea.append("MyMark: An ERROR occurred, please try again\n\n")
                    sendButton.isEnabled = true
                    waitingForResponse = false
                }
            })
        }

        // Gets the currently focused file and updates attribute, also updates tree
        fun getCurrentFile() {
            val fileManager = FileEditorManager.getInstance(project)
            if  (fileManager == null) {
                currentFile = null
                return
            }
            val file = FileEditorManager.getInstance(project).selectedEditor?.file
            if (file != null) {
                if (nodeMap[currentFile] != null) {
                    nodeMap[currentFile]!!.isChecked = false
                }
                if (nodeMap[file.path] != null) {
                    nodeMap[file.path]!!.isChecked = true
                }
                currentFile = file.path
            } else {
                currentFile = null
            }
        }

        // Checked tree cell renderer
        private class FileCheckboxTreeCellRenderer: CheckboxTreeCellRenderer() {
            override fun customizeRenderer(
                tree: JTree?,
                value: Any?,
                selected: Boolean,
                expanded: Boolean,
                leaf: Boolean,
                row: Int,
                hasFocus: Boolean
            ) {
                super.customizeRenderer(tree, value, selected, expanded, leaf, row, hasFocus)
                if (value is FileCheckedTreeNode) {
                    checkbox.text = value.fileName
                } else if (value is DefaultMutableTreeNode) {
                    checkbox.text = value.toString()
                }
            }
        }

        // Builds nodes of file tree recursively
        private fun buildTreeNew(): FileCheckedTreeNode {
            if (project.basePath == null) {
                return FileCheckedTreeNode("no project found", "no project found")
            }
            val localFileSystem = LocalFileSystem.getInstance()
            val baseDir = localFileSystem.findFileByPath(project.basePath!!)
                ?: return FileCheckedTreeNode("no project found", "no project found")
            val root = FileCheckedTreeNode(project.basePath!!, baseDir!!.name)
            nodeMap[project.basePath!!] = root
            root.isChecked = false
            if (baseDir.isDirectory) {
                for (childrenFile: VirtualFile in baseDir.children) {
                    val childNode = buildTreeHelper(childrenFile)
                    root.add(childNode)
                }
            }
            foundFiles = true
            return root
        }

        // Helper (same as above but no root)
        private fun buildTreeHelper(file: VirtualFile): FileCheckedTreeNode {
            val node = FileCheckedTreeNode(file.path, file.name)
            nodeMap[file.path] = node
            node.isChecked = false
            if (file.path == currentFile) {
                node.isChecked = true
            }
            if (file.isDirectory) {
                for (childrenFile: VirtualFile in file.children) {
                    val childNode = buildTreeHelper(childrenFile)
                    node.add(childNode)
                }
            }
            return node
        }

        // File node also records file path
        class FileCheckedTreeNode(userObject: Any, val fileName: String) : CheckedTreeNode(userObject) {
        }

        // Refreshes file tree
        fun refreshFileTree() {
            val newRoot = buildTreeNew()
            val newTree = CheckboxTree(FileCheckboxTreeCellRenderer(),newRoot)
            fileTree.model = newTree.model
        }

        // Gets code contents of all selected files
        private fun getCodeContents():String {
            if (!foundFiles) {
                return ""
            }
            val filePaths = fileTree.getCheckedNodes(String::class.java,null)
            val sb = StringBuilder()
            for (filePath in filePaths) {
                sb.append(filePath+"\n\n\n")
                val file = File(filePath)
                val code = file.readText()
                sb.append(code+"\n")
            }
            return sb.toString()
        }
    }
}
