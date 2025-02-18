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

package com.tencent.angel.mlcore.optimizer.decayer

import com.tencent.angel.mlcore.conf.{MLCoreConf, SharedConf}

class CorrectionDecay(eta: Double, alpha: Double, beta: Double)(implicit conf: SharedConf) extends StepSizeScheduler {

  var current: Int = 0
  val interval: Int = conf.getInt(MLCoreConf.ML_OPT_DECAY_INTERVALS,
    MLCoreConf.DEFAULT_ML_OPT_DECAY_INTERVALS)

  override def next(): Double = {
    val eta_time = eta / math.sqrt(1 + alpha * current) * (1 - beta) / (1 - math.pow(beta, current))
    current += 1

    eta_time
  }

  override def isIntervalBoundary: Boolean = {
    current % interval == 0
  }

}
