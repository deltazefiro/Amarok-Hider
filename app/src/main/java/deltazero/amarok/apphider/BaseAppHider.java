package deltazero.amarok.apphider;

import android.content.Context;
import java.util.Set;

public abstract class BaseAppHider {
  public Context context;

  public BaseAppHider(Context context) {
    this.context = context;
  }

  /**
   * Hide apps with option to only disable them (skip the hide step)
   *
   * @param pkgNames Package names to hide
   * @param disableOnly If true, only disable apps without hiding them from system
   */
  public abstract void hide(Set<String> pkgNames, boolean disableOnly);

  public abstract void unhide(Set<String> pkgNames);

  public abstract void tryToActivate(ActivationCallbackListener activationCallbackListener);

  public abstract String getName();

  public static BaseAppHider fromMode(Context context, int mode) {
    return switch (mode) {
      case 0 -> new NoneAppHider(context);
      case 1 -> new RootAppHider(context);
      case 2 -> new DsmAppHider(context);
      case 3 -> new ShizukuAppHider(context);
      case 4 -> new DhizukuAppHider(context);
      default -> throw new IndexOutOfBoundsException("Unknown app hider mode: " + mode);
    };
  }

  public static int modeOf(Class<? extends BaseAppHider> cls) {
    if (cls == NoneAppHider.class) return 0;
    if (cls == RootAppHider.class) return 1;
    if (cls == DsmAppHider.class) return 2;
    if (cls == ShizukuAppHider.class) return 3;
    if (cls == DhizukuAppHider.class) return 4;
    throw new IndexOutOfBoundsException("Unknown app hider class: " + cls.getName());
  }

  public interface ActivationCallbackListener {
    void onActivateCallback(Class<? extends BaseAppHider> appHider, boolean success, int msgResID);
  }
}
