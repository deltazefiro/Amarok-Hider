package deltazero.amarok.filehider;

import android.content.Context;
import java.util.Set;

public abstract class BaseFileHider {

  protected final Context context;

  public BaseFileHider(Context context) {
    this.context = context;
  }

  protected abstract void process(Set<String> targetDirs, ProcessMethod method)
      throws InterruptedException;

  public abstract void tryToActive(ActivationCallbackListener activationCallbackListener);

  public abstract String getName();

  protected enum ProcessMethod {
    HIDE,
    UNHIDE
  }

  public void hide(Set<String> targetDirs) throws InterruptedException {
    process(targetDirs, ProcessMethod.HIDE);
  }

  public void unhide(Set<String> targetDirs) throws InterruptedException {
    process(targetDirs, ProcessMethod.UNHIDE);
  }

  public static BaseFileHider fromMode(Context context, int mode) {
    return switch (mode) {
      case 0 -> new NoneFileHider(context);
      case 1 -> new ObfuscateFileHider(context);
      case 2 -> new NoMediaFileHider(context);
      case 3 -> new ChmodFileHider(context);
      default -> throw new IndexOutOfBoundsException("Unknown file hider mode: " + mode);
    };
  }

  public static int modeOf(Class<? extends BaseFileHider> cls) {
    if (cls == NoneFileHider.class) return 0;
    if (cls == ObfuscateFileHider.class) return 1;
    if (cls == NoMediaFileHider.class) return 2;
    if (cls == ChmodFileHider.class) return 3;
    throw new IndexOutOfBoundsException("Unknown file hider class: " + cls.getName());
  }

  public interface ActivationCallbackListener {
    void onActivateCallback(Class<? extends BaseFileHider> appHider, boolean success, int msgResID);
  }
}
