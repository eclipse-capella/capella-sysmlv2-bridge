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
package org.eclipse.capella.sysmlv2.bridge.mappings.services.transverse;

import static org.eclipse.capella.sysmlv2.bridge.mappings.services.transverse.TransverseQueryService.ARCADIA_DESCRIPTION;
import static org.eclipse.capella.sysmlv2.bridge.mappings.services.transverse.TransverseQueryService.PATH_SEPARATOR;

import java.util.List;
import java.util.Optional;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.syson.services.UtilService;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.Expression;
import org.eclipse.syson.sysml.Feature;
import org.eclipse.syson.sysml.FeatureDirectionKind;
import org.eclipse.syson.sysml.LiteralBoolean;
import org.eclipse.syson.sysml.Membership;
import org.eclipse.syson.sysml.ParameterMembership;
import org.eclipse.syson.sysml.ReferenceUsage;
import org.eclipse.syson.sysml.SysmlFactory;
import org.eclipse.syson.sysml.SysmlPackage;
import org.eclipse.syson.sysml.Type;
import org.eclipse.syson.sysml.Usage;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationContext;

/**
 * Transverse mutation service. It is important to note that this service must
 * retain its empty constructor and should not have constructors with
 * parameters.
 *
 * @author rpage
 */
public class TransverseMutationService {
	private final UtilService utilService;

	private final TransverseQueryService transverseQueryService;

	private final TransformationContext context;

	public TransverseMutationService(TransformationContext context) {
		this.utilService = new UtilService();
		this.transverseQueryService = new TransverseQueryService();
		this.context = context;
	}

	public Element setElementDescription(Element element,
			String newDescription) {
		var descriptionDoc = element.getDocumentation().stream()
				.filter(documentation -> ARCADIA_DESCRIPTION
						.equals(documentation.getDeclaredName()))
				.findFirst().orElseGet(() -> {
					var documentation = SysmlFactory.eINSTANCE
							.createDocumentation();
					documentation.setDeclaredName(ARCADIA_DESCRIPTION);
					addChildInParent(element, documentation);
					return documentation;
				});
		descriptionDoc.setBody(newDescription);
		return element;
	}

	public void setBooleanAttribute(Usage usage, String prefix,
			String attributeName, boolean newValue) {
		Optional<LiteralBoolean> optionalExitingValue = this.transverseQueryService
				.getFeatureReferenceExpression(usage, attributeName)
				.filter(LiteralBoolean.class::isInstance)
				.map(LiteralBoolean.class::cast);

		// If the value is already set, we retrieve it to set the new value.
		var descriptionValue = optionalExitingValue.orElseGet(() -> this
				.createLiteralBooleanAttribute(usage, prefix, attributeName));
		descriptionValue.setValue(newValue);
	}

	private LiteralBoolean createLiteralBooleanAttribute(Usage usage,
			String prefix, String attributeName) {
		var literalBoolean = SysmlFactory.eINSTANCE.createLiteralBoolean();
		this.redefineFeature(usage, prefix, attributeName,
				List.of(literalBoolean),
				SysmlPackage.eINSTANCE.getAttributeUsage());
		return literalBoolean;
	}

	private void redefineFeature(Usage usage, String prefix,
			String attributeName, List<Expression> values,
			EClass referencedFeatureType) {
		var libraryFeatureAbsolutePath = prefix + PATH_SEPARATOR
				+ attributeName;

		// Step 1 : We try to retrieve the Reference Usage if it is already
		// defined.
		var optionalReferenceUsage = usage.getNestedReference().stream()
				.filter(referenceUsage -> attributeName
						.equals(referenceUsage.getName()))
				.findFirst().or(() -> this.createReferenceUsage(usage,
						referencedFeatureType, libraryFeatureAbsolutePath));

		// Step 2 : for each values, we create the Feature Value and add it in
		// the reference usage relationships.
		if (optionalReferenceUsage.isPresent()) {

			// For multivalued (multi reference expression, we add them in an
			// operator expression.
			if (values.size() > 1) {
				var operatorExpression = SysmlFactory.eINSTANCE
						.createOperatorExpression();
				var featureValue = SysmlFactory.eINSTANCE.createFeatureValue();
				featureValue.getOwnedRelatedElement().add(operatorExpression);
				optionalReferenceUsage.get().getOwnedRelationship()
						.add(featureValue);
				for (Expression value : values) {
					var parameter = this.createParameter(value);
					operatorExpression.getOwnedRelationship().add(parameter);
				}
			} else if (!values.isEmpty()) {
				var featureValue = SysmlFactory.eINSTANCE.createFeatureValue();
				featureValue.getOwnedRelatedElement().add(values.get(0));
				optionalReferenceUsage.get().getOwnedRelationship()
						.add(featureValue);
			}

		}
	}

	private Optional<ReferenceUsage> createReferenceUsage(Usage usage,
			EClass referencedFeatureType, String libraryFeatureAbsolutePath) {

		// Step 1 : We retrieve the Usage defined in the library.
		var optionalUsage = this.utilService
				.getAllReachable(usage, referencedFeatureType).stream()
				.filter(Usage.class::isInstance).map(Usage.class::cast)
				.filter(currentUsage -> libraryFeatureAbsolutePath
						.equals(currentUsage.getQualifiedName()))
				.findFirst();
		if (optionalUsage.isPresent()) {

			// Step 2 : we create the Reference Usage
			var featureMembership = SysmlFactory.eINSTANCE
					.createFeatureMembership();
			usage.getOwnedRelationship().add(featureMembership);
			var referenceUsage = SysmlFactory.eINSTANCE.createReferenceUsage();
			featureMembership.getOwnedRelatedElement().add(referenceUsage);

			// Step 3 : We set the redefinition referencing the library usage
			// (ref usage or attribute usage).
			var redefinition = SysmlFactory.eINSTANCE.createRedefinition();
			redefinition.setRedefinedFeature(optionalUsage.get());
			redefinition.setRedefiningFeature(referenceUsage);
			referenceUsage.getOwnedRelationship().add(redefinition);
			return Optional.of(referenceUsage);
		}
		return Optional.empty();
	}

	private ParameterMembership createParameter(Expression value) {
		var parameterMembership = SysmlFactory.eINSTANCE
				.createParameterMembership();
		var feature = SysmlFactory.eINSTANCE.createFeature();
		feature.setDirection(FeatureDirectionKind.IN);
		parameterMembership.getOwnedRelatedElement().add(feature);
		var featureValue = SysmlFactory.eINSTANCE.createFeatureValue();
		feature.getOwnedRelationship().add(featureValue);
		featureValue.getOwnedRelatedElement().add(value);
		return parameterMembership;
	}

	/**
	 * Add an Element in a parent using the proper {@link Membership}. TODO :
	 * Replace by
	 * org.eclipse.syson.sysml.metamodel.services.MetamodelMutationElementService#addChildInParent
	 * when released
	 *
	 * @param parent
	 *            a parent element
	 * @param child
	 *            a child
	 */
	public void addChildInParent(Element parent, Element child) {
		final Membership membership;
		if (child instanceof Feature && parent instanceof Type) {
			membership = SysmlFactory.eINSTANCE.createFeatureMembership();
		} else {
			membership = SysmlFactory.eINSTANCE.createOwningMembership();
		}
		membership.getOwnedRelatedElement().add(child);
		parent.getOwnedRelationship().add(membership);

		context.registerGenerated(membership);
	}
}
