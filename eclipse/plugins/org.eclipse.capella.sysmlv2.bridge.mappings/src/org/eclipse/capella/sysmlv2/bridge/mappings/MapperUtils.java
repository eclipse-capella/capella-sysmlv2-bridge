package org.eclipse.capella.sysmlv2.bridge.mappings;

import org.eclipse.syson.sysml.Element;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationContext;
import org.polarsys.capella.common.data.modellingcore.AbstractNamedElement;

public class MapperUtils {

    private MapperUtils() {
    }

    public static void transformName(AbstractNamedElement source,
            Element target, TransformationContext context) {
        var name = source.getName();
        if (name == null || name.isBlank()) {
            context.diagnostics().error(source,
                    "LogicalComponent.name must be a non-blank string");
        } else {
            target.setDeclaredName(name);
        }
    }
}
