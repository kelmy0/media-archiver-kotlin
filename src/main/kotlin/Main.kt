import com.kelmy.mediaarchiver.bootstrap.Application

/**
 * Entry point of the application.
 *
 * It does nothing but hand the command-line arguments to [Application], which chooses the interface
 * (`--cli` or `--gui`) through the abstract factory.
 *
 * Arguments: `--seed` (import `seed.local.json` and exit), `--cli` (console menu), `--gui` (Swing window, the default).
 */
object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        Application.run(args)
    }
}
