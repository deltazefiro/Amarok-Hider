package deltazero.amarok.utils;

import android.icu.util.Calendar;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import nl.dionsegijn.konfetti.core.Angle;
import nl.dionsegijn.konfetti.core.Party;
import nl.dionsegijn.konfetti.core.PartyFactory;
import nl.dionsegijn.konfetti.core.Position;
import nl.dionsegijn.konfetti.core.Spread;
import nl.dionsegijn.konfetti.core.emitter.Emitter;
import nl.dionsegijn.konfetti.core.models.Shape;

public class EasterEggUtil {

    public static Party rainParty = new PartyFactory(new Emitter(5, TimeUnit.SECONDS).perSecond(100))
            .angle(Angle.BOTTOM)
            .spread(Spread.ROUND)
            .shapes(Arrays.asList(Shape.Square.INSTANCE, Shape.Circle.INSTANCE))
            .colors(Arrays.asList(0xfce18a, 0xff726d, 0xf4306d, 0xb48def))
            .setSpeedBetween(0f, 15f)
            .position(new Position.Relative(0.0, 0.0).between(new Position.Relative(1.0, 0.0)))
            .build();

    public static Party explodeParty = new PartyFactory(new Emitter(100L, TimeUnit.MILLISECONDS).max(100))
            .spread(360)
            .shapes(Arrays.asList(Shape.Square.INSTANCE, Shape.Circle.INSTANCE))
            .colors(Arrays.asList(0xfce18a, 0xff726d, 0xf4306d, 0xb48def))
            .setSpeedBetween(0f, 30f)
            .position(new Position.Relative(0.5, 0.3))
            .build();

    public static boolean is2024NewYear() {
        Calendar today = Calendar.getInstance();
        Calendar startDate = Calendar.getInstance();
        startDate.set(2024, Calendar.JANUARY, 1, 0, 0, 0);
        Calendar endDate = Calendar.getInstance();
        endDate.set(2024, Calendar.JANUARY, 3, 23, 59, 59);
        return today.after(startDate) && today.before(endDate);
    }
}
