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

package vadl.ast;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * A pass over the AST that produces a textual representation of the AST.
 */
public class AstDumper
    implements DefinitionVisitor<Void>, ExprVisitor<Void>, StatementVisitor<Void> {
  private StringBuilder builder = new StringBuilder();
  private int indent;

  /**
   * Dumps the AST into a textual representation.
   *
   * @param ast to dump.
   * @return a textual representation of the tree.
   */
  public String dump(Ast ast) {
    builder = new StringBuilder();
    indent = 0;

    for (var definition : ast.definitions) {
      definition.accept(this);
    }
    return builder.toString();
  }

  private String indentString() {
    var indentBy = 2;
    var indentCharacters = ". : ' | ";
    var indentLength = indent * indentBy;
    return indentCharacters.repeat(indentLength / indentCharacters.length())
        + indentCharacters.substring(0, indentLength % indentCharacters.length());
  }

  private void dumpNode(Node node) {
    builder.append(indentString());
    builder.append(node.toString());
    builder.append('\n');
  }

  private void dumpChildren(List<? extends Node> children) {
    indent++;
    for (var child : children) {
      if (child instanceof Definition def) {
        def.accept(this);
      } else if (child instanceof Expr expr) {
        expr.accept(this);
      } else if (child instanceof Statement statement) {
        statement.accept(this);
      } else if (child == null) {
        builder.append(indentString()).append("null");
      } else {
        throw new RuntimeException("NOT IMPLEMENTED");
      }
    }
    indent--;
  }

  private void dumpChildren(Node... children) {
    dumpChildren(Arrays.asList(children));
  }

  @Override
  public Void visit(ConstantDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier());
    if (definition.typeLiteral != null) {
      dumpChildren(definition.typeLiteral);
    }
    dumpChildren(definition.value);
    return null;
  }

  @Override
  public Void visit(FormatDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier(), definition.typeLiteral);
    this.indent++;
    for (var field : definition.fields) {
      if (field instanceof FormatDefinition.RangeFormatField f) {
        dumpNode(f);
        dumpChildren(f.identifier);
        dumpChildren(f.ranges);
      } else if (field instanceof FormatDefinition.TypedFormatField f) {
        dumpNode(f);
        dumpChildren(f.identifier, f.typeLiteral);
      } else if (field instanceof FormatDefinition.DerivedFormatField f) {
        dumpNode(f);
        dumpChildren(f.identifier, f.expr);
      }
    }
    for (var auxField : definition.auxiliaryFields) {
      for (FormatDefinition.AuxiliaryFieldEntry entry : auxField.entries()) {
        dumpChildren(entry.id);
        dumpChildren(entry.expr);
      }
    }
    this.indent--;
    return null;
  }

  @Override
  public Void visit(InstructionSetDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier);
    if (definition.extending != null) {
      dumpChildren(definition.extending);
    }
    dumpChildren(definition.definitions);
    return null;
  }

  @Override
  public Void visit(CounterDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier(), definition.typeLiteral);
    return null;
  }

  @Override
  public Void visit(MemoryDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier(), definition.addressTypeLiteral,
        definition.dataTypeLiteral);
    return null;
  }

  @Override
  public Void visit(RegisterDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier(), definition.typeLiteral);
    return null;
  }

  @Override
  public Void visit(RegisterFileDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier());
    dumpChildren(definition.typeLiteral.argTypes());
    dumpChildren(definition.typeLiteral.resultType());
    return null;
  }

  @Override
  public Void visit(Identifier expr) {
    dumpNode(expr);
    return null;
  }

  @Override
  public Void visit(BinaryExpr expr) {
    dumpNode(expr);
    dumpChildren(expr.left, expr.right);
    return null;
  }

  @Override
  public Void visit(GroupedExpr expr) {
    dumpNode(expr);
    dumpChildren(expr.expressions);
    return null;
  }

  @Override
  public Void visit(IntegerLiteral expr) {
    dumpNode(expr);
    return null;
  }

  @Override
  public Void visit(BinaryLiteral expr) {
    dumpNode(expr);
    return null;
  }

  @Override
  public Void visit(BoolLiteral expr) {
    dumpNode(expr);
    return null;
  }

  @Override
  public Void visit(StringLiteral expr) {
    dumpNode(expr);
    return null;
  }

  @Override
  public Void visit(PlaceholderExpr expr) {
    dumpNode(expr);
    return null;
  }

  @Override
  public Void visit(MacroInstanceExpr expr) {
    dumpNode(expr);
    return null;
  }

  @Override
  public Void visit(RangeExpr expr) {
    dumpNode(expr);
    dumpChildren(expr.from, expr.to);
    return null;
  }

  @Override
  public Void visit(TypeLiteral expr) {
    dumpNode(expr);
    dumpChildren((Expr) expr.baseType);
    indent++;
    for (List<Expr> sizes : expr.sizeIndices) {
      builder.append(indentString()).append("Sizes\n");
      dumpChildren(sizes);
    }
    indent--;
    return null;
  }

  @Override
  public Void visit(IdentifierPath expr) {
    dumpNode(expr);
    dumpChildren(expr.segments.stream().map(Node.class::cast).toList());
    return null;
  }

  @Override
  public Void visit(UnaryExpr expr) {
    dumpNode(expr);
    dumpChildren(expr.operand);
    return null;
  }

  @Override
  public Void visit(InstructionDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier(), definition.typeIdentifier(), definition.behavior);
    return null;
  }

  @Override
  public Void visit(PseudoInstructionDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier());
    dumpChildren(definition.params.stream()
        .flatMap(param -> Stream.of(param.identifier(), param.typeLiteral)).toList());
    dumpChildren(definition.statements);
    return null;
  }

  @Override
  public Void visit(RelocationDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier, definition.resultTypeLiteral, definition.expr);
    return null;
  }

  @Override
  public Void visit(EncodingDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier());
    dumpChildren(definition.encodings.items.stream()
        .map(EncodingDefinition.EncodingField.class::cast)
        .flatMap(entry -> Stream.of(entry.field, (Node) entry.value))
        .toList()
    );
    return null;
  }

  @Override
  public Void visit(AssemblyDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.expr);
    return null;
  }

  @Override
  public Void visit(UsingDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier(), definition.typeLiteral);
    return null;
  }

  @Override
  public Void visit(AbiPseudoInstructionDefinition definition) {
    dumpNode(definition);
    return null;
  }

  @Override
  public Void visit(FunctionDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier());
    for (var param : definition.params) {
      dumpChildren(param.identifier(), param.typeLiteral);
    }
    dumpChildren(definition.retType, definition.expr);
    return null;
  }

  @Override
  public Void visit(AliasDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier());
    if (definition.aliasType != null) {
      dumpChildren(definition.aliasType);
    }
    if (definition.targetType != null) {
      dumpChildren(definition.targetType);
    }
    dumpChildren(definition.value);
    return null;
  }

  @Override
  public Void visit(EnumerationDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier());
    if (definition.enumType != null) {
      dumpChildren(definition.enumType);
    }
    for (var entry : definition.entries) {
      dumpChildren(entry.name);
      if (entry.value != null) {
        dumpChildren(entry.value);
      }
    }
    return null;
  }

  @Override
  public Void visit(ExceptionDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier());
    dumpChildren(definition.statement);
    return null;
  }

  @Override
  public Void visit(PlaceholderDefinition definition) {
    dumpNode(definition);
    return null;
  }

  @Override
  public Void visit(MacroInstanceDefinition definition) {
    dumpNode(definition);
    return null;
  }

  @Override
  public Void visit(MacroMatchDefinition definition) {
    dumpNode(definition);
    return null;
  }

  @Override
  public Void visit(DefinitionList definition) {
    dumpNode(definition);
    dumpChildren(definition.items);
    return null;
  }

  @Override
  public Void visit(ModelDefinition definition) {
    dumpNode(definition);
    return null;
  }

  @Override
  public Void visit(RecordTypeDefinition definition) {
    dumpNode(definition);
    return null;
  }

  @Override
  public Void visit(ModelTypeDefinition definition) {
    dumpNode(definition);
    return null;
  }

  @Override
  public Void visit(ImportDefinition importDefinition) {
    dumpNode(importDefinition);
    indent++;
    builder.append(indentString()).append("File\n");
    if (importDefinition.fileId != null) {
      dumpChildren(importDefinition.fileId);
    }
    if (importDefinition.filePath != null) {
      dumpChildren(importDefinition.filePath);
    }
    for (List<Identifier> importPath : importDefinition.importedSymbols) {
      builder.append(indentString()).append("Import\n");
      indent++;
      dumpChildren(importPath);
      indent--;
    }
    if (!importDefinition.args.isEmpty()) {
      builder.append(indentString()).append("Args\n");
      indent++;
      dumpChildren(importDefinition.args);
      indent--;
    }
    builder.append(indentString()).append("Module AST\n");
    dumpChildren(importDefinition.moduleAst.definitions);
    indent--;
    return null;
  }

  @Override
  public Void visit(ProcessDefinition processDefinition) {
    dumpNode(processDefinition);
    dumpChildren(processDefinition.identifier());
    for (var templateParam : processDefinition.templateParams) {
      dumpChildren(templateParam.identifier(), templateParam.type);
      if (templateParam.value != null) {
        dumpChildren(templateParam.value);
      }
    }
    for (var input : processDefinition.inputs) {
      dumpChildren(input.identifier(), input.typeLiteral);
    }
    for (var output : processDefinition.outputs) {
      dumpChildren(output.identifier(), output.typeLiteral);
    }
    return null;
  }

  @Override
  public Void visit(OperationDefinition operationDefinition) {
    dumpNode(operationDefinition);
    dumpChildren(operationDefinition.identifier());
    for (IsId resource : operationDefinition.resources) {
      dumpChildren((Node) resource);
    }
    return null;
  }

  @Override
  public Void visit(GroupDefinition groupDefinition) {
    dumpNode(groupDefinition);
    dumpChildren(groupDefinition.identifier());
    if (groupDefinition.type != null) {
      dumpChildren(groupDefinition.type);
    }
    for (Group group : groupDefinition.groupSequence.groups) {
      builder.append(indentString()).append("Group\n");
      indent++;
      builder.append(indentString());
      group.prettyPrint(0, builder);
      indent--;
    }
    return null;
  }

  @Override
  public Void visit(ApplicationBinaryInterfaceDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.id, (Node) definition.isa);
    dumpChildren(definition.definitions);
    return null;
  }

  @Override
  public Void visit(AbiSequenceDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.statements);
    return null;
  }

  @Override
  public Void visit(SpecialPurposeRegisterDefinition definition) {
    dumpNode(definition);
    return null;
  }

  @Override
  public Void visit(MicroProcessorDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.id);
    dumpChildren(definition.implementedIsas.stream().map(Node.class::cast).toList());
    if (definition.abi != null) {
      dumpChildren((Node) definition.abi);
    }
    dumpChildren(definition.definitions);
    return null;
  }

  @Override
  public Void visit(PatchDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.generator, definition.handle);
    if (definition.reference != null) {
      dumpChildren((Node) definition.reference);
    }
    return null;
  }

  @Override
  public Void visit(SourceDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.id);
    return null;
  }

  @Override
  public Void visit(CpuFunctionDefinition definition) {
    dumpNode(definition);
    if (definition.stopWithReference != null) {
      dumpChildren((Node) definition.stopWithReference);
    }
    dumpChildren(definition.expr);
    return null;
  }

  @Override
  public Void visit(CpuProcessDefinition definition) {
    dumpNode(definition);
    for (Parameter startupOutput : definition.startupOutputs) {
      dumpChildren(startupOutput.identifier(), startupOutput.typeLiteral);
    }
    dumpChildren(definition.statement);
    return null;
  }

  @Override
  public Void visit(MicroArchitectureDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.id, (Node) definition.processor);
    dumpChildren(definition.definitions);
    return null;
  }

  @Override
  public Void visit(MacroInstructionDefinition definition) {
    dumpNode(definition);
    for (Parameter input : definition.inputs) {
      dumpChildren(input.identifier(), input.typeLiteral);
    }
    for (Parameter output : definition.outputs) {
      dumpChildren(output.identifier(), output.typeLiteral);
    }
    dumpChildren(definition.statement);
    return null;
  }

  @Override
  public Void visit(PortBehaviorDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.id);
    for (Parameter input : definition.inputs) {
      dumpChildren(input.identifier(), input.typeLiteral);
    }
    for (Parameter output : definition.outputs) {
      dumpChildren(output.identifier(), output.typeLiteral);
    }
    dumpChildren(definition.statement);
    return null;
  }

  @Override
  public Void visit(PipelineDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.id);
    for (Parameter output : definition.outputs) {
      dumpChildren(output.identifier(), output.typeLiteral);
    }
    dumpChildren(definition.statement);
    return null;
  }

  @Override
  public Void visit(StageDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.id);
    for (Parameter output : definition.outputs) {
      dumpChildren(output.identifier(), output.typeLiteral);
    }
    dumpChildren(definition.statement);
    return null;
  }

  @Override
  public Void visit(CacheDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.id, definition.sourceType, definition.targetType);
    return null;
  }

  @Override
  public Void visit(LogicDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.id);
    return null;
  }

  @Override
  public Void visit(SignalDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.id, definition.type);
    return null;
  }

  @Override
  public Void visit(AsmDescriptionDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.id, definition.abi);
    dumpChildren(definition.modifiers);
    dumpChildren(definition.directives);
    dumpChildren(definition.rules);
    dumpChildren(definition.commonDefinitions);
    return null;
  }

  @Override
  public Void visit(AsmModifierDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.stringLiteral, definition.isa, definition.relocation);
    return null;
  }

  @Override
  public Void visit(AsmDirectiveDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.stringLiteral, definition.builtinDirective);
    return null;
  }

  @Override
  public Void visit(AsmGrammarRuleDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.id);
    if (definition.asmTypeDefinition != null) {
      dumpChildren(definition.asmTypeDefinition);
    }
    dumpChildren(definition.alternatives);
    return null;
  }

  @Override
  public Void visit(AsmGrammarAlternativesDefinition definition) {
    dumpNode(definition);
    for (var alternative : definition.alternatives) {
      dumpChildren(alternative);
    }
    return null;
  }

  @Override
  public Void visit(AsmGrammarElementDefinition definition) {
    dumpNode(definition);
    if (definition.localVar != null) {
      dumpChildren(definition.localVar);
    }
    if (definition.attribute != null) {
      dumpChildren(definition.attribute);
    }
    if (definition.asmLiteral != null) {
      dumpChildren(definition.asmLiteral);
    }
    if (definition.groupAlternatives != null) {
      dumpChildren(definition.groupAlternatives);
    }
    if (definition.optionAlternatives != null) {
      dumpChildren(definition.optionAlternatives);
    }
    if (definition.repetitionAlternatives != null) {
      dumpChildren(definition.repetitionAlternatives);
    }
    if (definition.semanticPredicate != null) {
      dumpChildren(definition.semanticPredicate);
    }
    if (definition.groupAsmTypeDefinition != null) {
      dumpChildren(definition.groupAsmTypeDefinition);
    }
    return null;
  }

  @Override
  public Void visit(AsmGrammarLocalVarDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.id, definition.asmLiteral);
    return null;
  }

  @Override
  public Void visit(AsmGrammarLiteralDefinition definition) {
    dumpNode(definition);
    if (definition.id != null) {
      dumpChildren(definition.id);
    }
    if (definition.stringLiteral != null) {
      dumpChildren(definition.stringLiteral);
    }
    if (definition.asmTypeDefinition != null) {
      dumpChildren(definition.asmTypeDefinition);
    }
    dumpChildren(definition.parameters);
    return null;
  }

  @Override
  public Void visit(AsmGrammarTypeDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.id);
    return null;
  }

  @Override
  public Void visit(CallIndexExpr expr) {
    dumpNode(expr);
    dumpChildren((Expr) expr.target);
    indent++;
    for (CallIndexExpr.Arguments args : expr.argsIndices) {
      builder.append(indentString()).append("ArgsIndices\n");
      dumpChildren(args.values);
    }
    for (CallIndexExpr.SubCall subCall : expr.subCalls) {
      builder.append(indentString()).append("SubCall\n");
      dumpChildren(subCall.id);
      for (var args : subCall.argsIndices) {
        builder.append(indentString()).append("ArgsIndices\n");
        dumpChildren(args.values);
      }
    }
    indent--;
    return null;
  }

  @Override
  public Void visit(IfExpr expr) {
    dumpNode(expr);
    dumpChildren(expr.condition, expr.thenExpr, expr.elseExpr);
    return null;
  }

  @Override
  public Void visit(LetExpr expr) {
    dumpNode(expr);
    dumpChildren(expr.identifiers);
    dumpChildren(expr.valueExpr, expr.body);
    return null;
  }

  @Override
  public Void visit(CastExpr expr) {
    dumpNode(expr);
    dumpChildren(expr.value, (Expr) expr.typeLiteral);
    return null;
  }

  @Override
  public Void visit(SymbolExpr expr) {
    dumpNode(expr);
    dumpChildren((Expr) expr.path);
    if (expr.size != null) {
      dumpChildren(expr.size);
    }
    return null;
  }

  @Override
  public Void visit(MacroMatchExpr expr) {
    dumpNode(expr);
    return null;
  }

  @Override
  public Void visit(MatchExpr expr) {
    dumpNode(expr);
    dumpChildren(expr.candidate, expr.defaultResult);
    for (var matchCase : expr.cases) {
      dumpChildren(matchCase.patterns);
      dumpChildren(matchCase.result);
    }
    return null;
  }

  @Override
  public Void visit(ExtendIdExpr expr) {
    dumpNode(expr);
    return null;
  }

  @Override
  public Void visit(IdToStrExpr expr) {
    dumpNode(expr);
    return null;
  }

  @Override
  public Void visit(ExistsInExpr expr) {
    dumpNode(expr);
    for (IsId operation : expr.operations) {
      dumpChildren((Node) operation);
    }
    return null;
  }

  @Override
  public Void visit(ExistsInThenExpr expr) {
    dumpNode(expr);
    for (ExistsInThenExpr.Condition condition : expr.conditions) {
      dumpChildren((Node) condition.id());
      for (IsId operation : condition.operations()) {
        dumpChildren((Node) operation);
      }
    }
    dumpChildren(expr.thenExpr);
    return null;
  }

  @Override
  public Void visit(ForallThenExpr expr) {
    dumpNode(expr);
    for (ForallThenExpr.Index index : expr.indices) {
      dumpChildren((Node) index.identifier());
      for (IsId operation : index.operations) {
        dumpChildren((Node) operation);
      }
    }
    dumpChildren(expr.thenExpr);
    return null;
  }

  @Override
  public Void visit(ForallExpr expr) {
    dumpNode(expr);
    for (ForallExpr.Index index : expr.indices) {
      dumpChildren((Node) index.identifier(), index.domain);
    }
    dumpChildren(expr.expr);
    return null;
  }

  @Override
  public Void visit(SequenceCallExpr expr) {
    dumpNode(expr);
    dumpChildren(expr.target);
    if (expr.range != null) {
      dumpChildren(expr.range);
    }
    return null;
  }

  @Override
  public Void visit(BlockStatement blockStatement) {
    dumpNode(blockStatement);
    dumpChildren(blockStatement.statements);
    return null;
  }

  @Override
  public Void visit(LetStatement letStatement) {
    dumpNode(letStatement);
    dumpChildren(letStatement.valueExpr, letStatement.body);
    return null;
  }

  @Override
  public Void visit(IfStatement ifStatement) {
    dumpNode(ifStatement);
    dumpChildren(ifStatement.condition, ifStatement.thenStmt);
    if (ifStatement.elseStmt != null) {
      dumpChildren(ifStatement.elseStmt);
    }
    return null;
  }

  @Override
  public Void visit(AssignmentStatement assignmentStatement) {
    builder.append(indentString()).append("AssignmentStatement\n");
    indent++;
    assignmentStatement.target.accept(this);
    assignmentStatement.valueExpression.accept(this);
    indent--;
    return null;
  }

  @Override
  public Void visit(RaiseStatement raiseStatement) {
    dumpNode(raiseStatement);
    dumpChildren(raiseStatement.statement);
    return null;
  }

  @Override
  public Void visit(CallStatement callStatement) {
    dumpNode(callStatement);
    dumpChildren(callStatement.expr);
    return null;
  }

  @Override
  public Void visit(PlaceholderStatement placeholderStatement) {
    dumpNode(placeholderStatement);
    return null;
  }

  @Override
  public Void visit(MacroInstanceStatement macroInstanceStatement) {
    dumpNode(macroInstanceStatement);
    return null;
  }

  @Override
  public Void visit(MacroMatchStatement macroMatchStatement) {
    dumpNode(macroMatchStatement);
    return null;
  }

  @Override
  public Void visit(MatchStatement matchStatement) {
    dumpNode(matchStatement);
    dumpChildren(matchStatement.candidate);
    if (matchStatement.defaultResult != null) {
      dumpChildren(matchStatement.defaultResult);
    }
    for (var matchCase : matchStatement.cases) {
      dumpChildren(matchCase.patterns);
      dumpChildren(matchCase.result);
    }
    return null;
  }

  @Override
  public Void visit(StatementList statementList) {
    dumpNode(statementList);
    dumpChildren(statementList.items);
    return null;
  }

  @Override
  public Void visit(InstructionCallStatement instructionCallStatement) {
    dumpNode(instructionCallStatement);
    dumpChildren(instructionCallStatement.id());
    dumpChildren(instructionCallStatement.namedArguments.stream()
        .flatMap(namedArgument -> Stream.of(namedArgument.name, namedArgument.value)).toList());
    dumpChildren(instructionCallStatement.unnamedArguments);
    return null;
  }

  @Override
  public Void visit(LockStatement lockStatement) {
    dumpNode(lockStatement);
    dumpChildren(lockStatement.expr, lockStatement.statement);
    return null;
  }

  @Override
  public Void visit(ForallStatement forallStatement) {
    dumpNode(forallStatement);
    for (ForallStatement.Index index : forallStatement.indices) {
      dumpChildren(index.identifier(), index.domain);
    }
    dumpChildren(forallStatement.statement);
    return null;
  }
}
