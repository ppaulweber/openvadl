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

package vadl.types;

/**
 * An arbitrary sized signed integer.
 */
public class SIntType extends BitsType {

  protected SIntType(int bitWidth) {
    super(bitWidth);
  }

  @Override
  public int useableBitWidth() {
    return bitWidth() - 1;
  }

  @Override
  public String name() {
    return "SInt<%s>".formatted(bitWidth);
  }


  @Override
  public boolean isSigned() {
    return true;
  }

  @Override
  public boolean equals(Object obj) {
    return this.getClass() == obj.getClass()
        && super.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }


  @Override
  public BitsType withBitWidth(int bitWidth) {
    return Type.signedInt(bitWidth);
  }
}
