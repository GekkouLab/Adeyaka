@file:JvmName("Constants")

package gl.ky.adeyaka

import java.util.logging.Logger

lateinit var main: Adeyaka
lateinit var log: Logger

fun info(s: String) = log.info(s)