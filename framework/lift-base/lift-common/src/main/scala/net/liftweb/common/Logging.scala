/*
 * Copyright 2010 WorldWide Conferencing, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.liftweb {
package common {
  
import _root_.org.slf4j.{MDC => SLF4JMDC, Marker, Logger => SLF4JLogger, LoggerFactory}

object Logger {
  def apply(clz: Class[_]): LiftLogger = new WrappedLogger(LoggerFactory.getLogger(clz))
  def apply(name: String): LiftLogger = new WrappedLogger(LoggerFactory.getLogger(name))
  
 /**
   * Set the Mapped Diagnostic Context for the thread and execute
   * the function f
   *
   * Upon return, the MDC is cleared of the values passed (any
   * MDC values that existed prior to this call remains)
   */
  def logWith[F](mdcValues: (String,Any)*)(f: => F): F = {
    val old = SLF4JMDC.getCopyOfContextMap
    MDC.put(mdcValues:_*)
    try {
      f
    } finally {
      if (old eq null) {
        MDC.clear
      }
      else          {
        SLF4JMDC.setContextMap(old)
      }
    }
  }
}

/**
 * The Mapped Diagnostics Context can hold values per thread and output them
 * with each logged output.
 * 
 * The logging backend needs to be configured to log these values
 */
object MDC {
  /**
   * Put a (key,value) pair into the Mapped Diagnostic Context
   */
  def put(kvs: (String,Any)*) = {
    kvs foreach {v => SLF4JMDC.put(v._1, v._2.toString)}
  }

  /**
   * Clear key from the Mapped Diagnostic Context
   */
  def remove(keys: String*) = {
    keys foreach {k => SLF4JMDC.remove(k)}
  }

  /**
   * Clear all entries from the Mapped Diagnostic Context
   */
  def clear() = org.slf4j.MDC.clear
}

/**
 * LiftLogegr is a thin wrapper on top of an SLF4J Logger
 *
 * The main purpose is to utilize Scala features for logging
 * 
 */
trait LiftLogger {
  @transient val logger: SLF4JLogger 
  
  def assertLog(assertion: Boolean, msg: => String) = if (assertion) info(msg)

  /**
   * Print the value of v and return it Useful for tracing values in expressions
   */
  def trace[T](msg: String, v: T): T = {
    logger.info(msg+": "+v.toString)
    v
  }
  
  def trace(msg: => AnyRef) = if (logger.isTraceEnabled) logger.trace(String.valueOf(msg))
  def trace(msg: => AnyRef, t: Throwable) = if (logger.isTraceEnabled) logger.trace(String.valueOf(msg), t)
  def trace(msg: => AnyRef, marker:  Marker) = if (logger.isTraceEnabled) logger.trace(marker,String.valueOf(msg))
  def trace(msg: => AnyRef, t: Throwable, marker: => Marker) = if (logger.isTraceEnabled) logger.trace(marker,String.valueOf(msg), t)

  def debug(msg: => AnyRef) = if (logger.isDebugEnabled) logger.debug(String.valueOf(msg))
  def debug(msg: => AnyRef, t:  Throwable) = if (logger.isDebugEnabled) logger.debug(String.valueOf(msg), t)
  def debug(msg: => AnyRef, marker: Marker) = if (logger.isDebugEnabled) logger.debug(marker, String.valueOf(msg))
  def debug(msg: => AnyRef, t: Throwable, marker: Marker) = if (logger.isDebugEnabled) logger.debug(marker, String.valueOf(msg), t)

  def info(msg: => AnyRef) = if (logger.isInfoEnabled) logger.info(String.valueOf(msg))
  def info(msg: => AnyRef, t: => Throwable) = if (logger.isInfoEnabled) logger.info(String.valueOf(msg), t)
  def info(msg: => AnyRef, marker: Marker) = if (logger.isInfoEnabled) logger.info(marker,String.valueOf(msg))
  def info(msg: => AnyRef, t: Throwable, marker: Marker) = if (logger.isInfoEnabled) logger.info(marker,String.valueOf(msg), t)

  def warn(msg: => AnyRef) = if (logger.isWarnEnabled) logger.warn(String.valueOf(msg))
  def warn(msg: => AnyRef, t: Throwable) = if (logger.isWarnEnabled) logger.warn(String.valueOf(msg), t)
  def warn(msg: => AnyRef, marker: Marker) = if (logger.isWarnEnabled) logger.warn(marker,String.valueOf(msg))
  def warn(msg: => AnyRef, t: Throwable, marker: Marker) = if (logger.isWarnEnabled) logger.warn(marker,String.valueOf(msg), t)

  def error(msg: => AnyRef) = if (logger.isErrorEnabled) logger.error(String.valueOf(msg))
  def error(msg: => AnyRef, t: Throwable) = if (logger.isErrorEnabled) logger.error(String.valueOf(msg), t)
  def error(msg: => AnyRef, marker: Marker) = if (logger.isErrorEnabled) logger.error(marker,String.valueOf(msg))
  def error(msg: => AnyRef, t: Throwable, marker: Marker) = if (logger.isErrorEnabled) logger.error(marker,String.valueOf(msg), t)
}

class WrappedLogger(val logger: SLF4JLogger) extends LiftLogger

/**
 * Mixin with a nested LiftLogger
 */
trait Logger {
  @transient protected val logger = Logger(this.getClass)
}

/**
 * Mixin with direct access to logging functionality
 */
trait Logging extends LiftLogger {
  val logger = LoggerFactory.getLogger(this.getClass)
}

}
}

