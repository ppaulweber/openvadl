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

package vadl.iss;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.AbstractTest;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.IssConfiguration;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;

public class IssLoweringTest extends AbstractTest {

  private static final Logger log = LoggerFactory.getLogger(IssLoweringTest.class);

  // TODO: Remove this (it is just for testing purposes)
  @Test
  void issLoweringTest() throws IOException, DuplicatedPassKeyException {
    var config =
        new IssConfiguration(new GeneralConfiguration(Path.of("build/test-output"), true));
    // skip allocation
    config.setOptsToSkip(EnumSet.of(
//        IssConfiguration.IssOptsToSkip.OPT_JMP_SLOTS,
//        IssConfiguration.IssOptsToSkip.OPT_CTRL_FLOW,
//        IssConfiguration.IssOptsToSkip.OPT_VAR_ALLOC,
        IssConfiguration.IssOptsToSkip.OPT_ARGS
//        IssConfiguration.IssOptsToSkip.OPT_BUILT_INS
    ));

    setupPassManagerAndRunSpec("sys/risc-v/rv64im.vadl",
        PassOrders.iss(config)
    );

  }
}
