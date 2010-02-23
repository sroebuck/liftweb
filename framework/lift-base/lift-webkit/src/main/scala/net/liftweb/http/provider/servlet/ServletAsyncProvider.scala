/*
 * Copyright 2007-2009 WorldWide Conferencing, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */

package net.liftweb.http.provider.servlet

/**
 * Abstracts the management of asynchronous HTTP requests in order
 * to allow requests to be suspended and resumed later on.
 *  
 */
trait ServletAsyncProvider {

  /**
   * Returns whether or not the Servlet Async Provider has suspend and resume support.
   *
   * @return true if the underlying JEE container supports suspend/resume
   */
  def suspendResumeSupport_? : Boolean

  /**
   * Return the object (if any) associated with the resume call.
   *
   * @return the reference that was provided in the resume call 
   */ 
  def resumeInfo : Option[Any]

  /**
   * Suspends this request for a given period of time
   * 
   * @param timeout
   */
  def suspend(timeout: Long): Any
  
  /**
   * Resumes this request
   *
   * @param ref an object that will be associated with the resumed request
   */
  def resume(ref: AnyRef): Unit

}
