JaCoCo Java Code Coverage Library - Customized using sbt-jacoco code
=================================


Customized Jacoco Implementation using [sbt-jacoco](https://github.com/sbt/sbt-jacoco)

Below are the changes made to the original jacoco implementation
1. New Class Analyzer - ScalaClassAnalyzer.
2. Changes to addMethodCoverage method to ignore scala generated code.
3. Changes to Analyzer to use the new ScalaClassAnalyzer.

The customization does not impact any of the other languages like java, scala, kotlin, scala

-------------------------------------------------------------------------