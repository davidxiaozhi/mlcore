/*
 * Tencent is pleased to support the open source community by making Angel available.
 *
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/Apache-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package com.tencent.angel.mlcore

import com.tencent.angel.ml.math2.utils.LabeledData
import com.tencent.angel.mlcore.conf.{MLCoreConf, SharedConf}
import com.tencent.angel.ml.math2.utils.DataBlock
import com.tencent.angel.mlcore.local.LocalLearner
import com.tencent.angel.mlcore.local.data.LocalDataReader
import com.tencent.angel.mlcore.utils.JsonUtils
import org.apache.hadoop.conf.Configuration
import org.scalatest.{BeforeAndAfter, FunSuite}

class AlgoTest extends FunSuite with BeforeAndAfter {
  var conf: SharedConf = new SharedConf
  var reader: LocalDataReader = _
  var taskDataBlock: DataBlock[LabeledData] = _
  var posDataBlock: DataBlock[LabeledData] = _
  var negDataBlock: DataBlock[LabeledData] = _
  var validDataBlock: DataBlock[LabeledData] = _

  def getJson(name: String): String = {
    s"./src/test/jsons/$name.json"
  }

  def getDataFile(name: String, format: String = "libsvm", aType: String = "train"): String = {
    conf.set(MLCoreConf.ML_DATA_INPUT_FORMAT, format)
    val dim: Int = name match {
      case "a9a" => 123
      case "abalone" => 8
      case "agaricus" => 127
      case "protein" => 357
      case "census" => 148
      case "usps" => 256
      case "w6a" => 300
      case _ => throw new Exception("Cannot find data set!")
    }

    s"./data/$name/${name}_${dim}d_$aType.$format"
  }

  def init1(jsonFile: String, sourceFile: String): Unit = {
    conf.set(MLCoreConf.ML_JSON_CONF_FILE, jsonFile)
    JsonUtils.parseAndUpdateJson(jsonFile, conf, new Configuration())

    reader = new LocalDataReader(conf)
    val iter = reader.sourceIter(sourceFile)
    val dataBlocks = reader.readData2(iter)
    taskDataBlock = dataBlocks._1
    validDataBlock = dataBlocks._2

    taskDataBlock.shuffle()
  }

  def init2(jsonFile: String, sourceFile: String): Unit = {
    conf.set(MLCoreConf.ML_JSON_CONF_FILE, jsonFile)
    JsonUtils.parseAndUpdateJson(jsonFile, conf, new Configuration())

    reader = new LocalDataReader(conf)
    val iter = reader.sourceIter(sourceFile)
    val dataBlocks = reader.readData3(iter)
    posDataBlock = dataBlocks._1
    negDataBlock = dataBlocks._2
    validDataBlock = dataBlocks._3
  }

  def train1(): Unit = {
    val learner = new LocalLearner(conf)
    // learner.train(taskDataBlock, validDataBlock)
    // learner.model.predict(validDataBlock).foreach(res => println(res.getText))
    learner.train(taskDataBlock, validDataBlock)
    println(learner.model.predict(validDataBlock.get(0)).getText)
  }

  def train2(): Unit = {
    val learner = new LocalLearner(conf)
    learner.train(posDataBlock, negDataBlock, validDataBlock)
  }

  test("DAW") {
    init2(getJson("daw"), getDataFile("census"))
    train2()
  }

  test("DeepFM") {
    init2(getJson("deepfm"), getDataFile("census", "dummy"))
    train2()
  }

  test("DNN") {
    conf.set(MLCoreConf.ML_LEARN_RATE, "0.001")
    init2(getJson("dnn"), getDataFile("census"))
    train2()
  }

  test("FM") {
    init2(getJson("fm"), getDataFile("census", "dummy"))
    train2()
  }

  test("LinReg") {
    init2(getJson("linreg"), getDataFile("a9a"))
    train2()
  }

  test("LogReg") {
    init2(getJson("logreg"), getDataFile("a9a"))
    train2()
  }

  test("MixedLR") {
    init2(getJson("mixedlr"), getDataFile("a9a"))
    train2()
  }

  test("NFM") {
    init2(getJson("nfm"), getDataFile("census", "dummy"))
    train2()
  }

  test("PNN") {
    init2(getJson("pnn"), getDataFile("census", "dummy"))
    train2()
  }

  test("RobustReg") {
    init2(getJson("robustreg"), getDataFile("a9a"))
    train2()
  }

  test("Softmax") {
    init2(getJson("softmax"), getDataFile("protein"))
    train2()
  }

  test("SVM") {
    init2(getJson("svm"), getDataFile("a9a", "dummy"))
    train2()
  }

  test("AFM") {
    init2(getJson("afm"), getDataFile("census"))
    train2()
  }

  test("DCN") {
    init2(getJson("dcn"), getDataFile("census"))
    train2()
  }

  test("Kmeans") {
    init1(getJson("kmeans"), getDataFile("a9a"))
    train1()
  }
}
