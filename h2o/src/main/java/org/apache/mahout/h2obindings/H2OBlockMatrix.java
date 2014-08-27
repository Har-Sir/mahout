/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.mahout.h2obindings;

import org.apache.mahout.math.Matrix;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.AbstractMatrix;
import org.apache.mahout.math.DenseMatrix;
import org.apache.mahout.math.SparseMatrix;

import water.fvec.Chunk;

/*
 * A Matrix implementation to represent a vertical Block of DRM.
 *
 * Creation of the matrix is an O(1) operation with negligible
 * overhead, and will remain so as long as the matrix is only
 * read from (no modifications).
 *
 * On the first modification, create a copy on write Matrix and
 * all further operations happen on this cow matrix.
 *
 * The benefit is, mapBlock() closures which never modify the
 * input matrix save on the copy overhead.
 */
public class H2OBlockMatrix extends AbstractMatrix {
  Chunk _chks[];
  Matrix cow; /* Copy on Write */

  public H2OBlockMatrix(Chunk chks[]) {
    super(chks[0].len(), chks.length);
    _chks = chks;
  }

  private void cow() {
    if (cow != null) {
      return;
    }

    if (_chks[0].isSparse()) {
      cow = new SparseMatrix(_chks[0].len(), _chks.length);
    } else {
      cow = new DenseMatrix(_chks[0].len(), _chks.length);
    }

    for (int c = 0; c < _chks.length; c++) {
      for (int r = 0; r < _chks[0].len(); r++) {
        cow.setQuick(r, c, _chks[c].at0(r));
      }
    }
  }

  public void setQuick(int row, int col, double val) {
    cow();
    cow.setQuick(row, col, val);
  }

  public Matrix like(int nrow, int ncol) {
    if (_chks[0].isSparse()) {
      return new SparseMatrix(nrow, ncol);
    } else {
      return new DenseMatrix(nrow, ncol);
    }
  }

  public Matrix like() {
    if (_chks[0].isSparse()) {
      return new SparseMatrix(rowSize(), columnSize());
    } else {
      return new DenseMatrix(rowSize(), columnSize());
    }
  }

  public double getQuick(int row, int col) {
    if (cow != null) {
      return cow.getQuick(row, col);
    } else {
      return _chks[col].at0(row);
    }
  }

  public Matrix assignRow(int row, Vector v) {
    cow();
    cow.assignRow(row, v);
    return cow;
  }

  public Matrix assignColumn(int col, Vector v) {
    cow();
    cow.assignColumn(col, v);
    return cow;
  }
}
