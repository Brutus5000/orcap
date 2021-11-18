package net.brutus5000.orcap.shared

import java.io.IOException

class UplinkNotAvailableException : IOException()
class IllegalPathException(message: String, subPath: String) : IOException("$message (subPath: $subPath)")
