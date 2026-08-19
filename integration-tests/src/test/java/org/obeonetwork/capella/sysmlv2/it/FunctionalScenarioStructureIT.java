/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.obeonetwork.capella.sysmlv2.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.obeonetwork.capella.sysmlv2.testsupport.TestResources;

class FunctionalScenarioStructureIT {

    private static List<String> scenarios() {
        return List.of(
                "01-empty-model",
                "02-logical-component",
                "03-logical-function",
                "04-functional-exchange");
    }

    @ParameterizedTest
    @MethodSource("scenarios")
    void functionalScenarioContainsInputAndExpectedDirectories(String scenarioName) {
        var runner = new FunctionalTestRunner(new TestResources(getClass()));

        var scenario = runner.scenario(scenarioName);

        assertThat(scenario.rootDirectory()).isDirectory();
        assertThat(scenario.inputDirectory()).isDirectory();
        assertThat(scenario.expectedDirectory()).isDirectory();
    }
}
