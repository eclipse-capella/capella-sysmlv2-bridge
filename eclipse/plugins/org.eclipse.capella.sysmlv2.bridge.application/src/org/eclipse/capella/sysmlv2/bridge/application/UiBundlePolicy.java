package org.eclipse.capella.sysmlv2.bridge.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class UiBundlePolicy {

    private static final List<String> MONITORED_PREFIXES = List.of("org.eclipse.swt", "org.eclipse.ui",
            "org.eclipse.e4.ui");
    private static final Set<String> ALLOWED_HEADLESS_E4_BUNDLES = Set.of("org.eclipse.e4.ui.di",
            "org.eclipse.e4.ui.model.workbench", "org.eclipse.e4.ui.workbench");
    private static final Set<String> ACTIVE_STATES = Set.of("ACTIVE", "STOPPING");
    private static final Set<String> ACTIVATION_EVENTS = Set.of("STARTED", "STOPPING", "STOPPED");

    private UiBundlePolicy() {
    }

    static boolean isMonitored(String symbolicName) {
        return MONITORED_PREFIXES.stream()
                .anyMatch(prefix -> symbolicName.equals(prefix) || symbolicName.startsWith(prefix + "."));
    }

    static boolean isAllowedHeadlessInfrastructure(String symbolicName) {
        return ALLOWED_HEADLESS_E4_BUNDLES.contains(symbolicName);
    }

    static List<String> violations(List<BundleLifecycleAudit.BundleRecord> bundles) {
        var violations = new ArrayList<String>();
        for (var bundle : bundles) {
            if (!isMonitored(bundle.symbolicName()) || isAllowedHeadlessInfrastructure(bundle.symbolicName())) {
                continue;
            }
            boolean activeState = ACTIVE_STATES.contains(bundle.initialState())
                    || ACTIVE_STATES.contains(bundle.finalState());
            boolean activationEvent = bundle.events().stream().anyMatch(ACTIVATION_EVENTS::contains);
            if (activeState || activationEvent) {
                violations.add(bundle.symbolicName() + " initial=" + bundle.initialState() + " final="
                        + bundle.finalState() + " events=" + String.join(",", bundle.events()));
            }
        }
        return List.copyOf(violations);
    }
}
