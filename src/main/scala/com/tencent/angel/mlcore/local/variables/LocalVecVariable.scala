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
package com.tencent.angel.mlcore.local.variables

import java.util.Random

import com.tencent.angel.ml.math2.matrix.Matrix
import com.tencent.angel.ml.math2.storage._
import com.tencent.angel.ml.math2.utils.RowType
import com.tencent.angel.ml.math2.vector.Vector
import com.tencent.angel.ml.math2.{MFactory, StorageType}
import com.tencent.angel.mlcore.conf.SharedConf
import com.tencent.angel.mlcore.network.EnvContext
import com.tencent.angel.mlcore.utils.ValueNotAllowed
import com.tencent.angel.mlcore.variable.{Updater, VariableManager, VecVariable}


private[mlcore] class LocalVecVariable(name: String,
                                       val length: Long,
                                       updater: Updater,
                                       rowType: RowType,
                                       formatClassName: String,
                                       allowPullWithIndex: Boolean)
                                      (implicit conf: SharedConf, variableManager: VariableManager)
  extends LocalVariable(name, rowType, updater, formatClassName, allowPullWithIndex) with VecVariable {
  override protected var vector: Vector = _

  protected override def doCreate[T](envCtx: EnvContext[T]): Unit = {
    assert(envCtx == null || envCtx.client == null)
    storage = rowType match {
      case RowType.T_DOUBLE_DENSE =>
        MFactory.rbIntDoubleMatrix(numSlot + 1, length.toInt, StorageType.DENSE)
      case RowType.T_DOUBLE_SPARSE =>
        MFactory.rbIntDoubleMatrix(numSlot + 1, length.toInt, StorageType.SPARSE)
      case RowType.T_DOUBLE_SPARSE_LONGKEY =>
        MFactory.rbLongDoubleMatrix(numSlot + 1, length, StorageType.SPARSE)
      case RowType.T_FLOAT_DENSE =>
        MFactory.rbIntFloatMatrix(numSlot + 1, length.toInt, StorageType.DENSE)
      case RowType.T_FLOAT_SPARSE =>
        MFactory.rbIntFloatMatrix(numSlot + 1, length.toInt, StorageType.SPARSE)
      case RowType.T_FLOAT_SPARSE_LONGKEY =>
        MFactory.rbLongFloatMatrix(numSlot + 1, length, StorageType.SPARSE)
      case _ => throw ValueNotAllowed("Value Not Allowed, Only Float/Double Are Allowed!")
    }
  }

  protected override def doInit(taskFlag: Int): Unit = {
    if (taskFlag == 0 && rowType.isDense) {
      val random = new Random()
      storage.getRow(0).getStorage match {
        case s: IntDoubleDenseVectorStorage =>
          val values = s.getValues
          (0 until values.size).foreach { idx =>
            values(idx) = random.nextDouble() * stddev + mean
          }
        case s: IntFloatDenseVectorStorage =>
          val values = s.getValues
          val fstddev = stddev.toFloat
          val fmean = mean.toFloat
          (0 until values.size).foreach { idx =>
            values(idx) = random.nextFloat() * fstddev + fmean
          }
        case _ =>
      }
    }
  }

  protected override def doPull(epoch: Int, indices: Vector = null): Unit = {
    if (epoch == 0 && indices != null && rowType.isSparse) {
      val random = new Random()
      storage.getRow(0).getStorage match {
        case storage: IntDoubleSparseVectorStorage =>
          val idxs = indices.getStorage.asInstanceOf[IntIntDenseVectorStorage].getValues
          idxs.foreach { i =>
            if (!storage.hasKey(i)) {
              storage.set(i, random.nextDouble() * stddev + mean)
            }
          }
        case storage: LongDoubleSparseVectorStorage =>
          val idxs = indices.getStorage.asInstanceOf[IntLongDenseVectorStorage].getValues
          idxs.foreach { i =>
            if (!storage.hasKey(i)) {
              storage.set(i, random.nextDouble() * stddev + mean)
            }
          }
        case storage: IntFloatSparseVectorStorage =>
          val idxs = indices.getStorage.asInstanceOf[IntIntDenseVectorStorage].getValues
          idxs.foreach { i =>
            if (!storage.hasKey(i)) {
              storage.set(i, (random.nextDouble() * stddev + mean).toFloat)
            }
          }
        case storage: LongFloatSparseVectorStorage =>
          val idxs = indices.getStorage.asInstanceOf[IntLongDenseVectorStorage].getValues
          idxs.foreach { i =>
            if (!storage.hasKey(i)) {
              storage.set(i, (random.nextDouble() * stddev + mean).toFloat)
            }
          }
        case _ =>
      }
    }

    if (vector == null) {
      vector = storage.getRow(0)
    }
  }

  protected override def doPush(grad: Matrix, alpha: Double): Unit = {
    if (numSlot == 0) {
      storage.getRow(0).isub(grad.getRow(0).imul(alpha))
    } else {
      storage.getRow(numSlot).iadd(grad.getRow(0))
    }
  }

  protected override def doRelease[T](envCtx: EnvContext[T]): Unit = {
    assert(envCtx == null || envCtx.client == null)
    storage = rowType match {
      case RowType.T_DOUBLE_DENSE =>
        MFactory.rbIntDoubleMatrix(numSlot + 1, length.toInt, StorageType.DENSE)
      case RowType.T_DOUBLE_SPARSE =>
        MFactory.rbIntDoubleMatrix(numSlot + 1, length.toInt, StorageType.SPARSE)
      case RowType.T_DOUBLE_SPARSE_LONGKEY =>
        MFactory.rbLongDoubleMatrix(numSlot + 1, length, StorageType.SPARSE)
      case RowType.T_FLOAT_DENSE =>
        MFactory.rbIntFloatMatrix(numSlot + 1, length.toInt, StorageType.DENSE)
      case RowType.T_FLOAT_SPARSE =>
        MFactory.rbIntFloatMatrix(numSlot + 1, length.toInt, StorageType.SPARSE)
      case RowType.T_FLOAT_SPARSE_LONGKEY =>
        MFactory.rbLongFloatMatrix(numSlot + 1, length, StorageType.SPARSE)
      case _ => throw ValueNotAllowed("Value Not Allowed, Only Float/Double Are Allowed!")
    }
  }
}
