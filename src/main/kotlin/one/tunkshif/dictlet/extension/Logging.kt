package one.tunkshif.dictlet.extension

import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface Logging

inline fun <reified T : Logging> T.logger(): Logger = LoggerFactory.getLogger(T::class.java)
