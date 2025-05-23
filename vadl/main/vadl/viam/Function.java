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

package vadl.viam;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import vadl.types.ConcreteRelationType;
import vadl.types.Type;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ReturnNode;

/**
 * Represents a Function in a VADL specification.
 *
 * <p>
 * A Function is a type of Definition that has a behavior (body), return type, and arguments.
 * </p>
 */
public class Function extends Definition implements DefProp.WithBehavior, DefProp.WithType {
  private Graph behavior;
  private Type returnType;
  private Parameter[] parameters;

  /**
   * Creates a new Function with the specified identifier, parameters, and return type.
   *
   * <p>An empty behaviour-graph is automatically created.</p>
   *
   * @param identifier The identifier of the Function.
   * @param parameters The parameters of the Function.
   * @param returnType The return type of the Function.
   * @deprecated Use the other constructor instead.
   */
  @Deprecated
  public Function(Identifier identifier,
                  Parameter[] parameters,
                  Type returnType) {
    this(identifier, parameters, returnType, new Graph(identifier.name()));
  }

  /**
   * Creates a new Function with the specified identifier, parameters, and return type.
   *
   * @param identifier The identifier of the Function.
   * @param parameters The parameters of the Function.
   * @param returnType The return type of the Function.
   * @param behavior   The behavior of the Function.
   */
  public Function(Identifier identifier,
                  Parameter[] parameters,
                  Type returnType,
                  Graph behavior) {
    super(identifier);
    this.behavior = behavior;
    this.returnType = returnType;
    this.parameters = parameters;

    behavior.setParentDefinition(this);
    Arrays.stream(parameters).forEach(p -> p.setParent(this));
  }

  @Override
  @SuppressWarnings("LineLength")
  public void verify() {
    super.verify();
    ensure(behavior.isPureFunction(), "The function must be pure (no side effects)");

    var returnNode = behavior().getNodes(ReturnNode.class).findFirst().get();
    ensure(returnNode.value().type().isTrivialCastTo(returnType),
        "The function's behavior does not return a value that can be trivially cast to the as signature's result type: %s",
        returnNode.value().type());
    behavior.verify();
  }

  public Graph behavior() {
    return behavior;
  }

  public Parameter[] parameters() {
    return parameters;
  }

  public void setBehavior(Graph graph) {
    this.behavior = graph;
    graph.setParentDefinition(this);
  }

  public ConcreteRelationType signature() {
    return Type.concreteRelation(Stream.of(parameters).map(Parameter::type).toList(), returnType);
  }

  public Type returnType() {
    return returnType;
  }

  public void setReturnType(Type returnType) {
    this.returnType = returnType;
  }

  public void setParameters(Parameter[] parameters) {
    this.parameters = parameters;
  }

  @Override
  public String toString() {
    return simpleName() + signature();
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public List<Graph> behaviors() {
    return List.of(behavior);
  }

  @Override
  public Type type() {
    return signature();
  }
}
