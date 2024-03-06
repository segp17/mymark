package com.segp_17.mymark

import com.intellij.mock.MockApplication
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.*
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.toolWindow.ToolWindowHeadlessManagerImpl.MockToolWindow
import com.intellij.ui.content.ContentFactory
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.test.Test
import kotlin.test.assertEquals

object MockitoHelper {
    fun <T> anyObject(): T {
        Mockito.any<T>()
        return uninitialized()
    }
    @Suppress("UNCHECKED_CAST")
    fun <T> uninitialized(): T = null as T
}

class MyMarkToolWindowContentTest {

    val apiServiceMock = Mockito.mock(ApiService::class.java)

    private var myFixture: IdeaProjectTestFixture? = null
    private var codeInsightFixture:CodeInsightTestFixture? = null


    @BeforeEach
    fun setUp() {
        myFixture = IdeaTestFixtureFactory.getFixtureFactory().createLightFixtureBuilder("test").fixture
        myFixture!!.setUp();
    }
    @After
    fun tearDown() {
        myFixture!!.tearDown()
    }

    fun setupProject(): MyMarkToolWindowFactory.MyMarkToolWindowContent {
        val project: Project = myFixture!!.project
        val fac = MyMarkToolWindowFactory()
        val x = MockToolWindow(project)
        fac.createToolWindowContent(project, x)

        ToolWindowManager.getInstance(project).registerToolWindow(RegisterToolWindowTask(id="MyMark", contentFactory = MyMarkToolWindowFactory()))
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow: ToolWindow? = toolWindowManager.getToolWindow("MyMark")
        val content = ContentFactory.getInstance().createContent(MyMarkToolWindowFactory.MyMarkToolWindowContent(toolWindow!!,project), "", false)
        toolWindow.contentManager.addContent(content)
        val toolWindowContent = toolWindow.contentManager.contents[0].component as MyMarkToolWindowFactory.MyMarkToolWindowContent
        toolWindowContent.apiService = apiServiceMock
        return toolWindowContent
    }

    @Test
    fun updateExercisesSuccessUpdatesAttribute() {

       val toolWindowContent = setupProject()

        val mockExercisesCall: Call<Exercises> = Mockito.mock(Call::class.java) as Call<Exercises>
        Mockito.`when`(apiServiceMock.fetchExercises(Mockito.anyString())).thenReturn(mockExercisesCall)
        Mockito.`when`(mockExercisesCall.enqueue(Mockito.any())).thenAnswer {
            val callback = it.arguments[0] as Callback<Exercises>
            callback.onResponse(mockExercisesCall, Response.success(Exercises(arrayOf("Exercise 1", "Exercise 2"))))
        }

        toolWindowContent.updateExercises()

        Assert.assertArrayEquals(toolWindowContent.exercises,arrayOf("Exercise 1", "Exercise 2"))
    }

    @Test
    fun updateExercisesFailureBecomesConnectionError() {
        val toolWindowContent = setupProject()

        val mockExercisesCall: Call<Exercises> = Mockito.mock(Call::class.java) as Call<Exercises>
        Mockito.`when`(apiServiceMock.fetchExercises(Mockito.anyString())).thenReturn(mockExercisesCall)
        Mockito.`when`(mockExercisesCall.enqueue(Mockito.any())).thenAnswer {
            val callback = it.arguments[0] as Callback<Exercises>
            callback.onResponse(mockExercisesCall, Response.error(500, ResponseBody.create(null, "Error")))
        }

        toolWindowContent.updateExercises()

        Assert.assertArrayEquals(toolWindowContent.exercises, arrayOf("Connection Error"))

    }

