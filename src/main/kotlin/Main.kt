import com.kelmy.mediaarchiver.bootstrap.Application

/**
 * Entry point of the application (the only file in the default package).
 *
 * It does nothing but hand the command-line arguments to [Application], which chooses the interface
 * (`--cli` or `--gui`) through the abstract factory, so this file never has to change when an interface is added.
 *
 * Arguments: `--seed` (import `seed.local.json` and exit), `--cli` (console menu), `--gui` (Swing window, the default).
 */
fun main(args: Array<String>) = Application.run(args)
