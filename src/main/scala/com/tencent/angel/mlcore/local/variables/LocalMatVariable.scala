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
import com.tencent.angel.ml.math2.vector._
import com.tencent.angel.ml.math2.{MFactory, StorageType}
import com.tencent.angel.mlcore.conf.SharedConf
import com.tencent.angel.mlcore.network.EnvContext
import com.tencent.angel.mlcore.utils.{OptUtils, ValueNotAllowed}
import com.tencent.angel.mlcore.variable.{MatVariable, Updater, VariableManager}


private[mlcore] class LocalMatVariable(name: String,
                                       val numRows: Int,
                                       val numCols: Long,
                                       updater: Updater,
                                       rowType: RowType,
                                       formatClassName: String,
                                       allowPullWithIndex: Boolean)
                                      (implicit conf: SharedConf, variableManager: VariableManager)
  extends LocalVariable(name, rowType, updater, formatClassName, allowPullWithIndex) with MatVariable {
  override protected var matrix: Matrix = _

  protected override def doCreate[T](envCtx: EnvContext[T]): Unit = {
    assert(envCtx == null || envCtx.client == null)
    storage = rowType match {
      case RowType.T_DOUBLE_DENSE =>
        MFactory.rbIntDoubleMatrix((numSlot + 1) * numRows, numCols.toInt, StorageType.DENSE)
      case RowType.T_DOUBLE_SPARSE =>
        MFactory.rbIntDoubleMatrix((numSlot + 1) * numRows, numCols.toInt, StorageType.SPARSE)
      case RowType.T_DOUBLE_SPARSE_LONGKEY =>
        MFactory.rbLongDoubleMatrix((numSlot + 1) * numRows, numCols, StorageType.SPARSE)
      case RowType.T_FLOAT_DENSE =>
        MFactory.rbIntFloatMatrix((numSlot + 1) * numRows, numCols.toInt, StorageType.DENSE)
      case RowType.T_FLOAT_SPARSE =>
        MFactory.rbIntFloatMatrix((numSlot + 1) * numRows, numCols.toInt, StorageType.SPARSE)
      case RowType.T_FLOAT_SPARSE_LONGKEY =>
        MFactory.rbLongFloatMatrix((numSlot + 1) * numRows, numCols, StorageType.SPARSE)
      case _ => throw ValueNotAllowed("Value Not Allowed, Only Float/Double Are Allowed!")
    }
  }

  protected override def doInit(taskFlag: Int): Unit = {
    if (taskFlag == 0 && rowType.isDense) {
      val random = new Random()
      (0 until numRows).foreach { rId =>
        storage.getRow(rId).getStorage match {
          case storage: IntDoubleDenseVectorStorage =>
            val values = storage.getValues
            values.indices.foreach { idx =>
              values(idx) = random.nextDouble() * stddev + mean
            }
          case storage: IntFloatDenseVectorStorage =>
            val values = storage.getValues
            values.indices.foreach { idx =>
              values(idx) = (random.nextDouble() * stddev + mean).toFloat
            }
          case _ =>
        }
      }
    }
  }

  protected override def doPull(epoch: Int, indices: Vector = null): Unit = {
    if (matrix == null) {
      matrix = rowType match {
        case RowType.T_DOUBLE_DENSE =>
          val rows = (0 until numRows).toArray.map { rId => storage.getRow(rId).asInstanceOf[IntDoubleVector] }
          MFactory.rbIntDoubleMatrix(rows)
        case RowType.T_DOUBLE_SPARSE =>
          val rows = (0 until numRows).toArray.map { rId => storage.getRow(rId).asInstanceOf[IntDoubleVector] }
          MFactory.rbIntDoubleMatrix(rows)
        case RowType.T_DOUBLE_SPARSE_LONGKEY =>
          val rows = (0 until numRows).toArray.map { rId => storage.getRow(rId).asInstanceOf[LongDoubleVector] }
          MFactory.rbLongDoubleMatrix(rows)
        case RowType.T_FLOAT_DENSE =>
          val rows = (0 until numRows).toArray.map { rId => storage.getRow(rId).asInstanceOf[IntFloatVector] }
          MFactory.rbIntFloatMatrix(rows)
        case RowType.T_FLOAT_SPARSE =>
          val rows = (0 until numRows).toArray.map { rId => storage.getRow(rId).asInstanceOf[IntFloatVector] }
          MFactory.rbIntFloatMatrix(rows)
        case RowType.T_FLOAT_SPARSE_LONGKEY =>
          val rows = (0 until numRows).toArray.map { rId => storage.getRow(rId).asInstanceOf[LongFloatVector] }
          MFactory.rbLongFloatMatrix(rows)
        case _ => throw ValueNotAllowed("Value Not Allowed, Only Float/Double Are Allowed!")
      }
    }
  }

  protected override def doPush(grad: Matrix, alpha: Double): Unit = {
    if (numSlot == 0) {
      OptUtils.getRowsAsMatrix(storage, 0, numRows).isub(grad.imul(alpha))
    } else {
      OptUtils.getRowsAsMatrix(storage, numRows * numSlot, numRows * (numSlot + 1)).iadd(grad)
    }
  }

  protected def doRelease[T](envCtx: EnvContext[T]): Unit = {
    assert(envCtx == null || envCtx.client == null)
    storage = rowType match {
      case RowType.T_DOUBLE_DENSE =>
        MFactory.rbIntDoubleMatrix((numSlot + 1) * numRows, numCols.toInt, StorageType.DENSE)
      case RowType.T_DOUBLE_SPARSE =>
        MFactory.rbIntDoubleMatrix((numSlot + 1) * numRows, numCols.toInt, StorageType.SPARSE)
      case RowType.T_DOUBLE_SPARSE_LONGKEY =>
        MFactory.rbLongDoubleMatrix((numSlot + 1) * numRows, numCols, StorageType.SPARSE)
      case RowType.T_FLOAT_DENSE =>
        MFactory.rbIntFloatMatrix((numSlot + 1) * numRows, numCols.toInt, StorageType.DENSE)
      case RowType.T_FLOAT_SPARSE =>
        MFactory.rbIntFloatMatrix((numSlot + 1) * numRows, numCols.toInt, StorageType.SPARSE)
      case RowType.T_FLOAT_SPARSE_LONGKEY =>
        MFactory.rbLongFloatMatrix((numSlot + 1) * numRows, numCols, StorageType.SPARSE)
      case _ => throw ValueNotAllowed("Value Not Allowed, Only Float/Double Are Allowed!")
    }
  }
}
