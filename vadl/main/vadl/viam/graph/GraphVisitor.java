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

package vadl.viam.graph;

import java.util.List;
import javax.annotation.Nullable;

/**
 * The {@link GraphVisitor} interface represents a visitor that can visit nodes in a graph and
 * obtain a result of type R.
 *
 * @param <R> the type of the result obtained from visiting the graph nodes
 */
public interface GraphVisitor<R> {

  @Nullable
  R visit(Node from, @Nullable Node to);

  /**
   * The {@code NodeApplier} interface represents a graph node visitor that changes
   * nodes in a graph. When the {@code visit} method returns {@code null} then the
   * node will be deleted. If the same node was returned then it will be replaced nonetheless.
   * Also note that mutability in {@code visit} method is allowed.
   */
  interface NodeApplier<T extends Node, R extends Node> {
    @Nullable
    R visit(T node);

    boolean acceptable(Node node);

    List<NodeApplier<? extends Node, ? extends Node>> recursiveHooks();

    /**
     * Checks whether the {@link NodeApplier} is applicable for the given {@code node}.
     */
    default List<NodeApplier<? extends Node, ? extends Node>> applicable(Node node) {
      return recursiveHooks()
          .stream()
          .filter(x -> x.acceptable(node))
          .toList();
    }

    /**
     * Find the applicable hook and run it for the given {@code arg}.
     */
    default void visitApplicable(Node arg) {
      var applicable = applicable(arg);
      for (var applier : applicable) {
        NodeApplier<Node, Node> cast = (NodeApplier<Node, Node>) applier;
        cast.apply(arg);
      }
    }

    /**
     * Apply the {@link NodeApplier} and replace and delete the node optionally.
     */
    @Nullable
    default R apply(T node) {
      var newNode = visit(node);

      if (newNode == null) {
        if (!node.isDeleted()) {
          node.safeDelete();
        }
        return null;
      }

      if (!node.isDeleted() && node != newNode) {
        return node.replaceAndDelete(newNode);
      } else if (!node.isDeleted() && node == newNode) {
        return newNode;
      }

      return null;
    }
  }

  /**
   * The Applier interface represents a graph visitor that assigns new values to inputs of
   * a node in a graph.
   *
   * @param <R> the type of the result that gets assigned to the node's inputs
   */
  interface Applier<R extends Node> extends GraphVisitor<R> {
    @Nullable
    @Override
    default R visit(Node from, @Nullable Node to) {
      return applyNullable(from, to);
    }

    /**
     * Applies a transformation to an edge and returns the result as an instance of the
     * specified class.
     *
     * @param from  the start of the edge (not input node)
     * @param to    the end of the edge (input node)
     * @param clazz the class representing the desired type of the result
     * @param <T>   the type of the result
     * @return the result that is applied to the {@code to} node
     * @throws ViamGraphError if the applier produces invalid node type
     */
    default <T extends Node> T apply(Node from, @Nullable Node to, Class<T> clazz) {
      var newNode = apply(from, to);
      newNode.ensure(clazz.isInstance(newNode), "unable to apply %s to %s: types are mismatching",
          newNode.getClass(),
          clazz);
      return clazz.cast(newNode);
    }

    /**
     * Applies a transformation to the given nodes and returns the result.
     *
     * @param from the start of the edge (not input node)
     * @param to   the end of the edge (input node)
     * @return the result of the transformation
     * @throws ViamGraphError if the applier produces a null node
     */
    default R apply(Node from, @Nullable Node to) {
      var newNode = applyNullable(from, to);
      ViamGraphError.ensureNonNull(newNode, from.graph(), from, to,
          "applier produced a null node for apply(). Checkout the implementation of the "
              + "from-node, you probably want to use applyNullable for the Nullable field.");
      return newNode;
    }

    /**
     * Applies a transformation to an edge and returns the result as an instance of the
     * specified class. The result may be null.
     *
     * @param from  the start of the edge (not input node)
     * @param to    the end of the edge (input node)
     * @param clazz the class representing the desired type of the result
     * @param <T>   the type of the result
     * @return the result that is applied to the {@code to} node
     * @throws ViamGraphError if the applier produces invalid node type
     */
    @Nullable
    default <T extends Node> T applyNullable(Node from, @Nullable Node to, Class<T> clazz) {
      var newNode = applyNullable(from, to);
      if (newNode == null) {
        return null;
      }
      newNode.ensure(clazz.isInstance(newNode), "unable to apply %s to %s: types are mismatching",
          newNode.getClass(),
          clazz);
      return clazz.cast(newNode);
    }

    @Nullable
    R applyNullable(Node from, @Nullable Node to);


  }
}
