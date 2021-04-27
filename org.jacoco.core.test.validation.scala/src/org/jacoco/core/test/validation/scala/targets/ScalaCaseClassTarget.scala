/*******************************************************************************
 * Copyright (c) 2009, 2020 Mountainminds GmbH & Co. KG and Contributors
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Arvind Waiker - initial API and implementation
 *
 *******************************************************************************/
package org.jacoco.core.test.validation.scala.targets

/**
 * Scala Case Class Target
 * @param args
 */
object ScalaCaseClassTarget {

  def main(args: Array[String]): Unit = {
    val p1 = new Point(10,20)
    val p2 = new Point(1,2)
    val slope = (p1.y - p2.y) / (p1.x - p2.x)
  }

  case class Point(x: Int, y: Int) // assertFullyCovered()

}
