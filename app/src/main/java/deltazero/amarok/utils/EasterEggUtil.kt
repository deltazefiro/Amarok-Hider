package deltazero.amarok.utils

import android.icu.util.Calendar
import java.util.concurrent.TimeUnit
import nl.dionsegijn.konfetti.core.Angle
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.PartyFactory
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.Spread
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Shape

object EasterEggUtil {
  @JvmField
  var rainParty: Party =
    PartyFactory(Emitter(5, TimeUnit.SECONDS).perSecond(100))
      .angle(Angle.BOTTOM)
      .spread(Spread.ROUND)
      .shapes(listOf(Shape.Square, Shape.Circle))
      .colors(listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def))
      .setSpeedBetween(0f, 15f)
      .position(Position.Relative(0.0, 0.0).between(Position.Relative(1.0, 0.0)))
      .build()

  @JvmField
  var explodeParty: Party =
    PartyFactory(Emitter(100L, TimeUnit.MILLISECONDS).max(100))
      .spread(360)
      .shapes(listOf(Shape.Square, Shape.Circle))
      .colors(listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def))
      .setSpeedBetween(0f, 30f)
      .position(Position.Relative(0.5, 0.3))
      .build()

  @JvmStatic
  fun is2024NewYear(): Boolean {
    val today = Calendar.getInstance()
    val startDate = Calendar.getInstance()
    startDate.set(2024, Calendar.JANUARY, 1, 0, 0, 0)
    val endDate = Calendar.getInstance()
    endDate.set(2024, Calendar.JANUARY, 3, 23, 59, 59)
    return today.after(startDate) && today.before(endDate)
  }
}
