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
package com.tencent.angel.mlcore.utils


import com.tencent.angel.mlcore.PredictResult
import com.tencent.angel.mlcore.optimizer.loss._
import org.apache.commons.logging.{Log, LogFactory}

object ValidationUtils {
  private val LOG: Log = LogFactory.getLog("ValidationUtils")

  private implicit val keyOrdering = new Ordering[PredictResult] {
    override def compare(x: PredictResult, y: PredictResult): Int = {
      if (x.proba == Double.NaN || y.proba == Double.NaN) {
        0
      } else if (x.proba > y.proba) {
        1
      } else if (x.proba < y.proba) {
        -1
      } else {
        0
      }
    }
  }

  def calMetrics(epoch: Int, result: List[PredictResult], lossFunc: LossFunc): Unit = {
    lossFunc match {
      case lfunc: L2Loss =>
        calRegressionMetrics(epoch, result, lfunc)
      case lfunc: HuberLoss =>
        calRegressionMetrics(epoch, result, lfunc)
      case lfunc: SoftmaxLoss =>
        calMultiClassMetrics(epoch, result, lfunc)
      case lfunc: KmeansLoss =>
        calKmeansMetrics(epoch, result, lossFunc)
      case _ =>
        calClassifyMetrics(epoch, result, lossFunc)
    }
  }

  private def calMultiClassMetrics(epoch: Int, result: List[PredictResult], lossFunc: LossFunc): Unit = {
    var loss: Double = 0.0
    var acc: Double = 0.0

    result.foreach { pr =>
      loss += lossFunc.loss(pr.attached, pr.trueLabel)
      val bj = if (pr.predLabel == pr.trueLabel) {
        1.0
      } else {
        0.0
      }

      acc += bj
    }

    loss /= result.size
    acc = acc / result.size

    println(f"epoch=$epoch, loss=$loss%.2f, acc=${100 * acc}%.2f")
    LOG.info(f"epoch=$epoch, loss=$loss%.2f, acc=${100 * acc}%.2f")
  }

  private def calClassifyMetrics(epoch: Int, result: List[PredictResult], lossFunc: LossFunc): Unit = {
    var (tp: Int, fp: Int, tn: Int, fn: Int) = (1, 1, 1, 1)
    var sigma: Double = 0.0
    var loss: Double = 0.0

    val sorted = result.sorted
    sorted.zipWithIndex.foreach { case (pr, idx) =>
      loss += lossFunc.loss(pr.pred, pr.trueLabel)
      if (pr.proba >= 0.5 && pr.trueLabel == 1) {
        tp += 1
        sigma += idx + 1
      } else if (pr.proba < 0.5 && pr.trueLabel == 1) {
        fn += 1
        sigma += idx + 1
      } else if (pr.proba < 0.5 && pr.trueLabel == -1) {
        tn += 1
      } else {
        fp += 1
      }
    }

    loss /= result.size

    val pos = tp + fn
    val neg = tn + fp
    val trueRecall = 100.0 * tp / pos
    val falseRecall = 100.0 * tn / neg
    val acc = 100.0 * (tp + tn) / (pos + neg)
    val auc = (sigma - (pos + 1) * pos / 2) / (pos * neg)

    println(f"epoch=$epoch, loss=$loss%.2f, acc=$acc%.2f, auc=$auc%.2f, trueRecall=$trueRecall%.2f, falseRecall=$falseRecall%.2f")
    LOG.info(f"loss=$loss%.2f, acc=$acc%.2f, auc=$auc%.2f, trueRecall=$trueRecall%.2f, falseRecall=$falseRecall%.2f")
  }

  private def calRegressionMetrics(epoch: Int, result: List[PredictResult], lossFunc: LossFunc): Unit = {
    var loss: Double = 0.0
    var sse: Double = 0.0

    result.foreach { pr =>
      loss += lossFunc.loss(pr.pred, pr.trueLabel)
      sse += Math.pow(pr.pred - pr.trueLabel, 2)
    }

    loss /= result.size
    sse = Math.sqrt(sse / result.size)

    println(f"epoch=$epoch, loss=$loss%.2f, sse=$sse%.2f")
    LOG.info(f"epoch=$epoch, loss=$loss%.2f, sse=$sse%.2f")
  }

  private def calKmeansMetrics(epoch: Int, result: List[PredictResult], lossFunc: LossFunc): Unit = {
    var loss: Double = 0.0

    result.foreach { pr =>
      loss += lossFunc.loss(pr.pred, pr.predLabel)
    }

    loss /= result.size

    println(f"epoch=$epoch, loss=$loss%.2f")
    LOG.info(f"epoch=$epoch, loss=$loss%.2f")
  }
}
