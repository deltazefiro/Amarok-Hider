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
  private static MutableLiveData<Integer> _appHiderMode;
  public static LiveData<Integer> appHiderMode;
  private static MutableLiveData<Integer> _fileHiderMode;
  public static LiveData<Integer> fileHiderMode;

  /** Error string resource ID from the last failed tryToActivate, 0 means no error. */
  private static MutableLiveData<Integer> _appHiderError;

  public static LiveData<Integer> appHiderError;

  /** Error string resource ID from the last failed tryToActive, 0 means no error. */
  private static MutableLiveData<Integer> _fileHiderError;

  public static LiveData<Integer> fileHiderError;

  public enum State {
    HIDDEN,
    VISIBLE,
    PROCESSING
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
    _appHiderMode = new MutableLiveData<>(PrefMgr.getAppHiderMode());
    appHiderMode = _appHiderMode;
    _fileHiderMode = new MutableLiveData<>(PrefMgr.getFileHiderMode());
    fileHiderMode = _fileHiderMode;
    _appHiderError = new MutableLiveData<>(0);
    appHiderError = _appHiderError;
    _fileHiderError = new MutableLiveData<>(0);
    fileHiderError = _fileHiderError;

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
    BaseAppHider.fromMode(context, getAppHiderMode())
        .tryToActivate(
            (appHiderClass, succeed, msg) -> {
              if (succeed) {
                _appHiderError.postValue(0);
                processHide(context);
              } else {
                _appHiderError.postValue(msg);
                showErrorToast(context, msg);
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
              BaseAppHider.fromMode(context, getAppHiderMode()).hide(appsToHide, disableOnly);
            }

            Set<String> managedFolders = PrefMgr.getHideFilePath();
            Set<String> alreadyHiddenFolders =
                _hiddenFolders.getValue() != null ? _hiddenFolders.getValue() : new HashSet<>();
            Set<String> foldersToHide = new HashSet<>(managedFolders);
            foldersToHide.removeAll(alreadyHiddenFolders);
            if (!foldersToHide.isEmpty()) {
              BaseFileHider.fromMode(context, getFileHiderMode()).hide(foldersToHide);
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
    BaseAppHider.fromMode(context, getAppHiderMode())
        .tryToActivate(
            (appHiderClass, succeed, msg) -> {
              if (succeed) {
                _appHiderError.postValue(0);
                processUnhide(context);
              } else {
                _appHiderError.postValue(msg);
                showErrorToast(context, msg);
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
              BaseAppHider.fromMode(context, getAppHiderMode()).unhide(currentlyHiddenApps);
            }

            Set<String> currentlyHiddenFolders =
                _hiddenFolders.getValue() != null ? _hiddenFolders.getValue() : new HashSet<>();
            if (!currentlyHiddenFolders.isEmpty()) {
              BaseFileHider.fromMode(context, getFileHiderMode()).unhide(currentlyHiddenFolders);
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
    Integer err = _appHiderError.getValue();
    if (err != null && err != 0) {
      Log.w(TAG, "hideApp skipped: app hider in error state");
      showErrorToast(context, err);
      return;
    }
    Set<String> current = new HashSet<>(_hiddenApps.getValue());
    current.add(pkgName);
    _hiddenApps.postValue(current);
    PrefMgr.setHiddenApps(current);

    threadHandler.post(
        () -> {
          boolean disableOnly = PrefMgr.isXHideEnabled() && PrefMgr.getDisableOnlyWithXHide();
          BaseAppHider.fromMode(context, getAppHiderMode()).hide(Set.of(pkgName), disableOnly);
          recomputeState();
        });
  }

  public static void unhideApp(Context context, String pkgName) {
    Integer err = _appHiderError.getValue();
    if (err != null && err != 0) {
      Log.w(TAG, "unhideApp skipped: app hider in error state");
      showErrorToast(context, err);
      return;
    }
    Set<String> current = new HashSet<>(_hiddenApps.getValue());
    current.remove(pkgName);
    _hiddenApps.postValue(current);
    PrefMgr.setHiddenApps(current);

    threadHandler.post(
        () -> {
          BaseAppHider.fromMode(context, getAppHiderMode()).unhide(Set.of(pkgName));
          recomputeState();
        });
  }

  public static void hideFolder(Context context, String path) {
    Integer err = _fileHiderError.getValue();
    if (err != null && err != 0) {
      Log.w(TAG, "hideFolder skipped: file hider in error state");
      showErrorToast(context, err);
      return;
    }
    Set<String> current = new HashSet<>(_hiddenFolders.getValue());
    current.add(path);
    _hiddenFolders.postValue(current);
    PrefMgr.setHiddenFolders(current);

    threadHandler.post(
        () -> {
          try {
            BaseFileHider.fromMode(context, getFileHiderMode()).hide(Set.of(path));
          } catch (InterruptedException e) {
            Log.w(TAG, "hideFolder interrupted");
          }
          recomputeState();
        });
  }

  public static void unhideFolder(Context context, String path) {
    Integer err = _fileHiderError.getValue();
    if (err != null && err != 0) {
      Log.w(TAG, "unhideFolder skipped: file hider in error state");
      showErrorToast(context, err);
      return;
    }
    Set<String> current = new HashSet<>(_hiddenFolders.getValue());
    current.remove(path);
    _hiddenFolders.postValue(current);
    PrefMgr.setHiddenFolders(current);

    threadHandler.post(
        () -> {
          try {
            BaseFileHider.fromMode(context, getFileHiderMode()).unhide(Set.of(path));
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

  public static int getAppHiderMode() {
    return _appHiderMode.getValue();
  }

  public static void setAppHiderMode(int mode) {
    PrefMgr.setAppHiderMode(mode);
    _appHiderMode.postValue(mode);
  }

  public static void setAppHiderError(int errorResId) {
    _appHiderError.postValue(errorResId);
  }

  public static int getFileHiderMode() {
    return _fileHiderMode.getValue();
  }

  public static void setFileHiderMode(int mode) {
    PrefMgr.setFileHiderMode(mode);
    _fileHiderMode.postValue(mode);
  }

  public static void setFileHiderError(int errorResId) {
    _fileHiderError.postValue(errorResId);
  }

  private static void showErrorToast(Context context, int msgResId) {
    new Handler(Looper.getMainLooper())
        .post(() -> Toast.makeText(context, msgResId, Toast.LENGTH_LONG).show());
  }
}
