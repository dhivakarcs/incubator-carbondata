/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.carbondata.spark.testsuite.bigdecimal

import org.apache.spark.sql.common.util.CarbonHiveContext._
import org.apache.spark.sql.common.util.QueryTest
import org.scalatest.BeforeAndAfterAll

import org.apache.carbondata.core.constants.CarbonCommonConstants
import org.apache.carbondata.core.util.CarbonProperties

/**
  * Test cases for testing big decimal functionality
  */
class TestBigDecimal extends QueryTest with BeforeAndAfterAll {

  override def beforeAll {
    sql("drop table if exists carbonTable")
    sql("drop table if exists hiveTable")
    sql("drop table if exists hiveBigDecimal")
    sql("drop table if exists carbonBigDecimal_2")
    CarbonProperties.getInstance()
      .addProperty(CarbonCommonConstants.CARBON_TIMESTAMP_FORMAT, "yyyy/MM/dd")
    CarbonProperties.getInstance().addProperty(CarbonCommonConstants.SORT_SIZE, "1")
    CarbonProperties.getInstance().addProperty(CarbonCommonConstants.SORT_INTERMEDIATE_FILES_LIMIT, "2")
    sql("CREATE TABLE IF NOT EXISTS carbonTable (ID Int, date Timestamp, country String, name String, phonetype String, serialname String, salary Decimal(17,2))STORED BY 'org.apache.carbondata.format'")
    sql("create table if not exists hiveTable(ID Int, date Timestamp, country String, name String, phonetype String, serialname String, salary Decimal(17,2))row format delimited fields terminated by ','")
    sql("LOAD DATA LOCAL INPATH './src/test/resources/decimalDataWithHeader.csv' into table carbonTable")
    sql("LOAD DATA local inpath './src/test/resources/decimalDataWithoutHeader.csv' INTO table hiveTable")
    sql("create table if not exists hiveBigDecimal(ID Int, date Timestamp, country String, name String, phonetype String, serialname String, salary decimal(27, 10))row format delimited fields terminated by ','")
    sql("LOAD DATA local inpath './src/test/resources/decimalBoundaryDataHive.csv' INTO table hiveBigDecimal")
    sql("create table if not exists carbonBigDecimal_2 (ID Int, date Timestamp, country String, name String, phonetype String, serialname String, salary decimal(30, 10)) STORED BY 'org.apache.carbondata.format'")
    sql("LOAD DATA LOCAL INPATH './src/test/resources/decimalBoundaryDataCarbon.csv' into table carbonBigDecimal_2")
  }

  test("test detail query on big decimal column") {
    checkAnswer(sql("select salary from carbonTable order by salary"),
      sql("select salary from hiveTable order by salary"))
  }

  test("test sum function on big decimal column") {
    checkAnswer(sql("select sum(salary) from carbonTable"),
      sql("select sum(salary) from hiveTable"))
  }

  test("test max function on big decimal column") {
    checkAnswer(sql("select max(salary) from carbonTable"),
      sql("select max(salary) from hiveTable"))
  }

  test("test min function on big decimal column") {
    checkAnswer(sql("select min(salary) from carbonTable"),
      sql("select min(salary) from hiveTable"))
  }
  
  test("test min datatype on big decimal column") {
    val output = sql("select min(salary) from carbonTable").collectAsList().get(0).get(0)
    assert(output.isInstanceOf[java.math.BigDecimal])
  }

  test("test max datatype on big decimal column") {
    val output = sql("select max(salary) from carbonTable").collectAsList().get(0).get(0)
    assert(output.isInstanceOf[java.math.BigDecimal])
  }
  
  test("test count function on big decimal column") {
    checkAnswer(sql("select count(salary) from carbonTable"),
      sql("select count(salary) from hiveTable"))
  }

  test("test distinct function on big decimal column") {
    checkAnswer(sql("select distinct salary from carbonTable order by salary"),
      sql("select distinct salary from hiveTable order by salary"))
  }

  test("test sum-distinct function on big decimal column") {
    checkAnswer(sql("select sum(distinct salary) from carbonTable"),
      sql("select sum(distinct salary) from hiveTable"))
  }

  test("test count-distinct function on big decimal column") {
    checkAnswer(sql("select count(distinct salary) from carbonTable"),
      sql("select count(distinct salary) from hiveTable"))
  }
  test("test filter query on big decimal column") {
    // equal to
    checkAnswer(sql("select salary from carbonTable where salary=45234525465882.24"),
      sql("select salary from hiveTable where salary=45234525465882.24"))
    // greater than
    checkAnswer(sql("select salary from carbonTable where salary>15000"),
      sql("select salary from hiveTable where salary>15000"))
    // greater than equal to
    checkAnswer(sql("select salary from carbonTable where salary>=15000.43525"),
      sql("select salary from hiveTable where salary>=15000.43525"))
    // less than
    checkAnswer(sql("select salary from carbonTable where salary<45234525465882"),
      sql("select salary from hiveTable where salary<45234525465882"))
    // less than equal to
    checkAnswer(sql("select salary from carbonTable where salary<=45234525465882.24"),
      sql("select salary from hiveTable where salary<=45234525465882.24"))
  }

