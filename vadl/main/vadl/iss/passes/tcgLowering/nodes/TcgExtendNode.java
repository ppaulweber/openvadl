// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import java.util.function.Function;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.tcgLowering.TcgExtend;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.Node;

/**
 * Represents a unary operation node
 * that extends a value from a smaller bit-width to a larger bit-width.
 * This class handles both sign- and zero-extension operations.
 */
public class TcgExtendNode extends TcgUnaryOpNode {

  @DataValue
  int fromSize;

  @DataValue
  TcgExtend extend;

  /**
   * Initializes a TcgExtendNode, representing an operation that extends a value from a
   * smaller bit-width to a larger bit-width, handling both sign- and zero-extension.
   *
   * @param fromSize The original size (bit-width) of the value.
   * @param extend   The type of extension (sign or zero) to be applied.
   * @param res      The result variable of the operation.
   * @param arg      The argument variable to be extended.
   */
  public TcgExtendNode(int fromSize, TcgExtend extend, TcgVRefNode res, TcgVRefNode arg) {
    super(res, arg);
    this.fromSize = fromSize;
    this.extend = extend;
  }

  @Override
  public Node copy() {
    return new TcgExtendNode(fromSize, extend, firstDest().copy(TcgVRefNode.class),
        arg.copy(TcgVRefNode.class));
  }

  @Override
  public Node shallowCopy() {
    return new TcgExtendNode(fromSize, extend, firstDest(), arg);
  }

  @Override
  public String tcgFunctionName() {
    var postfix = extend == TcgExtend.SIGN ? "s" : "u";
    return "gen_ext" + postfix;
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return tcgFunctionName() + "(" + firstDest().varName() + ", " + arg.varName() + ", "
        + fromSize + ");";
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(fromSize);
    collection.add(extend);
  }
}
