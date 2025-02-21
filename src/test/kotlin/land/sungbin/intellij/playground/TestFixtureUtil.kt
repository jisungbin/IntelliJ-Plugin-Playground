package land.sungbin.intellij.playground

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.testFramework.junit5.fixture.TestFixture
import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.isAccessible

val defaultOpenProjectTask = OpenProjectTask { projectName = "test-project" }
const val defaultModuleName = "test-module"

@Suppress("unused")
operator fun <T : Any> TestFixture<T>.getValue(thisRef: Any?, property: Any?): T = get()

@Suppress("UNCHECKED_CAST")
fun <T : Any> KProperty0<T>.asFixture(): TestFixture<T> =
  apply { isAccessible = true }.getDelegate() as TestFixture<T>