  test("test aggregation on big decimal column with increased precision") {
    sql("drop table if exists carbonBigDecimal")
    sql("create table if not exists carbonBigDecimal (ID Int, date Timestamp, country String, name String, phonetype String, serialname String, salary decimal(27, 10)) STORED BY 'org.apache.carbondata.format'")
    sql("LOAD DATA LOCAL INPATH './src/test/resources/decimalBoundaryDataCarbon.csv' into table carbonBigDecimal")

    checkAnswer(sql("select sum(salary) from carbonBigDecimal"),
      sql("select sum(salary) from hiveBigDecimal"))

    checkAnswer(sql("select sum(distinct salary) from carbonBigDecimal"),
      sql("select sum(distinct salary) from hiveBigDecimal"))

    sql("drop table if exists carbonBigDecimal")
  }

  test("test big decimal for dictionary look up") {
    sql("drop table if exists decimalDictLookUp")
    sql("create table if not exists decimalDictLookUp (ID Int, date Timestamp, country String, name String, phonetype String, serialname String, salary decimal(27, 10)) STORED BY 'org.apache.carbondata.format' TBLPROPERTIES('dictionary_include'='salary')")
    sql("LOAD DATA LOCAL INPATH './src/test/resources/decimalBoundaryDataCarbon.csv' into table decimalDictLookUp")

    checkAnswer(sql("select sum(salary) from decimalDictLookUp"),
      sql("select sum(salary) from hiveBigDecimal"))

    sql("drop table if exists decimalDictLookUp")
  }

  test("test sum+10 aggregation on big decimal column with high precision") {
    checkAnswer(sql("select sum(salary)+10 from carbonBigDecimal_2"),
      sql("select sum(salary)+10 from hiveBigDecimal"))
  }

  test("test sum*10 aggregation on big decimal column with high precision") {
    checkAnswer(sql("select sum(salary)*10 from carbonBigDecimal_2"),
      sql("select sum(salary)*10 from hiveBigDecimal"))
  }

  test("test sum/10 aggregation on big decimal column with high precision") {
    checkAnswer(sql("select sum(salary)/10 from carbonBigDecimal_2"),
      sql("select sum(salary)/10 from hiveBigDecimal"))
  }

  test("test sum-distinct+10 aggregation on big decimal column with high precision") {
    checkAnswer(sql("select sum(distinct(salary))+10 from carbonBigDecimal_2"),
      sql("select sum(distinct(salary))+10 from hiveBigDecimal"))
  }

  test("test sum-distinct*10 aggregation on big decimal column with high precision") {
    checkAnswer(sql("select sum(distinct(salary))*10 from carbonBigDecimal_2"),
      sql("select sum(distinct(salary))*10 from hiveBigDecimal"))
  }

  test("test sum-distinct/10 aggregation on big decimal column with high precision") {
    checkAnswer(sql("select sum(distinct(salary))/10 from carbonBigDecimal_2"),
      sql("select sum(distinct(salary))/10 from hiveBigDecimal"))
  }

  test("test avg+10 aggregation on big decimal column with high precision") {
    checkAnswer(sql("select avg(salary)+10 from carbonBigDecimal_2"),
      sql("select avg(salary)+10 from hiveBigDecimal"))
  }

  test("test avg*10 aggregation on big decimal column with high precision") {
    checkAnswer(sql("select avg(salary)*10 from carbonBigDecimal_2"),
      sql("select avg(salary)*10 from hiveBigDecimal"))
  }

  test("test avg/10 aggregation on big decimal column with high precision") {
    checkAnswer(sql("select avg(salary)/10 from carbonBigDecimal_2"),
      sql("select avg(salary)/10 from hiveBigDecimal"))
  }

  override def afterAll {
    sql("drop table if exists carbonTable")
    sql("drop table if exists hiveTable")
    sql("drop table if exists hiveBigDecimal")
    sql("drop table if exists carbonBigDecimal_2")
    CarbonProperties.getInstance()
      .addProperty(CarbonCommonConstants.CARBON_TIMESTAMP_FORMAT, "dd-MM-yyyy")
    CarbonProperties.getInstance().addProperty(CarbonCommonConstants.SORT_SIZE,
      CarbonCommonConstants.SORT_SIZE_DEFAULT_VAL)
    CarbonProperties.getInstance().addProperty(CarbonCommonConstants.SORT_INTERMEDIATE_FILES_LIMIT,
      CarbonCommonConstants.SORT_INTERMEDIATE_FILES_LIMIT_DEFAULT_VALUE)
  }
}


