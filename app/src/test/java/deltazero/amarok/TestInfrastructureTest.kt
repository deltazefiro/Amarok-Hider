package deltazero.amarok

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class TestInfrastructureTest {
  @Test
  fun robolectricCanLoadAppContextAndResources() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    assertEquals("Amarok", context.getString(R.string.app_name))
  }
}
