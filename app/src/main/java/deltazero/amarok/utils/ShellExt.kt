package deltazero.amarok.utils

import com.topjohnwu.superuser.Shell
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

suspend fun Shell.Job.await(): Shell.Result = suspendCancellableCoroutine { cont ->
  submit { result -> cont.resume(result) }
}

suspend fun awaitShell(): Shell = suspendCancellableCoroutine { cont ->
  Shell.getShell { shell -> cont.resume(shell) }
}
