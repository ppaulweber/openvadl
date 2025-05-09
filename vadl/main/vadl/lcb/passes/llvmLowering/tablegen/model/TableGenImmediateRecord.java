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

package vadl.lcb.passes.llvmLowering.tablegen.model;

import java.util.Objects;
import vadl.gcb.valuetypes.VariantKind;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.template.lib.Target.Disassembler.EmitDisassemblerCppFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCCodeEmitterCppFilePass;
import vadl.types.BitsType;
import vadl.viam.Format;
import vadl.viam.Identifier;

/**
 * Represents an immediate record in TableGen.
 */
public class TableGenImmediateRecord {
  private final String name;
  // The `encoderMethod` will be used by tablegen and has different arguments.
  private final Identifier encoderMethod;
  // The `rawEncoderMethod` is the method for the raw logic.
  private final Identifier rawEncoderMethod;
  private final Identifier decoderMethod;
  private final Identifier rawDecoderMethod;
  private final Identifier predicateMethod;
  private final ValueType llvmType;
  private final BitsType rawType;
  private final int formatFieldBitSize;
  private final Format.FieldAccess fieldAccessRef;
  private final VariantKind absoluteVariantKind;
  private final VariantKind relativeVariantKind;

  /**
   * Constructor for an immediate operand.
   */
  private TableGenImmediateRecord(Identifier identifier,
                                  Identifier encoderIdentifier,
                                  Identifier rawEncoderIdentifier,
                                  Identifier decoderIdentifier,
                                  Identifier rawDecoderIdentifier,
                                  Identifier predicateIdentifier,
                                  ValueType llvmType,
                                  Format.FieldAccess fieldAccessRef) {
    this.name = identifier.lower();
    this.encoderMethod = encoderIdentifier;
    this.rawEncoderMethod = rawEncoderIdentifier;
    this.decoderMethod = decoderIdentifier;
    this.rawDecoderMethod = rawDecoderIdentifier;
    this.predicateMethod = predicateIdentifier;
    this.llvmType = llvmType;
    this.rawType = (BitsType) fieldAccessRef.type();
    this.formatFieldBitSize = fieldAccessRef.fieldRef().size();
    this.fieldAccessRef = fieldAccessRef;
    this.absoluteVariantKind = VariantKind.absolute(fieldAccessRef.fieldRef());
    this.relativeVariantKind = VariantKind.relative(fieldAccessRef.fieldRef());
  }

  /**
   * Constructor.
   */
  public TableGenImmediateRecord(Format.FieldAccess fieldAccess,
                                 ValueType llvmType) {
    this(fieldAccess.fieldRef().identifier,
        Objects.requireNonNull(fieldAccess.encoding()).identifier.append(
            EmitMCCodeEmitterCppFilePass.WRAPPER),
        Objects.requireNonNull(fieldAccess.encoding()).identifier,
        fieldAccess.accessFunction().identifier.append(EmitDisassemblerCppFilePass.WRAPPER),
        fieldAccess.accessFunction().identifier,
        fieldAccess.predicate().identifier,
        llvmType,
        fieldAccess);
  }

  public String rawName() {
    return name;
  }

  public String encoderMethod() {
    return encoderMethod.lower();
  }

  public String rawEncoderMethod() {
    return rawEncoderMethod.lower();
  }

  public String decoderMethod() {
    return decoderMethod.lower();
  }

  public String rawDecoderMethod() {
    return rawDecoderMethod.lower();
  }

  public ValueType llvmType() {
    return llvmType;
  }

  public BitsType rawType() {
    return rawType;
  }

  public String fullname() {
    return String.format("%sAs%s", this.name, llvmType.getTableGen());
  }

  public String predicateMethod() {
    return predicateMethod.lower();
  }

  public int formatFieldBitSize() {
    return formatFieldBitSize;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TableGenImmediateRecord that = (TableGenImmediateRecord) o;
    return Objects.equals(name, that.name)
        && Objects.equals(encoderMethod, that.encoderMethod)
        && Objects.equals(decoderMethod, that.decoderMethod)
        && Objects.equals(predicateMethod, that.predicateMethod)
        && llvmType == that.llvmType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, encoderMethod, decoderMethod, predicateMethod, llvmType);
  }

  public Format.FieldAccess fieldAccessRef() {
    return fieldAccessRef;
  }

  public VariantKind absoluteVariantKind() {
    return absoluteVariantKind;
  }

  public VariantKind relativeVariantKind() {
    return relativeVariantKind;
  }
}