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
package org.eclipse.capella.sysmlv2.bridge.mappings.tests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.transaction.RecordingCommand;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.EObjectUtils;
import org.polarsys.capella.common.platform.sirius.ted.SemanticEditingDomainFactory;
import org.polarsys.capella.common.platform.sirius.ted.SemanticEditingDomainFactory.SemanticEditingDomain;
import org.polarsys.capella.core.data.cs.Component;
import org.polarsys.capella.core.data.ctx.CtxFactory;
import org.polarsys.capella.core.data.ctx.CtxPackage;
import org.polarsys.capella.core.data.fa.AbstractFunction;
import org.polarsys.capella.core.data.fa.ComponentExchange;
import org.polarsys.capella.core.data.fa.ComponentExchangeKind;
import org.polarsys.capella.core.data.fa.FaFactory;
import org.polarsys.capella.core.data.fa.FunctionalExchange;
import org.polarsys.capella.core.data.fa.OrientationPortKind;
import org.polarsys.capella.core.data.la.LaFactory;
import org.polarsys.capella.core.data.la.LaPackage;
import org.polarsys.capella.core.data.oa.OaFactory;
import org.polarsys.capella.core.data.pa.PaFactory;
import org.polarsys.capella.core.data.pa.PaPackage;

/**
 * Builds Capella models and supplies editing domains for mapper tests.
 */
public final class CapellaTestFixtures {

    /**
     * Arcadia perspectives supported by the exchange fixtures.
     */
    public enum Perspective {
        OPERATIONAL, SYSTEM, LOGICAL, PHYSICAL
    }

    private CapellaTestFixtures() {
    }

    /**
     * Creates a component exchange fixture, using a communication mean in the operational perspective.
     */
    public static ComponentExchangeFixture componentExchange(Perspective perspective) {
        EObject architecture;
        EObject root;
        ComponentExchange exchange;
        if (perspective == Perspective.OPERATIONAL) {
            var analysis = OaFactory.eINSTANCE.createOperationalAnalysis();
            var pkg = OaFactory.eINSTANCE.createEntityPkg();
            var entity = OaFactory.eINSTANCE.createEntity();
            analysis.setOwnedEntityPkg(pkg);
            pkg.getOwnedEntities().add(entity);
            var from = OaFactory.eINSTANCE.createEntity();
            var to = OaFactory.eINSTANCE.createEntity();
            entity.getOwnedEntities().addAll(List.of(from, to));
            var mean = OaFactory.eINSTANCE.createCommunicationMean();
            mean.setSource(from);
            mean.setTarget(to);
            entity.getOwnedCommunicationMeans().add(mean);
            architecture = analysis;
            root = entity;
            exchange = mean;
        } else {
            var modelPackage = switch (perspective) {
                case SYSTEM -> CtxPackage.eINSTANCE;
                case LOGICAL -> LaPackage.eINSTANCE;
                case PHYSICAL -> PaPackage.eINSTANCE;
                default -> throw new IllegalArgumentException();
            };
            var perspectiveName = switch (perspective) {
                case SYSTEM -> "System";
                case LOGICAL -> "Logical";
                case PHYSICAL -> "Physical";
                default -> throw new IllegalArgumentException();
            };
            var factory = modelPackage.getEFactoryInstance();
            var prefix = perspectiveName + "Architecture";
            if (perspective == Perspective.SYSTEM) {
                prefix = "SystemAnalysis";
            }
            architecture = factory.create((org.eclipse.emf.ecore.EClass) modelPackage.getEClassifier(prefix));
            var componentType = (org.eclipse.emf.ecore.EClass) modelPackage.getEClassifier(perspectiveName + "Component");
            var pkg = factory.create((org.eclipse.emf.ecore.EClass) modelPackage.getEClassifier(perspectiveName + "ComponentPkg"));
            set(architecture, "owned" + perspectiveName + "ComponentPkg", pkg);
            root = factory.create(componentType);
            add(pkg, "owned" + perspectiveName + "Components", root);
            var from = (Component) factory.create(componentType);
            var to = (Component) factory.create(componentType);
            add(root, "owned" + perspectiveName + "Components", from);
            add(root, "owned" + perspectiveName + "Components", to);
            var output = FaFactory.eINSTANCE.createComponentPort();
            output.setOrientation(OrientationPortKind.OUT);
            var input = FaFactory.eINSTANCE.createComponentPort();
            input.setOrientation(OrientationPortKind.IN);
            from.getOwnedFeatures().add(output);
            to.getOwnedFeatures().add(input);
            exchange = FaFactory.eINSTANCE.createComponentExchange();
            exchange.setKind(ComponentExchangeKind.FLOW);
            exchange.setSource(output);
            exchange.setTarget(input);
            add(root, "ownedComponentExchanges", exchange);
        }
        exchange.setName("Cable");
        exchange.setDescription("Carries data");
        return new ComponentExchangeFixture(architecture, root, exchange);
    }

