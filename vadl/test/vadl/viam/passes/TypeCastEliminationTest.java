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

package vadl.viam.passes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vadl.utils.GraphUtils.getSingleNode;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.AbstractTest;
import vadl.TestFrontend;
import vadl.TestUtils;
import vadl.types.BuiltInTable;
import vadl.utils.ViamUtils;
import vadl.viam.Definition;
import vadl.viam.Function;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.ZeroExtendNode;
import vadl.viam.passes.verification.ViamVerifier;

public class TypeCastEliminationTest extends AbstractTest {

  private static TestFrontend validFrontend;

  @BeforeAll
  static void runValidSpecAndEliminationPass() throws IOException {
    validFrontend =
        AbstractTest.runViamSpecificationWithNewFrontend(
            "passes/typeCastElimination/valid_type_cast_elimination.vadl");
    ViamVerifier.verifyAllIn(validFrontend.getViam());
  }


  @ParameterizedTest
  @MethodSource("testTrivial_Source")
  void testTrivial_shouldDeleteTypeCastAndMakeArgumentTypeSameAsResultType(String functionName) {
    var behavior = getTestBehavior(functionName);
    assertEquals(3, behavior.getNodes().count());
    var returnNode = getSingleNode(behavior, ReturnNode.class);
    var paramNode = getSingleNode(behavior, FuncParamNode.class);
    assertEquals(returnNode.returnType(), paramNode.type());
  }

  static Stream<Arguments> testTrivial_Source() {
    return findFuncNameArgumentsByPrefix("Trivial_", validFrontend.getViam());
  }

  @ParameterizedTest
  @MethodSource("testTruncate_Source")
  void testTruncate_shouldUseTruncateNodeInsteadOfTypeCast(String functionName) {
    var behavior = getTestBehavior(functionName);
    assertEquals(4, behavior.getNodes().count());
    assertEquals(1, behavior.getNodes(TruncateNode.class).count());
  }

  static Stream<Arguments> testTruncate_Source() {
    return findFuncNameArgumentsByPrefix("Truncate_", validFrontend.getViam());
  }

  @ParameterizedTest
  @MethodSource("testZeroExtend_Source")
  void testZeroExtend_shouldUseZeroExtendNodeInsteadOfTypeCast(String functionName) {
    var behavior = getTestBehavior(functionName);
    assertEquals(4, behavior.getNodes().count());
    var paramNode = getSingleNode(behavior, FuncParamNode.class);
    var zeroExtendNode = getSingleNode(behavior, ZeroExtendNode.class);
    assertEquals(paramNode, zeroExtendNode.value());
  }

  static Stream<Arguments> testZeroExtend_Source() {
    return findFuncNameArgumentsByPrefix("ZeroExtend_", validFrontend.getViam());
  }


  @ParameterizedTest
  @MethodSource("testSignedExtend_Source")
  void testSignExtend_shouldUseSignExtendNodeInsteadOfTypeCast(String functionName) {
    var behavior = getTestBehavior(functionName);
    assertEquals(4, behavior.getNodes().count());
    var paramNode = getSingleNode(behavior, FuncParamNode.class);
    var signExtendNode = getSingleNode(behavior, SignExtendNode.class);
    assertEquals(paramNode, signExtendNode.value());
  }

  static Stream<Arguments> testSignedExtend_Source() {
    return findFuncNameArgumentsByPrefix("SignExtend_", validFrontend.getViam());
  }

  @ParameterizedTest
  @MethodSource("testBoolCast_Source")
  void testBoolCast_shouldReplaceByNegCallWithSecondArg0Const(String functionName) {
    var behavior = getTestBehavior(functionName);
    assertEquals(5, behavior.getNodes().count());
    var paramNode = getSingleNode(behavior, FuncParamNode.class);
    var compareNode = getSingleNode(behavior, BuiltInCall.class);
    var zeroConstant = getSingleNode(behavior, ConstantNode.class);
    assertEquals(BuiltInTable.NEQ, compareNode.builtIn());
    assertEquals(0, zeroConstant.constant().asVal().intValue());
    assertEquals(paramNode, compareNode.arguments().get(0));
    assertEquals(zeroConstant, compareNode.arguments().get(1));
    var returnNode = getSingleNode(behavior, ReturnNode.class);
    assertEquals(compareNode, returnNode.value());
  }

  static Stream<Arguments> testBoolCast_Source() {
    return findFuncNameArgumentsByPrefix("BoolCast_", validFrontend.getViam());
  }

  @ParameterizedTest
  @EmptySource
  void testNonOptimalExample(List<Object> sources) {
    var behavior = getTestBehavior("NonOptimalExample");
    assertEquals(5, behavior.getNodes().count());
    var truncateNode = getSingleNode(behavior, TruncateNode.class);
    var signExtendNode = getSingleNode(behavior, SignExtendNode.class);
    var paramNode = getSingleNode(behavior, FuncParamNode.class);
    assertEquals(paramNode, signExtendNode.value());
    assertEquals(signExtendNode, truncateNode.value());
  }

  private Graph getTestBehavior(String testFunction) {
    var spec = validFrontend.getViam();
    var simpleTypCast = TestUtils.findDefinitionByNameIn(testFunction, spec, Function.class);
    ViamVerifier.verifyAllIn(simpleTypCast);
    var behavior = simpleTypCast.behavior();
    Assertions.assertEquals(1, behavior.getNodes(StartNode.class).count());
    Assertions.assertEquals(1, behavior.getNodes(ReturnNode.class).count());
    return behavior;
  }

  private static Stream<Arguments> findFuncNameArgumentsByPrefix(String prefix,
                                                                 Specification spec) {
    return
        ViamUtils.findDefinitionsByFilter(spec,
                (def) -> def.identifier.name().startsWith(prefix)
                    && def instanceof Function)
            .stream()
            .map(Definition::simpleName)
            .map(Arguments::of);
  }

}
