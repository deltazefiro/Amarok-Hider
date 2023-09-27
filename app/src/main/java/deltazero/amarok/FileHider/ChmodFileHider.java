package deltazero.amarok.FileHider;

import android.content.Context;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

import deltazero.amarok.utils.ShellUtil;

public class ChmodFileHider extends BaseFileHider {

    public ChmodFileHider(Context context) {
        super(context);
    }

    @Override
    protected void process(Set<String> targetDirs, ProcessMethod method) throws InterruptedException {

        var processDirs = new HashSet<String>();
        for (var d : targetDirs) {
            if (d.startsWith("/storage/emulated/0/"))
                processDirs.add(d.replace("/storage/emulated/", "/data/media/"));
            else
                Log.w("ChmodFileHider", String.format("Unsupported path: %s", d));
        }

        StringBuilder cmd = new StringBuilder("echo amarok");
        for (String d : processDirs)
            cmd.append(String.format("&chmod -R %s %s", method == ProcessMethod.HIDE ? 0 : 2770, d));
        ShellUtil.exec(new String[]{"su", "-c", cmd.toString()});
    }
}