    /**
     * Creates a functional exchange fixture in the given perspective.
     *
     * @param operationalPorts
     *            whether operational endpoints use ports instead of activities; other perspectives always use ports
     */
    public static FunctionalExchangeFixture functionalExchange(Perspective perspective, boolean operationalPorts) {
        EObject architecture;
        AbstractFunction root;
        switch (perspective) {
            case OPERATIONAL -> {
                var arch = OaFactory.eINSTANCE.createOperationalAnalysis();
                var pkg = OaFactory.eINSTANCE.createOperationalActivityPkg();
                var function = OaFactory.eINSTANCE.createOperationalActivity();
                arch.setOwnedFunctionPkg(pkg);
                pkg.getOwnedOperationalActivities().add(function);
                architecture = arch;
                root = function;
            }
            case SYSTEM -> {
                var arch = CtxFactory.eINSTANCE.createSystemAnalysis();
                var pkg = CtxFactory.eINSTANCE.createSystemFunctionPkg();
                var function = CtxFactory.eINSTANCE.createSystemFunction();
                arch.setOwnedFunctionPkg(pkg);
                pkg.getOwnedSystemFunctions().add(function);
                architecture = arch;
                root = function;
            }
            case LOGICAL -> {
                var arch = LaFactory.eINSTANCE.createLogicalArchitecture();
                var pkg = LaFactory.eINSTANCE.createLogicalFunctionPkg();
                var function = LaFactory.eINSTANCE.createLogicalFunction();
                arch.setOwnedFunctionPkg(pkg);
                pkg.getOwnedLogicalFunctions().add(function);
                architecture = arch;
                root = function;
            }
            case PHYSICAL -> {
                var arch = PaFactory.eINSTANCE.createPhysicalArchitecture();
                var pkg = PaFactory.eINSTANCE.createPhysicalFunctionPkg();
                var function = PaFactory.eINSTANCE.createPhysicalFunction();
                arch.setOwnedFunctionPkg(pkg);
                pkg.getOwnedPhysicalFunctions().add(function);
                architecture = arch;
                root = function;
            }
            default -> throw new IllegalArgumentException();
        }
        var source = (AbstractFunction) EcoreUtil.create(root.eClass());
        var target = (AbstractFunction) EcoreUtil.create(root.eClass());
        root.getOwnedFunctions().add(source);
        root.getOwnedFunctions().add(target);
        var exchange = FaFactory.eINSTANCE.createFunctionalExchange();
        exchange.setName("Data flow");
        exchange.setDescription("Transfers data");
        source.getOwnedFunctionalExchanges().add(exchange);
        if (perspective == Perspective.OPERATIONAL && !operationalPorts) {
            exchange.setSource(source);
            exchange.setTarget(target);
        } else {
            var output = FaFactory.eINSTANCE.createFunctionOutputPort();
            var input = FaFactory.eINSTANCE.createFunctionInputPort();
            source.getOutputs().add(output);
            target.getInputs().add(input);
            exchange.setSource(output);
            exchange.setTarget(input);
        }
        return new FunctionalExchangeFixture(architecture, root, source, target, exchange);
    }

    /**
     * Attaches the architecture to a temporary editing domain, runs assertions in a transaction and disposes the
     * domain.
     */
    public static void withEditingDomain(EObject architecture, Runnable assertions) {
        var domain = (SemanticEditingDomain) new SemanticEditingDomainFactory().createEditingDomain();
        try {
            domain.getResourceSet().eAdapters().remove(domain.getDataNotifier());
            domain.getCommandStack().execute(new RecordingCommand(domain) {
                @Override
                protected void doExecute() {
                    var resource = new ResourceImpl();
                    domain.getResourceSet().getResources().add(resource);
                    resource.getContents().add(architecture);
                    assertions.run();
                }
            });
        } finally {
            domain.dispose();
        }
    }

    private static void set(EObject owner, String feature, EObject value) {
        owner.eSet(owner.eClass().getEStructuralFeature(feature), value);
    }

    @SuppressWarnings("unchecked")
    private static void add(EObject owner, String feature, EObject value) {
        ((List<EObject>) owner.eGet(owner.eClass().getEStructuralFeature(feature))).add(value);
    }

    private static List<EObject> inputsWithExchangeFirst(EObject architecture, EObject exchange) {
        var inputs = new ArrayList<EObject>(EObjectUtils.allContents(architecture));
        Collections.reverse(inputs);
        inputs.remove(exchange);
        inputs.addFirst(exchange);
        return inputs;
    }

    /**
     * A component exchange with its containing architecture and root component or entity.
     */
    public record ComponentExchangeFixture(EObject architecture, EObject root, ComponentExchange exchange) {
        /**
         * Returns a fresh transformation input list with the exchange first and all remaining elements in reverse
         * traversal order.
         */
        public List<EObject> inputs() {
            return inputsWithExchangeFirst(architecture, exchange);
        }
    }

    /**
     * A functional exchange with its containing architecture, root function and endpoint functions.
     */
    public record FunctionalExchangeFixture(EObject architecture, AbstractFunction root, AbstractFunction source,
            AbstractFunction target, FunctionalExchange exchange) {
        /**
         * Returns a fresh transformation input list with the exchange first and all remaining elements in reverse
         * traversal order.
         */
        public List<EObject> inputs() {
            return inputsWithExchangeFirst(architecture, exchange);
        }
    }
}
