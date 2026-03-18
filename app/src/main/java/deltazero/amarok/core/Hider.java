package deltazero.amarok.core;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import deltazero.amarok.QuickHideService;
import deltazero.amarok.R;
import deltazero.amarok.apphider.BaseAppHider;
import deltazero.amarok.filehider.BaseFileHider;
import deltazero.amarok.utils.SecurityUtil;
import java.util.HashSet;
import java.util.Set;

public final class Hider {

  private static final String TAG = "Hider";
  private static final HandlerThread hiderThread = new HandlerThread("HIDER_THREAD");
  private static final Handler threadHandler;

  public static boolean initialized = false;
  private static MutableLiveData<State> _state;
  public static LiveData<State> state;
  private static MutableLiveData<Set<String>> _hiddenApps;
  public static LiveData<Set<String>> hiddenApps;
  private static MutableLiveData<Set<String>> _hiddenFolders;
  public static LiveData<Set<String>> hiddenFolders;

  public enum State {
    HIDDEN,
    VISIBLE,
    PROCESSING
  }

  public interface OnActivationFailedListener {
    void onActivationFailed(int msgResID);
  }

  static {
    hiderThread.start();
    threadHandler = new Handler(hiderThread.getLooper());
  }

  /**
   * This method should be invoked in {@link deltazero.amarok.AmarokApplication#onCreate()}, after
   * {@link PrefMgr#init(Context)}. Do not invoke this method in static part or before {@link
   * PrefMgr#init(Context)}.
   */
  public static void init() {
    assert PrefMgr.initialized;

    _hiddenApps = new MutableLiveData<>(PrefMgr.getHiddenApps());
    hiddenApps = _hiddenApps;
    _hiddenFolders = new MutableLiveData<>(PrefMgr.getHiddenFolders());
    hiddenFolders = _hiddenFolders;

    if (PrefMgr.getIsHidden()) {
      if (PrefMgr.getHiddenApps().isEmpty() && !PrefMgr.getHideApps().isEmpty()) {
        Set<String> managed = PrefMgr.getHideApps();
        PrefMgr.setHiddenApps(managed);
        _hiddenApps.setValue(managed);
      }
      if (PrefMgr.getHiddenFolders().isEmpty() && !PrefMgr.getHideFilePath().isEmpty()) {
        Set<String> managed = PrefMgr.getHideFilePath();
        PrefMgr.setHiddenFolders(managed);
        _hiddenFolders.setValue(managed);
      }
    }

    _state = new MutableLiveData<>(computeState());
    state = _state;

    _state.observeForever(
        s -> {
          if (s != State.PROCESSING) PrefMgr.setIsHidden(s == State.HIDDEN);
        });

    initialized = true;
  }

  /**
   * NOTE: Calling this method on a background thread does not guarantee that the latest value set
   * will be received.
   */
  public static State getState() {
    return _state.getValue();
  }

  private static State computeState() {
    Set<String> ha = _hiddenApps.getValue();
    Set<String> hf = _hiddenFolders.getValue();
    boolean anyHidden = (ha != null && !ha.isEmpty()) || (hf != null && !hf.isEmpty());
    if (!anyHidden) return State.VISIBLE;

    Set<String> managedApps = PrefMgr.getHideApps();
    Set<String> managedFolders = PrefMgr.getHideFilePath();
    boolean allAppsHidden = ha != null && ha.containsAll(managedApps);
    boolean allFoldersHidden = hf != null && hf.containsAll(managedFolders);
    if (allAppsHidden && allFoldersHidden) return State.HIDDEN;

    return State.VISIBLE;
  }

  private static void recomputeState() {
    _state.postValue(computeState());
  }

  public static void hide(Context context) {
    hide(context, null);
  }

  public static void hide(Context context, OnActivationFailedListener listener) {
    BaseAppHider.fromMode(context, PrefMgr.getAppHiderMode())
        .tryToActivate(
            (appHiderClass, succeed, msg) -> {
              if (succeed) {
                processHide(context);
                return;
              }
              if (listener != null) {
                listener.onActivationFailed(msg);
              } else {
                showNoHiderToast(context, msg);
              }
            });

    // Avoid password or disguise right after hide
    if (PrefMgr.getDisableSecurityWhenUnhidden()) {
      SecurityUtil.unlock();
      SecurityUtil.dismissDisguise();
    }
  }

  private static void processHide(Context context) {

    threadHandler.post(
        () -> {
          Log.i(TAG, "Process 'hide' start.");
          _state.postValue(State.PROCESSING);

          try {
            boolean disableOnly = PrefMgr.isXHideEnabled() && PrefMgr.getDisableOnlyWithXHide();

            Set<String> managedApps = PrefMgr.getHideApps();
            Set<String> alreadyHiddenApps =
                _hiddenApps.getValue() != null ? _hiddenApps.getValue() : new HashSet<>();
            Set<String> appsToHide = new HashSet<>(managedApps);
            appsToHide.removeAll(alreadyHiddenApps);
            if (!appsToHide.isEmpty()) {
              BaseAppHider.fromMode(context, PrefMgr.getAppHiderMode())
                  .hide(appsToHide, disableOnly);
            }

            Set<String> managedFolders = PrefMgr.getHideFilePath();
            Set<String> alreadyHiddenFolders =
                _hiddenFolders.getValue() != null ? _hiddenFolders.getValue() : new HashSet<>();
            Set<String> foldersToHide = new HashSet<>(managedFolders);
            foldersToHide.removeAll(alreadyHiddenFolders);
            if (!foldersToHide.isEmpty()) {
              BaseFileHider.fromMode(context, PrefMgr.getFileHiderMode()).hide(foldersToHide);
            }
          } catch (InterruptedException e) {
            Log.w(TAG, "Process 'hide' interrupted.");
            return;
          }

          Set<String> allManagedApps = PrefMgr.getHideApps();
          Set<String> allManagedFolders = PrefMgr.getHideFilePath();
          _hiddenApps.postValue(allManagedApps);
          _hiddenFolders.postValue(allManagedFolders);
          PrefMgr.setHiddenApps(allManagedApps);
          PrefMgr.setHiddenFolders(allManagedFolders);

          Log.i(TAG, "Process 'hide' finish.");
          _state.postValue(State.HIDDEN);

          if (!PrefMgr.getDisableToasts())
            Toast.makeText(context, R.string.hidden_toast, Toast.LENGTH_SHORT).show();

          QuickHideService.stopService(context);
        });
  }

