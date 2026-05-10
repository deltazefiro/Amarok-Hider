package deltazero.amarok.utils;

import deltazero.amarok.AmarokApplication;
import deltazero.amarok.core.Hider;
import deltazero.amarok.core.SettingsSnapshot;

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

  public static boolean isDisguiseNeeded(AmarokApplication app) {
    SettingsSnapshot settings = app.getSettingsRepo().getSettings().getValue();
    if (settings.getDisableSecurityWhenUnhidden() && Hider.getState() == Hider.State.VISIBLE)
      return false;
    return settings.getDisguise() && disguised;
  }

  public static boolean isUnlockRequired(AmarokApplication app) {
    SettingsSnapshot settings = app.getSettingsRepo().getSettings().getValue();
    if (settings.getDisableSecurityWhenUnhidden() && Hider.getState() == Hider.State.VISIBLE)
      return false;
    return settings.getPassword() != null && locked;
  }
}