    @Test
    fun updateExercisesDoesntFetchIfEmptyModules() {
        val toolWindowContent = setupProject()

        val mockExercisesCall: Call<Exercises> = Mockito.mock(Call::class.java) as Call<Exercises>
        Mockito.`when`(apiServiceMock.fetchExercises(Mockito.anyString())).thenReturn(mockExercisesCall)
        Mockito.`when`(mockExercisesCall.enqueue(Mockito.any())).thenAnswer {
            val callback = it.arguments[0] as Callback<Exercises>
            callback.onResponse(mockExercisesCall, Response.error(500, ResponseBody.create(null, "Error")))
        }
        toolWindowContent.modules = arrayOf()
        toolWindowContent.updateExercises()
        Assert.assertArrayEquals(toolWindowContent.exercises, arrayOf())
    }
    @Test
    fun updateExercisesDoesntFetchIfModuleIsConnectionError() {
        val toolWindowContent = setupProject()

        val mockExercisesCall: Call<Exercises> = Mockito.mock(Call::class.java) as Call<Exercises>
        Mockito.`when`(apiServiceMock.fetchExercises(Mockito.anyString())).thenReturn(mockExercisesCall)
        Mockito.`when`(mockExercisesCall.enqueue(Mockito.any())).thenAnswer {
            val callback = it.arguments[0] as Callback<Exercises>
            callback.onResponse(mockExercisesCall, Response.error(500, ResponseBody.create(null, "Error")))
        }
        toolWindowContent.modules = arrayOf("Connection Error")
        toolWindowContent.updateExercises()
        Assert.assertArrayEquals(toolWindowContent.exercises, arrayOf("Connection Error"))

    }

    @Test
    fun updateModulesSuccessUpdatesAttribute() {
        val toolWindowContent = setupProject()

        val mockModulesCall: Call<Modules> = Mockito.mock(Call::class.java) as Call<Modules>
        Mockito.`when`(apiServiceMock.fetchModules()).thenReturn(mockModulesCall)
        Mockito.`when`(mockModulesCall.enqueue(Mockito.any())).thenAnswer {
            val callback = it.arguments[0] as Callback<Modules>
            callback.onResponse(mockModulesCall, Response.success(Modules(arrayOf("Module 1", "Module 2"))))
        }
        val mockExercisesCall: Call<Exercises> = Mockito.mock(Call::class.java) as Call<Exercises>
        Mockito.`when`(apiServiceMock.fetchExercises(Mockito.anyString())).thenReturn(mockExercisesCall)
        Mockito.`when`(mockExercisesCall.enqueue(Mockito.any())).thenAnswer {
            val callback = it.arguments[0] as Callback<Exercises>
            callback.onResponse(mockExercisesCall, Response.success(Exercises(arrayOf("Exercise 1", "Exercise 2"))))
        }

        toolWindowContent.exercises = arrayOf()
        toolWindowContent.modules = arrayOf()
        toolWindowContent.updateModules()
        Assert.assertArrayEquals(toolWindowContent.modules,arrayOf("Module 1", "Module 2"))
        Assert.assertArrayEquals(toolWindowContent.exercises, arrayOf("Exercise 1", "Exercise 2"))
    }

    @Test
    fun updateModulesFailureBecomesConnectionError() {
        val toolWindowContent = setupProject()

        val mockModulesCall: Call<Modules> = Mockito.mock(Call::class.java) as Call<Modules>
        Mockito.`when`(apiServiceMock.fetchModules()).thenReturn(mockModulesCall)
        Mockito.`when`(mockModulesCall.enqueue(Mockito.any())).thenAnswer {
            val callback = it.arguments[0] as Callback<Modules>
            callback.onResponse(mockModulesCall, Response.error(500, ResponseBody.create(null, "Error")))
        }

        val mockExercisesCall: Call<Exercises> = Mockito.mock(Call::class.java) as Call<Exercises>
        Mockito.`when`(apiServiceMock.fetchExercises(Mockito.anyString())).thenReturn(mockExercisesCall)
        Mockito.`when`(mockExercisesCall.enqueue(Mockito.any())).thenAnswer {
            val callback = it.arguments[0] as Callback<Exercises>
            callback.onResponse(mockExercisesCall, Response.error(500, ResponseBody.create(null, "Error")))
        }

        toolWindowContent.updateModules()

        Assert.assertArrayEquals(toolWindowContent.modules, arrayOf("Connection Error"))
        Assert.assertArrayEquals(toolWindowContent.exercises, arrayOf("Connection Error"))

    }

