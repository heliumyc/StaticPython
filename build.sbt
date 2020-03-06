lazy val root = (project in file(".")).
  settings(
      name := "PythonCompiler",
      version := "0.1",
      scalaVersion := "2.13.1",
      mainClass in Compile := Some("Main")
  )

libraryDependencies += "com.github.pathikrit" %% "better-files" % "3.8.0"
libraryDependencies += "com.novocode" % "junit-interface" % "0.8" % "test->default"
