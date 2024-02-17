package deltazero.amarok.xposed.hooks;

public abstract class BaseHook {
    public boolean isInit = false;

    public abstract String getName();

    public abstract void init();
}
