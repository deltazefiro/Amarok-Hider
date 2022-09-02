package deltazero.amarok.AppHider;

import java.util.Set;

public class NoneAppHider implements AppHiderBase {

    @Override
    public void hide(Set<String> pkgNames) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unhide(Set<String> pkgNames) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean checkAvailability() {
        return false;
    }
}
