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

package vadl.lcb.passes.llvmLowering.strategies;

import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Stream;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.gcb.passes.IdentifyFieldUsagePass;
import vadl.gcb.passes.IsaMachineInstructionMatchingPass;
import vadl.gcb.passes.pseudo.PseudoFuncParamNode;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.domain.RegisterRef;
import vadl.lcb.passes.llvmLowering.tablegen.model.ReferencesFormatField;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.utils.Pair;
import vadl.viam.CompilerInstruction;
import vadl.viam.Parameter;
import vadl.viam.graph.Graph;
import vadl.viam.graph.HasRegisterFile;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.WriteRegFileNode;

/**
 * Abstract class to lower {@link CompilerInstruction} to {@link LlvmLoweringRecord.Compiler}.
 */
public abstract class LlvmCompilerInstructionLowerStrategy {
  protected final List<LlvmInstructionLoweringStrategy> strategies;

  protected LlvmCompilerInstructionLowerStrategy(
      List<LlvmInstructionLoweringStrategy> strategies) {
    this.strategies = strategies;
  }

  /**
   * Lower an instruction.
   */
  public Optional<LlvmLoweringRecord.Compiler> lowerInstruction(
      CompilerInstruction compilerInstruction,
      IsaMachineInstructionMatchingPass.Result supportedInstructions
  ) {
    var uses = new ArrayList<RegisterRef>();
    var defs = new ArrayList<RegisterRef>();
    var inputOperands = new ArrayList<TableGenInstructionOperand>();
    var outputOperands = new ArrayList<TableGenInstructionOperand>();

    var isTerminator = false;
    var isReturn = false;
    var mayLoad = false;
    var mayStore = false;
    var isBranch = false;

    if (compilerInstruction.behavior().getNodes(InstrCallNode.class).toList().size() > 1) {
      DeferredDiagnosticStore.add(
          Diagnostic.warning(
              "Cannot generate instruction selectors for pseudo instruction with multiple "
                  + "machine instructions",
              compilerInstruction.sourceLocation()).build());
    }


    for (var callNode : compilerInstruction.behavior().getNodes(InstrCallNode.class).toList()) {
      var instructionBehavior = callNode.target().behavior().copy();

      replaceNodesInBehavior(instructionBehavior, callNode);

      var label = supportedInstructions.reverse().get(callNode.target());

      // Skip not supported instructions
      if (label == null) {
        continue;
      }

      for (var strategy : strategies) {
        if (!strategy.isApplicable(label)) {
          continue;
        }

        var baseInstructionInfo = strategy.lowerBaseInfo(instructionBehavior);

        var flags = baseInstructionInfo.flags();
        isTerminator |= flags.isTerminator();
        isReturn |= flags.isReturn();
        mayLoad |= flags.mayLoad();
        mayStore |= flags.mayStore();
        isBranch |= flags.isBranch();
        defs.addAll(baseInstructionInfo.defs());
        uses.addAll(baseInstructionInfo.uses());
        inputOperands.addAll(baseInstructionInfo.inputs());
        outputOperands.addAll(baseInstructionInfo.outputs());

        break;
      }
    }

    var flags = new LlvmLoweringPass.Flags(isTerminator,
        isBranch,
        false,
        isReturn,
        true,
        true,
        mayLoad,
        mayStore,
        false,
        false,
        false);

    var info = new LlvmLoweringPass.BaseInstructionInfo(
        dedup(inputOperands),
        dedup(outputOperands),
        flags,
        dedup(uses),
        dedup(defs)
    );

    return Optional.of(new LlvmLoweringRecord.Compiler(
        info
    ));
  }