    @Test
    fun sendMessageDisabledIfModuleSelectedIsNull() {
        val toolWindowContent = setupProject()

        val mockModulesCall: Call<Modules> = Mockito.mock(Call::class.java) as Call<Modules>
        Mockito.`when`(apiServiceMock.fetchModules()).thenReturn(mockModulesCall)
        Mockito.`when`(mockModulesCall.enqueue(Mockito.any())).thenAnswer {
            val callback = it.arguments[0] as Callback<Modules>
            callback.onResponse(mockModulesCall, Response.success(Modules(arrayOf())))
        }
        val mockExercisesCall: Call<Exercises> = Mockito.mock(Call::class.java) as Call<Exercises>
        Mockito.`when`(apiServiceMock.fetchExercises(Mockito.anyString())).thenReturn(mockExercisesCall)
        Mockito.`when`(mockExercisesCall.enqueue(Mockito.any())).thenAnswer {
            val callback = it.arguments[0] as Callback<Exercises>
            callback.onResponse(mockExercisesCall, Response.success(Exercises(arrayOf("Exists"))))
        }

        toolWindowContent.updateModules()
        assertEquals(toolWindowContent.sendButton.isEnabled, false)
    }

    @Test
    fun sendMessageDisabledIfExercisesSelectedIsNull() {

        val toolWindowContent = setupProject()

        val mockModulesCall: Call<Modules> = Mockito.mock(Call::class.java) as Call<Modules>
        Mockito.`when`(apiServiceMock.fetchModules()).thenReturn(mockModulesCall)
        Mockito.`when`(mockModulesCall.enqueue(Mockito.any())).thenAnswer {
            val callback = it.arguments[0] as Callback<Modules>
            callback.onResponse(mockModulesCall, Response.success(Modules(arrayOf("Exists"))))
        }
        val mockExercisesCall: Call<Exercises> = Mockito.mock(Call::class.java) as Call<Exercises>
        Mockito.`when`(apiServiceMock.fetchExercises(Mockito.anyString())).thenReturn(mockExercisesCall)
        Mockito.`when`(mockExercisesCall.enqueue(Mockito.any())).thenAnswer {
            val callback = it.arguments[0] as Callback<Exercises>
            callback.onResponse(mockExercisesCall, Response.success(Exercises(arrayOf())))
        }

        toolWindowContent.updateModules()
        assertEquals(toolWindowContent.sendButton.isEnabled, false)

    }

    //TODO also make it disable if connection error is either of them

    @Test
    fun sendMessageDisabledIfModuleSelectedIsError() {
        val toolWindowContent = setupProject()

        val mockModulesCall: Call<Modules> = Mockito.mock(Call::class.java) as Call<Modules>
        Mockito.`when`(apiServiceMock.fetchModules()).thenReturn(mockModulesCall)
        Mockito.`when`(mockModulesCall.enqueue(Mockito.any())).thenAnswer {
            val callback = it.arguments[0] as Callback<Modules>
            callback.onResponse(mockModulesCall, Response.error(500, ResponseBody.create(null, "Error")))
        }
        val mockExercisesCall: Call<Exercises> = Mockito.mock(Call::class.java) as Call<Exercises>
        Mockito.`when`(apiServiceMock.fetchExercises(Mockito.anyString())).thenReturn(mockExercisesCall)
        Mockito.`when`(mockExercisesCall.enqueue(Mockito.any())).thenAnswer {
            val callback = it.arguments[0] as Callback<Exercises>
            callback.onResponse(mockExercisesCall, Response.success(Exercises(arrayOf("Exists"))))
        }

        toolWindowContent.updateModules()
        assertEquals(toolWindowContent.sendButton.isEnabled, false)
    }

