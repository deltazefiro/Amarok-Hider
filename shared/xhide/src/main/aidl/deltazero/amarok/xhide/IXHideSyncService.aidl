package deltazero.amarok.xhide;

import android.os.Bundle;

interface IXHideSyncService {
    Bundle getStatus();
    boolean pushSnapshot(in Bundle snapshot);
}