  /**
   * There are two relevant cases.
   * The first is that the {@code argument} is a constant. Then, we do not have to do anything.
   * The second case is when {@link CompilerInstruction} uses an {@code index}. Then, the argument
   * is replaced by a {@link FuncParamNode}. However, we still require to know the index for
   * the pseudo instance expansion. That's why we extend {@link FuncParamNode} with
   * {@link PseudoFuncParamNode} which has an {@code index} property.
   * Here is an example of the index. Note that {@code rs} will be transformed into
   * a {@link PseudoFuncParamNode} when it is replaced.
   * <code>
   * pseudo instruction BGEZ( rs : Index, offset : Bits<12> ) =
   * {
   * BGE{ rs1 = rs, rs2 = 0 as Bits5, imm = offset }
   * }
   * </code>
   */
  protected ExpressionNode indexArgument(List<ExpressionNode> arguments, ExpressionNode argument) {
    if (argument instanceof FuncParamNode funcParamNode) {
      int index = arguments.indexOf(argument);
      return new PseudoFuncParamNode(funcParamNode.parameter(), index);
    }
    return argument;
  }

  /**
   * Replace the arguments in the behavior of {@code copiedInstructionBehavior}.
   */
  public void replaceNodesInBehavior(Graph copiedInstructionBehavior,
                                     InstrCallNode callNode) {
    Streams.zip(callNode.getParamFields().stream(), callNode.arguments().stream(),
            Pair::new)
        .forEach(app -> {
          var formatField = app.left();
          var argument = indexArgument(callNode.arguments(), app.right());

          Stream.concat(
                  copiedInstructionBehavior.getNodes(FieldRefNode.class),
                  copiedInstructionBehavior.getNodes(FieldAccessRefNode.class)
              )
              .filter(x -> {
                if (x instanceof FieldRefNode fieldRefNode) {
                  return fieldRefNode.formatField().equals(formatField);
                } else if (x instanceof FieldAccessRefNode fieldAccessRefNode) {
                  return fieldAccessRefNode.fieldAccess().fieldRef().equals(formatField);
                }
                return false;
              })
              .forEach(occurrence -> {
                // Edge case:
                // When we have the following pseudo instruction. Note that "r1" is replaced
                // by a constant. Sometimes, we need to create instruction selectors in TableGen,
                // and it requires a variable. However, if we replace the field by a constant
                // we lose the name of the variable because we have no field anymore.
                // {
                //     JALR{ rs1 = 1 as Bits5, rd = 0 as Bits5, imm = 0 as Bits12 }
                // }

                if (argument instanceof ConstantNode constantNode) {
                  // The constantNode tells me that it will be used as a register index.

                  // Go over the usages to emit warnings.
                  // We need the usage because we need to find out what the register file
                  // to check for constraints.
                  occurrence.usages().filter(node -> (node instanceof HasRegisterFile))
                      .forEach(node -> {
                        var cast = (HasRegisterFile) node;

                        var constraintValue =
                            Arrays.stream(cast.registerFile().constraints()).filter(
                                c -> c.address().intValue()
                                    == constantNode.constant().asVal().intValue()).findFirst();

                        if (constraintValue.isEmpty()) {
                          DeferredDiagnosticStore.add(Diagnostic.warning(
                              "There is no constraint value for this register. "
                                  +
                                  "Therefore, we cannot generate instruction selectors for it.",
                              occurrence.sourceLocation()).build());
                        }
                      });

                  occurrence.replaceAndDelete(argument.copy());

                  // After the replacement, we can check whether we have a write node with
                  // constant node as address which has a constraint. If that's the case, then we
                  // can remove the side effect.
                  occurrence.usages()
                      .filter(node -> node instanceof WriteRegFileNode writeRegFileNode
                          && writeRegFileNode.hasConstantAddress()
                          // Check if there is a constraint for this register index.
                          && Arrays.stream(writeRegFileNode.registerFile().constraints())
                          .anyMatch(constraint -> constraint.address().intValue()
                              == constantNode.constant().asVal().intValue()))
                      .forEach(Node::safeDelete);
                } else {
                  occurrence.replaceAndDelete(argument.copy());
                }
              });
        });
  }

  private <T> List<T> dedup(
      List<T> x) {
    return new ArrayList<>(new LinkedHashSet<>(x));
  }
}
