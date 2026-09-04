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

import java.util.Objects;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.MetaclassId;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;

public abstract class TypedCapellaMapperAdapter<T extends EObject> implements ElementMapper {

    private final MetaclassId sourceType;

    private final Class<T> sourceJavaType;

    protected TypedCapellaMapperAdapter(EClass sourceType, Class<T> sourceJavaType) {
        this(MetaclassId.of(Objects.requireNonNull(sourceType, "sourceType must not be null")), sourceJavaType);
    }

    protected TypedCapellaMapperAdapter(String sourceNsUri, String sourceClassName, Class<T> sourceJavaType) {
        this(new MetaclassId(sourceNsUri, sourceClassName), sourceJavaType);
    }

    private TypedCapellaMapperAdapter(MetaclassId sourceType, Class<T> sourceJavaType) {
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
        this.sourceJavaType = Objects.requireNonNull(sourceJavaType, "sourceJavaType must not be null");
    }

    @Override
    public final MetaclassId sourceType() {
        return this.sourceType;
    }

    @Override
    public final void transform(EObject source, TransformationContext context) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(context, "context must not be null");
        if (!this.sourceJavaType.isInstance(source)) {
            context.diagnostics().error(source, "Expected a Capella " + this.sourceJavaType.getSimpleName());
            return;
        }
        this.transformTyped(this.sourceJavaType.cast(source), context);
    }

    protected abstract void transformTyped(T source, TransformationContext context);
}
