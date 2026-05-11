package deltazero.amarok.utils

import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts

/**
 * A wrapper class for [ActivityResultLauncher] that allows customising callback after receiving a
 * result from the target activity.
 *
 * @see [https://stackoverflow.com/a/63654043/19827111]
 * @author Muntashir Akon
 */
class BetterActivityLauncher<Input, Result>
private constructor(
  caller: ActivityResultCaller,
  contract: ActivityResultContract<Input, Result>,
  private var onActivityResult: OnActivityResult<Result>?,
) {
  /** Callback interface */
  fun interface OnActivityResult<O> {
    /** Called after receiving a result from the target activity */
    fun onActivityResult(result: O)
  }

  private val launcher: ActivityResultLauncher<Input> =
    caller.registerForActivityResult(contract, this::callOnActivityResult)

  fun setOnActivityResult(onActivityResult: OnActivityResult<Result>?) {
    this.onActivityResult = onActivityResult
  }

  /**
   * Launch activity, same as [ActivityResultLauncher.launch] except that it allows a callback
   * executed after receiving a result from the target activity.
   */
  fun launch(input: Input, onActivityResult: OnActivityResult<Result>?) {
    if (onActivityResult != null) {
      this.onActivityResult = onActivityResult
    }
    launcher.launch(input)
  }

  /** Same as [launch] with last parameter set to `null`. */
  fun launch(input: Input) {
    launch(input, onActivityResult)
  }

  private fun callOnActivityResult(result: Result) {
    onActivityResult?.onActivityResult(result)
  }

  companion object {
    /**
     * Register activity result using a [ActivityResultContract] and an in-place activity result
     * callback like the default approach. You can still customise callback using [launch].
     */
    @JvmStatic
    fun <Input, Result> registerForActivityResult(
      caller: ActivityResultCaller,
      contract: ActivityResultContract<Input, Result>,
      onActivityResult: OnActivityResult<Result>?,
    ): BetterActivityLauncher<Input, Result> =
      BetterActivityLauncher(caller, contract, onActivityResult)

    /** Same as [registerForActivityResult] except the last argument is set to `null`. */
    @JvmStatic
    fun <Input, Result> registerForActivityResult(
      caller: ActivityResultCaller,
      contract: ActivityResultContract<Input, Result>,
    ): BetterActivityLauncher<Input, Result> = registerForActivityResult(caller, contract, null)

    /** Specialised method for launching new activities. */
    @JvmStatic
    fun registerActivityForResult(
      caller: ActivityResultCaller
    ): BetterActivityLauncher<Intent, ActivityResult> =
      registerForActivityResult(caller, ActivityResultContracts.StartActivityForResult())
  }
}
