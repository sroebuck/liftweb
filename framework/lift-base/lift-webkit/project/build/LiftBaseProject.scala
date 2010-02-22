import sbt._

class LiftBaseProject(info: ProjectInfo) extends DefaultWebProject(info)
{
  // Compile...
  val lift = "net.liftweb" % "lift-core" % "2.0-scala280-SNAPSHOT" % "compile"
  // Lift util
  // Lift json
  val logging = "commons-logging" % "commons-logging" % "1.1.1" % "compile"
  val upload = "commons-fileupload" % "commons-fileupload"
  // Test...
  val scalatest = "org.scalatest" % "scalatest" % "1.0.1-for-scala-2.8.0.Beta1-with-test-interfaces-0.3-SNAPSHOT" % "test"
  val junit = "junit" % "junit" % "4.8.1" % "test"
  val jetty6 = "org.mortbay.jetty" % "jetty" % "6.1.22" % "test"
  // Runtime...
  val derby = "org.apache.derby" % "derby" % "10.4.2.0" % "runtime"
  // Provided...
  val mail = "javax.mail" % "mail" % "1.4.2" % "provided"
  val servlet = "javax.servlet" % "servlet-api" % "2.5" % "provided"


  // Note that scala-compiler does not explicitly need to be included for SBT as it identifies the
  // dependency on its own.

  // required because Ivy doesn't pull repositories from poms
  val smackRepo = "m2-repository-smack" at "http://maven.reucon.com/public"

  // Required for additional Lift snapshot bits and pieces...
  val scalaDev = "scala-tools-dev" at "http://nexus.scala-tools.org/content/repositories/snapshots"
  //val terracottaRepo = "terracotta" at "http://www.terracotta.org/download/reflector/maven2"
  val nexusRepo = "nexus" at "https://nexus.griddynamics.net/nexus/content/groups/public"

}
