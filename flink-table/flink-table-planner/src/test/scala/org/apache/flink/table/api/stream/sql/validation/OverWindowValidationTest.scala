/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.flink.table.api.stream.sql.validation

import org.apache.flink.api.scala._
import org.apache.flink.table.api._
import org.apache.flink.table.api.bridge.scala._
import org.apache.flink.table.planner.runtime.utils.JavaUserDefinedAggFunctions.OverAgg0
import org.apache.flink.table.planner.utils.TableTestBase
import org.apache.flink.types.Row

import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test

class OverWindowValidationTest extends TableTestBase {

  private val streamUtil = scalaStreamTestUtil()
  streamUtil.addDataStream[(Int, String, Long)]("T1", 'a, 'b, 'c, 'proctime.proctime)

  /** All aggregates must be computed on the same window. */
  @Test
  def testMultiWindow(): Unit = {

    val sqlQuery = "SELECT " +
      "c, " +
      "count(a) OVER (PARTITION BY c ORDER BY proctime RANGE UNBOUNDED preceding), " +
      "sum(a) OVER (PARTITION BY b ORDER BY proctime RANGE UNBOUNDED preceding) " +
      "from T1"

    assertThatExceptionOfType(classOf[TableException])
      .isThrownBy(() => streamUtil.tableEnv.sqlQuery(sqlQuery).toAppendStream[Row])
  }

  /** OVER clause is necessary for [[OverAgg0]] window function. */
  @Test
  def testInvalidOverAggregation(): Unit = {
    streamUtil.addTemporarySystemFunction("overAgg", new OverAgg0)

    val sqlQuery = "SELECT overAgg(c, a) FROM MyTable"

    assertThatExceptionOfType(classOf[ValidationException])
      .isThrownBy(() => streamUtil.tableEnv.sqlQuery(sqlQuery))
  }
}