  public static void unhide(Context context) {
    unhide(context, null);
  }

  public static void unhide(Context context, OnActivationFailedListener listener) {
    BaseAppHider.fromMode(context, PrefMgr.getAppHiderMode())
        .tryToActivate(
            (appHiderClass, succeed, msg) -> {
              if (succeed) {
                processUnhide(context);
                return;
              }
              if (listener != null) {
                listener.onActivationFailed(msg);
              } else {
                showNoHiderToast(context, msg);
              }
            });
  }

  private static void processUnhide(Context context) {

    threadHandler.post(
        () -> {
          Log.i(TAG, "Process 'unhide' start.");
          _state.postValue(State.PROCESSING);

          try {
            Set<String> currentlyHiddenApps =
                _hiddenApps.getValue() != null ? _hiddenApps.getValue() : new HashSet<>();
            if (!currentlyHiddenApps.isEmpty()) {
              BaseAppHider.fromMode(context, PrefMgr.getAppHiderMode()).unhide(currentlyHiddenApps);
            }

            Set<String> currentlyHiddenFolders =
                _hiddenFolders.getValue() != null ? _hiddenFolders.getValue() : new HashSet<>();
            if (!currentlyHiddenFolders.isEmpty()) {
              BaseFileHider.fromMode(context, PrefMgr.getFileHiderMode())
                  .unhide(currentlyHiddenFolders);
            }
          } catch (InterruptedException e) {
            Log.w(TAG, "Process 'unhide' interrupted.");
            return;
          }

          _hiddenApps.postValue(new HashSet<>());
          _hiddenFolders.postValue(new HashSet<>());
          PrefMgr.setHiddenApps(new HashSet<>());
          PrefMgr.setHiddenFolders(new HashSet<>());

          Log.i(TAG, "Process 'unhide' finish.");
          _state.postValue(State.VISIBLE);

          if (!PrefMgr.getDisableToasts())
            Toast.makeText(context, R.string.unhidden_toast, Toast.LENGTH_SHORT).show();

          new Handler(Looper.getMainLooper()).post(() -> QuickHideService.startService(context));
        });
  }

  public static void hideApp(Context context, String pkgName) {
    Set<String> current = new HashSet<>(_hiddenApps.getValue());
    current.add(pkgName);
    _hiddenApps.postValue(current);
    PrefMgr.setHiddenApps(current);

    threadHandler.post(
        () -> {
          boolean disableOnly = PrefMgr.isXHideEnabled() && PrefMgr.getDisableOnlyWithXHide();
          BaseAppHider.fromMode(context, PrefMgr.getAppHiderMode())
              .hide(Set.of(pkgName), disableOnly);
          recomputeState();
        });
  }

  public static void unhideApp(Context context, String pkgName) {
    Set<String> current = new HashSet<>(_hiddenApps.getValue());
    current.remove(pkgName);
    _hiddenApps.postValue(current);
    PrefMgr.setHiddenApps(current);

    threadHandler.post(
        () -> {
          BaseAppHider.fromMode(context, PrefMgr.getAppHiderMode()).unhide(Set.of(pkgName));
          recomputeState();
        });
  }

  public static void hideFolder(Context context, String path) {
    Set<String> current = new HashSet<>(_hiddenFolders.getValue());
    current.add(path);
    _hiddenFolders.postValue(current);
    PrefMgr.setHiddenFolders(current);

    threadHandler.post(
        () -> {
          try {
            BaseFileHider.fromMode(context, PrefMgr.getFileHiderMode()).hide(Set.of(path));
          } catch (InterruptedException e) {
            Log.w(TAG, "hideFolder interrupted");
          }
          recomputeState();
        });
  }

  public static void unhideFolder(Context context, String path) {
    Set<String> current = new HashSet<>(_hiddenFolders.getValue());
    current.remove(path);
    _hiddenFolders.postValue(current);
    PrefMgr.setHiddenFolders(current);

    threadHandler.post(
        () -> {
          try {
            BaseFileHider.fromMode(context, PrefMgr.getFileHiderMode()).unhide(Set.of(path));
          } catch (InterruptedException e) {
            Log.w(TAG, "unhideFolder interrupted");
          }
          recomputeState();
        });
  }

  public static void forceUnhide(Context context) {
    if (_state.getValue() == State.PROCESSING) hiderThread.interrupt();
    PrefMgr.setIsHidden(true);
    unhide(context);
  }

  private static void showNoHiderToast(Context context, int message) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show();
  }
}
