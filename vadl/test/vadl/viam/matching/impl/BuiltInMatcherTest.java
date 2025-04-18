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

package vadl.viam.matching.impl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;

class BuiltInMatcherTest {
  @Test
  void matches_shouldReturnTrue_whenInputsMatchExactly() {
    var matcher = new BuiltInMatcher(BuiltInTable.ADD, List.of(
        new AnyConstantValueMatcher(),
        new AnyConstantValueMatcher()
    ));
    var input1 = new ConstantNode(Constant.Value.of(1, DataType.unsignedInt(32)));
    var input2 = new ConstantNode(Constant.Value.of(1, DataType.unsignedInt(32)));
    var operation = new BuiltInCall(BuiltInTable.ADD, new NodeList<>(
        input1, input2
    ), Type.unsignedInt(32));

    // When
    var result = matcher.matches(operation);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void matches_shouldReturnTrue_whenInputsNotMatchExactly() {
    // Here we specify only one matcher,
    // even though the node has two inputs.
    var matcher = new BuiltInMatcher(BuiltInTable.ADD, List.of(
        new AnyConstantValueMatcher()
    ));
    var input1 = new ConstantNode(Constant.Value.of(1, DataType.unsignedInt(32)));
    var input2 = new ConstantNode(Constant.Value.of(1, DataType.unsignedInt(32)));
    var operation = new BuiltInCall(BuiltInTable.ADD, new NodeList<>(
        input1, input2
    ), Type.unsignedInt(32));

    // When
    var result = matcher.matches(operation);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void matches_shouldReturnTrue_whenNoMatchers() {
    // Here we specify no matchers,
    // even though the node has two inputs.
    var matcher = new BuiltInMatcher(BuiltInTable.ADD, Collections.emptyList());
    var input1 = new ConstantNode(Constant.Value.of(1, DataType.unsignedInt(32)));
    var input2 = new ConstantNode(Constant.Value.of(1, DataType.unsignedInt(32)));
    var operation = new BuiltInCall(BuiltInTable.ADD, new NodeList<>(
        input1, input2
    ), Type.unsignedInt(32));

    // When
    var result = matcher.matches(operation);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void matches_shouldReturnFalse_whenBuiltInDoesNotMatch() {
    var matcher = new BuiltInMatcher(BuiltInTable.SUB, Collections.emptyList());
    var input1 = new ConstantNode(Constant.Value.of(1, DataType.unsignedInt(32)));
    var input2 = new ConstantNode(Constant.Value.of(1, DataType.unsignedInt(32)));
    var operation = new BuiltInCall(BuiltInTable.ADD, new NodeList<>(
        input1, input2
    ), Type.unsignedInt(32));

    // When
    var result = matcher.matches(operation);

    // Then
    assertThat(result).isFalse();
  }
}