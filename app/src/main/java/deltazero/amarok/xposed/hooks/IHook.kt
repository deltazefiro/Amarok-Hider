package deltazero.amarok.xposed.hooks

interface IHook {
  fun getName(): String

  fun load()
}
