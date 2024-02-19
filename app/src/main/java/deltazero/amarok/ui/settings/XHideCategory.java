package deltazero.amarok.ui.settings;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;
import deltazero.amarok.utils.XHideUtil;
import rikka.material.preference.MaterialSwitchPreference;

public class XHideCategory extends BaseCategory {
    public XHideCategory(@NonNull FragmentActivity activity, @NonNull PreferenceScreen screen) {
        super(activity, screen);
        setTitle(R.string.x_hide);

        var xHideStatus = new Preference(activity);
        // xHideStatus.setTitle(R.string.x_hide);
        xHideStatus.setSummary(R.string.x_hide_description);
        xHideStatus.setEnabled(XHideUtil.isAvailable);
        addPreference(xHideStatus);

        var enableXHide = new MaterialSwitchPreference(activity);
        enableXHide.setKey(PrefMgr.ENABLE_X_HIDE);
        enableXHide.setTitle(R.string.enable_x_hide);
        enableXHide.setIcon(R.drawable.domino_mask_fill0_wght400_grad0_opsz24);
        enableXHide.setSummary(XHideUtil.isAvailable ?
                activity.getString(R.string.xposed_active, XHideUtil.xposedVersion)
                : activity.getString(R.string.xposed_inactive));
        enableXHide.setEnabled(XHideUtil.isAvailable);
        addPreference(enableXHide);
    }
}
