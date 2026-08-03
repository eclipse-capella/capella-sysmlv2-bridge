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
package org.obeonetwork.capella.sysmlv2.arcadia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalArcadiaLibraryLocatorTest {

	@TempDir
	private Path temporaryDirectory;

	@Test
	void locatesValidLocalCheckoutPath() throws IOException {
		var checkout = Files.createDirectories(
				temporaryDirectory.resolve("arcadia-sysmlv2-lib"));
		var libraryFile = Files.writeString(checkout.resolve("Arcadia.sysml"),
				"package Arcadia;\n");

		var library = new LocalArcadiaLibraryLocator(checkout).locate();

		assertThat(library.rootDirectory()).isEqualTo(checkout);
		assertThat(library.sysmlFiles()).containsExactly(libraryFile);
	}

	@Test
	void rejectsMissingPath() {
		var missingCheckout = temporaryDirectory
				.resolve("missing-arcadia-sysmlv2-lib");
		var locator = new LocalArcadiaLibraryLocator(missingCheckout);

		assertThatThrownBy(locator::locate)
				.isInstanceOf(ArcadiaLibraryException.class)
				.hasMessageContaining("does not exist")
				.hasMessageContaining(missingCheckout.toString());
	}

	@Test
	void rejectsDirectoryWithoutSysMLFiles() throws IOException {
		var checkout = Files.createDirectories(
				temporaryDirectory.resolve("arcadia-sysmlv2-lib"));
		Files.writeString(checkout.resolve("README.md"), "# Arcadia library\n");
		var locator = new LocalArcadiaLibraryLocator(checkout);
		assertThatThrownBy(locator::locate)
				.isInstanceOf(ArcadiaLibraryException.class)
				.hasMessageContaining("contains no .sysml files")
				.hasMessageContaining(checkout.toString());
	}

	@Test
	void discoversSysMLFilesRecursively() throws IOException {
		var checkout = Files.createDirectories(
				temporaryDirectory.resolve("arcadia-sysmlv2-lib"));
		var nestedDirectory = Files
				.createDirectories(checkout.resolve("libraries/arcadia"));
		var nestedFile = Files.writeString(
				nestedDirectory.resolve("Behavior.sysml"),
				"package Behavior;\n");

		var library = new LocalArcadiaLibraryLocator(checkout).locate();

		assertThat(library.sysmlFiles()).containsExactly(nestedFile);
	}

	@Test
	void returnsDiscoveredSysMLFilesInDeterministicOrder() throws IOException {
		var checkout = Files.createDirectories(
				temporaryDirectory.resolve("arcadia-sysmlv2-lib"));
		var zFile = Files.writeString(checkout.resolve("z.sysml"),
				"package Z;\n");
		var nestedDirectory = Files.createDirectories(checkout.resolve("a"));
		var aFile = Files.writeString(nestedDirectory.resolve("a.sysml"),
				"package A;\n");
		var bFile = Files.writeString(checkout.resolve("b.sysml"),
				"package B;\n");

		var library = new LocalArcadiaLibraryLocator(checkout).locate();

		assertThat(library.sysmlFiles()).containsExactly(aFile, bFile, zFile);
	}

	@Test
	void rejectsMissingCheckoutDirectoryValue() {
		assertThatNullPointerException()
				.isThrownBy(() -> new LocalArcadiaLibraryLocator(null))
				.withMessage("checkoutDirectory must not be null");
	}
}
