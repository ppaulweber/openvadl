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

package vadl.lcb.template.lib.Target.MCTargetDesc;

import static vadl.lcb.template.utils.AbiSequencesUtil.createConstantSequences;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This file contains the implementation for constant materialisation.
 */
public class EmitConstMatIntCppFilePass extends LcbTemplateRenderingPass {

  public EmitConstMatIntCppFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/MCTargetDesc/TargetConstMatInt.cpp";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().targetName().value();
    return "llvm/lib/Target/" + processorName + "/MCTargetDesc/" + processorName
        + "ConstMatInt.cpp";
  }



  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var constantSequences = createConstantSequences(specification);

    var map = new HashMap<String, Object>();
    map.put(CommonVarNames.NAMESPACE, lcbConfiguration().targetName().value().toLowerCase());
    map.put("constantSequences", constantSequences);
    return map;
  }
}
