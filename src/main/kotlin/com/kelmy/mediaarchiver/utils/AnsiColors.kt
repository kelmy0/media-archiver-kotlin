package com.kelmy.mediaarchiver.utils

class AnsiColors private constructor() {
    companion object {
        const val RESET = "\u001B[0m"

        const val BOLD = "\u001B[1m"
        const val UNDERLINE = "\u001B[4m"

        const val BLACK = "\u001B[30m"
        const val RED = "\u001B[31m"
        const val GREEN = "\u001B[32m"
        const val YELLOW = "\u001B[33m"
        const val BLUE = "\u001B[34m"
        const val PURPLE = "\u001B[35m"
        const val CYAN = "\u001B[36m"
        const val WHITE = "\u001B[37m"

        const val BG_RED = "\u001B[41m"
        const val BG_GREEN = "\u001B[42m"
        const val BG_YELLOW = "\u001B[43m"
    }
}
