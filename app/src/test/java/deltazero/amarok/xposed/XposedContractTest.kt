package deltazero.amarok.xposed

import deltazero.amarok.xposed.hooks.IHook
import deltazero.amarok.xposed.utils.ParceledListSliceUtil
import deltazero.amarok.xposed.utils.XPref
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class XposedContractTest {
  @Test
  fun xPrefConstantsAndDefaultCacheStateAreStable() {
    assertEquals("deltazero.amarok.xposed.prefs", XPref.XPREF_PATH)
    assertEquals("hidePkgNames", XPref.HIDE_PKG_NAMES)
    assertEquals("isActive", XPref.IS_ACTIVE)

    assertFalse(XPref.isXHideActive())
    assertFalse(XPref.shouldHide("deltazero.amarok"))
  }

  @Test
  fun iHookInterfaceKeepsMinimalHookContract() {
    val getName = IHook::class.java.getDeclaredMethod("getName")
    val load = IHook::class.java.getDeclaredMethod("load")

    assertEquals(String::class.java, getName.returnType)
    assertEquals(0, getName.parameterCount)
    assertEquals(Void.TYPE, load.returnType)
    assertEquals(0, load.parameterCount)
  }

  @Test
  fun parceledListSliceUtilReadsListUsingGetListReflection() {
    val packages = listOf("pkg.one", "pkg.two")
    val slice = FakeParceledListSlice(packages)

    val result = ParceledListSliceUtil.sliceToList<String>(slice)

    assertSame(packages, result)
  }

  private class FakeParceledListSlice(private val list: List<String>) {
    fun getList(): List<String> = list
  }
}
