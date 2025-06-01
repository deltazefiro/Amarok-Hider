package deltazero.amarok.utils;

import deltazero.amarok.Hider;
import deltazero.amarok.PrefMgr;

public class SecurityUtil {
    private static boolean locked = true;
    private static boolean disguised = true;

    public static void lockAndDisguise() {
        locked = true;
        disguised = true;
    }

    public static void unlock() {
        locked = false;
    }

    public static void dismissDisguise() {
        disguised = false;
    }

    public static boolean isDisguiseNeeded() {
        if (PrefMgr.getDisableSecurityWhenUnhidden() && Hider.getState() == Hider.State.VISIBLE)
            return false;
        return PrefMgr.getEnableDisguise() && disguised;
    }

    public static boolean isUnlockRequired() {
        if (PrefMgr.getDisableSecurityWhenUnhidden() && Hider.getState() == Hider.State.VISIBLE)
            return false;
        return PrefMgr.getAmarokPassword() != null && locked;
    }
}
