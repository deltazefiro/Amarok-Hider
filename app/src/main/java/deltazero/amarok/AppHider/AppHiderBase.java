package deltazero.amarok.AppHider;

import java.util.Set;

public interface AppHiderBase {
    void hide(Set<String> pkgNames);
    void unhide(Set<String> pkgNames);
    boolean checkAvailability();
}