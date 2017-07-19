// Copyright: 2010 - 2017 https://github.com/ensime/ensime-server/graphs
// License: http://www.gnu.org/licenses/gpl-3.0.en.html
package org.ensime.util.fixtures

import java.nio.file.{ Path, Paths }

import scala.util.Properties.jdkHome

import org.ensime.util.path._
import org.scalatest.Assertions

trait JdkSrcFixture extends Assertions {

  def jdkSrc(): Path = {
    val src: Path = Paths.get(jdkHome) / "src.zip"
    assume(src.exists(), "Please make sure you have JDK source code available at `$JAVA_HOME/src.zip`.")

    src
  }

}