    @Test
    fun sendMessageDisabledIfExercisesSelectedIsError() {

        val toolWindowContent = setupProject()

        val mockModulesCall: Call<Modules> = Mockito.mock(Call::class.java) as Call<Modules>
        Mockito.`when`(apiServiceMock.fetchModules()).thenReturn(mockModulesCall)
        Mockito.`when`(mockModulesCall.enqueue(Mockito.any())).thenAnswer {
            val callback = it.arguments[0] as Callback<Modules>
            callback.onResponse(mockModulesCall, Response.success(Modules(arrayOf("Exists"))))
        }
        val mockExercisesCall: Call<Exercises> = Mockito.mock(Call::class.java) as Call<Exercises>
        Mockito.`when`(apiServiceMock.fetchExercises(Mockito.anyString())).thenReturn(mockExercisesCall)
        Mockito.`when`(mockExercisesCall.enqueue(Mockito.any())).thenAnswer {
            val callback = it.arguments[0] as Callback<Exercises>
            callback.onResponse(mockExercisesCall, Response.error(500, ResponseBody.create(null, "Error")))
        }

        toolWindowContent.updateModules()
        assertEquals(toolWindowContent.sendButton.isEnabled, false)

    }

    @Test
    fun sendMessageClearsMessageBox() {
        val toolWindowContent = setupProject()

        val mockModulesCall: Call<Modules> = Mockito.mock(Call::class.java) as Call<Modules>
        Mockito.`when`(apiServiceMock.fetchModules()).thenReturn(mockModulesCall)
        Mockito.`when`(mockModulesCall.enqueue(Mockito.any())).thenAnswer {
            val callback = it.arguments[0] as Callback<Modules>
            callback.onResponse(mockModulesCall, Response.success(Modules(arrayOf("Module 1", "Module 2"))))
        }
        val mockExercisesCall: Call<Exercises> = Mockito.mock(Call::class.java) as Call<Exercises>
        Mockito.`when`(apiServiceMock.fetchExercises(Mockito.anyString())).thenReturn(mockExercisesCall)
        Mockito.`when`(mockExercisesCall.enqueue(Mockito.any())).thenAnswer {
            val callback = it.arguments[0] as Callback<Exercises>
            callback.onResponse(mockExercisesCall, Response.success(Exercises(arrayOf("Exercise 1", "Exercise 2"))))
        }

        toolWindowContent.updateModules()
        toolWindowContent.messageTextField.text = "Hotel?"

        val mockQuestionCall: Call<Answer> = Mockito.mock(Call::class.java) as Call<Answer>
        Mockito.`when`(apiServiceMock.askQuestion(MockitoHelper.anyObject(),Mockito.anyString(), Mockito.anyString())).thenReturn(mockQuestionCall)
        Mockito.`when`(mockQuestionCall.enqueue(Mockito.any())).thenAnswer {
            val callback = it.arguments[0] as Callback<Answer>
            callback.onResponse(mockQuestionCall, Response.success(Answer("42")))
        }
        toolWindowContent.sendMessage()

        assertEquals(toolWindowContent.messageTextField.text ,"")

    }

