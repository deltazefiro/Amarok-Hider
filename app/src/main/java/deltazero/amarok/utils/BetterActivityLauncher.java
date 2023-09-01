package deltazero.amarok.utils;

import android.content.Intent;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A wrapper class for {@link ActivityResultLauncher} that allows customising callback after receiving a result
 * from the target activity.
 * @see <a href="https://stackoverflow.com/a/63654043/19827111">...</a>
 * @author Muntashir Akon
 */
public class BetterActivityLauncher<Input, Result> {
    /**
     * Register activity result using a {@link ActivityResultContract} and an in-place activity result callback like
     * the default approach. You can still customise callback using {@link #launch(Object, OnActivityResult)}.
     */
    @NonNull
    public static <Input, Result> BetterActivityLauncher<Input, Result> registerForActivityResult(
            @NonNull ActivityResultCaller caller,
            @NonNull ActivityResultContract<Input, Result> contract,
            @Nullable OnActivityResult<Result> onActivityResult) {
        return new BetterActivityLauncher<>(caller, contract, onActivityResult);
    }

    /**
     * Same as {@link #registerForActivityResult(ActivityResultCaller, ActivityResultContract, OnActivityResult)} except
     * the last argument is set to {@code null}.
     */
    @NonNull
    public static <Input, Result> BetterActivityLauncher<Input, Result> registerForActivityResult(
            @NonNull ActivityResultCaller caller,
            @NonNull ActivityResultContract<Input, Result> contract) {
        return registerForActivityResult(caller, contract, null);
    }

    /**
     * Specialised method for launching new activities.
     */
    @NonNull
    public static BetterActivityLauncher<Intent, ActivityResult> registerActivityForResult(
            @NonNull ActivityResultCaller caller) {
        return registerForActivityResult(caller, new ActivityResultContracts.StartActivityForResult());
    }

    /**
     * Callback interface
     */
    public interface OnActivityResult<O> {
        /**
         * Called after receiving a result from the target activity
         */
        void onActivityResult(O result);
    }

    private final ActivityResultLauncher<Input> launcher;
    @Nullable
    private OnActivityResult<Result> onActivityResult;

    private BetterActivityLauncher(@NonNull ActivityResultCaller caller,
                                   @NonNull ActivityResultContract<Input, Result> contract,
                                   @Nullable OnActivityResult<Result> onActivityResult) {
        this.onActivityResult = onActivityResult;
        this.launcher = caller.registerForActivityResult(contract, this::callOnActivityResult);
    }

    public void setOnActivityResult(@Nullable OnActivityResult<Result> onActivityResult) {
        this.onActivityResult = onActivityResult;
    }

    /**
     * Launch activity, same as {@link ActivityResultLauncher#launch(Object)} except that it allows a callback
     * executed after receiving a result from the target activity.
     */
    public void launch(Input input, @Nullable OnActivityResult<Result> onActivityResult) {
        if (onActivityResult != null) {
            this.onActivityResult = onActivityResult;
        }
        launcher.launch(input);
    }

    /**
     * Same as {@link #launch(Object, OnActivityResult)} with last parameter set to {@code null}.
     */
    public void launch(Input input) {
        launch(input, this.onActivityResult);
    }

    private void callOnActivityResult(Result result) {
        if (onActivityResult != null) onActivityResult.onActivityResult(result);
    }
}