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
package org.eclipse.capella.sysmlv2.bridge.mappings;

/**
 * SysML target labels used in mapping descriptions.
 */
public final class SysmlNames {
    public static final String DECLARED_NAME = "SysML::Element::declaredName";

    public static final String DESCRIPTION = "doc description /* */";

    public static final String FLOW_USAGE_SOURCE = "SysML::FlowUsage::source";

    public static final String FLOW_USAGE_TARGET = "SysML::FlowUsage::target";

    public static final String ITEM_USAGE = "SysML::ItemUsage";

    public static final String FEATURE_VALUE = "SysML::Feature::value";

    public static final String FLOW_USAGE_PAYLOAD = "SysML::FlowUsage::payload";

    private SysmlNames() {
    }
}