    @Test
    fun successfulMessageAddedToMessages() {
        val toolWindowContent = setupProject()

        val mockModulesCall: Call<Modules> = Mockito.mock(Call::class.java) as Call<Modules>
        Mockito.`when`(apiServiceMock.fetchModules()).thenReturn(mockModulesCall)
        Mockito.`when`(mockModulesCall.enqueue(Mockito.any())).thenAnswer {
            val callback = it.arguments[0] as Callback<Modules>
            callback.onResponse(mockModulesCall, Response.success(Modules(arrayOf("Module 1", "Module 2"))))
        }
        val mockExercisesCall: Call<Exercises> = Mockito.mock(Call::class.java) as Call<Exercises>
        Mockito.`when`(apiServiceMock.fetchExercises(Mockito.anyString())).thenReturn(mockExercisesCall)
        Mockito.`when`(mockExercisesCall.enqueue(Mockito.any())).thenAnswer {
            val callback = it.arguments[0] as Callback<Exercises>
            callback.onResponse(mockExercisesCall, Response.success(Exercises(arrayOf("Exercise 1", "Exercise 2"))))
        }

        toolWindowContent.updateModules()
        toolWindowContent.messageTextField.text = "Hotel?"

        val mockQuestionCall: Call<Answer> = Mockito.mock(Call::class.java) as Call<Answer>
        Mockito.`when`(apiServiceMock.askQuestion(MockitoHelper.anyObject(),Mockito.anyString(), Mockito.anyString())).thenReturn(mockQuestionCall)
        Mockito.`when`(mockQuestionCall.enqueue(Mockito.any())).thenAnswer {
            val callback = it.arguments[0] as Callback<Answer>
            callback.onResponse(mockQuestionCall, Response.success(Answer("42")))
        }
        toolWindowContent.sendMessage()

        assert(toolWindowContent.chatTextArea.text.contains("Hotel?"))
        assert(toolWindowContent.chatTextArea.text.contains("42"))
        assertEquals(toolWindowContent.messages.get(toolWindowContent.messages.size - 2).content,"Hotel?")
        assertEquals(toolWindowContent.messages.get(toolWindowContent.messages.size - 1).content,"42")
        assertEquals(toolWindowContent.messages.get(toolWindowContent.messages.size - 2).role,"user")
        assertEquals(toolWindowContent.messages.get(toolWindowContent.messages.size - 1).role,"assistant")
    }

    @Test
    fun failedMessageRemovedFromMessages() {
        val toolWindowContent = setupProject()

        val mockModulesCall: Call<Modules> = Mockito.mock(Call::class.java) as Call<Modules>
        Mockito.`when`(apiServiceMock.fetchModules()).thenReturn(mockModulesCall)
        Mockito.`when`(mockModulesCall.enqueue(Mockito.any())).thenAnswer {
            val callback = it.arguments[0] as Callback<Modules>
            callback.onResponse(mockModulesCall, Response.success(Modules(arrayOf("Module 1", "Module 2"))))
        }
        val mockExercisesCall: Call<Exercises> = Mockito.mock(Call::class.java) as Call<Exercises>
        Mockito.`when`(apiServiceMock.fetchExercises(Mockito.anyString())).thenReturn(mockExercisesCall)
        Mockito.`when`(mockExercisesCall.enqueue(Mockito.any())).thenAnswer {
            val callback = it.arguments[0] as Callback<Exercises>
            callback.onResponse(mockExercisesCall, Response.success(Exercises(arrayOf("Exercise 1", "Exercise 2"))))
        }

        toolWindowContent.updateModules()
        toolWindowContent.messageTextField.text = "Hotel?"

        val mockQuestionCall: Call<Answer> = Mockito.mock(Call::class.java) as Call<Answer>
        Mockito.`when`(apiServiceMock.askQuestion(MockitoHelper.anyObject(),Mockito.anyString(), Mockito.anyString())).thenReturn(mockQuestionCall)
        Mockito.`when`(mockQuestionCall.enqueue(Mockito.any())).thenAnswer {
            val callback = it.arguments[0] as Callback<Answer>
            callback.onResponse(mockQuestionCall, Response.error(500, ResponseBody.create(null, "Error")))
        }
        toolWindowContent.sendMessage()

        assert(toolWindowContent.chatTextArea.text.contains("Hotel?"))
        assert(!toolWindowContent.chatTextArea.text.contains("42"))
        assertEquals(toolWindowContent.messages.size,0)
    }

}