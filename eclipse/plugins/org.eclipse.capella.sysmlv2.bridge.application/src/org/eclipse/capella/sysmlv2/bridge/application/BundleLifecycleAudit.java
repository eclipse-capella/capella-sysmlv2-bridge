package org.eclipse.capella.sysmlv2.bridge.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;

final class BundleLifecycleAudit implements AutoCloseable {

    private final BundleContext context;
    private final Map<Long, BundleState> initialStates = new HashMap<>();
    private final Map<Long, LinkedHashSet<String>> events = new HashMap<>();
    private final SynchronousBundleListener listener = this::bundleChanged;
    private boolean listening;
    private Report report;

    BundleLifecycleAudit(BundleContext context) {
        this.context = context;
    }

    synchronized void start() {
        if (listening) {
            return;
        }
        context.addBundleListener(listener);
        listening = true;
        initialStates.putAll(snapshot());
    }

    synchronized Report finish() {
        if (report != null) {
            return report;
        }
        stopListening();
        Map<Long, BundleState> finalStates = snapshot();
        var ids = new LinkedHashSet<Long>();
        ids.addAll(initialStates.keySet());
        ids.addAll(finalStates.keySet());
        ids.addAll(events.keySet());

        var bundles = new ArrayList<BundleRecord>();
        for (Long id : ids) {
            BundleState initial = initialStates.get(id);
            BundleState current = finalStates.get(id);
            BundleState identity = current != null ? current : initial;
            if (identity != null) {
                bundles.add(new BundleRecord(id, identity.symbolicName(), identity.version(),
                        initial == null ? "NOT_OBSERVED" : initial.state(),
                        current == null ? "NOT_OBSERVED" : current.state(),
                        List.copyOf(events.getOrDefault(id, new LinkedHashSet<>()))));
            }
        }
        bundles.sort(Comparator.comparing(BundleRecord::symbolicName)
                .thenComparing(BundleRecord::version)
                .thenComparingLong(BundleRecord::id));
        List<BundleRecord> immutableBundles = List.copyOf(bundles);
        report = new Report(Runtime.version().feature(), System.getProperty("java.version", ""),
                System.getProperty("java.vendor", ""), System.getProperty("os.name", ""),
                System.getProperty("os.arch", ""), immutableBundles, UiBundlePolicy.violations(immutableBundles));
        return report;
    }

    @Override
    public synchronized void close() {
        stopListening();
    }

    private synchronized void bundleChanged(BundleEvent event) {
        events.computeIfAbsent(event.getBundle().getBundleId(), ignored -> new LinkedHashSet<>())
                .add(eventName(event.getType()));
    }

    private Map<Long, BundleState> snapshot() {
        var states = new HashMap<Long, BundleState>();
        for (Bundle bundle : context.getBundles()) {
            String symbolicName = bundle.getSymbolicName();
            if (symbolicName == null || symbolicName.isBlank()) {
                symbolicName = "<unnamed:" + bundle.getBundleId() + ">";
            }
            states.put(bundle.getBundleId(), new BundleState(symbolicName, bundle.getVersion().toString(),
                    stateName(bundle.getState())));
        }
        return states;
    }

    private void stopListening() {
        if (listening) {
            context.removeBundleListener(listener);
            listening = false;
        }
    }

    private static String stateName(int state) {
        return switch (state) {
            case Bundle.UNINSTALLED -> "UNINSTALLED";
            case Bundle.INSTALLED -> "INSTALLED";
            case Bundle.RESOLVED -> "RESOLVED";
            case Bundle.STARTING -> "STARTING";
            case Bundle.STOPPING -> "STOPPING";
            case Bundle.ACTIVE -> "ACTIVE";
            default -> "UNKNOWN(" + state + ")";
        };
    }

    private static String eventName(int type) {
        return switch (type) {
            case BundleEvent.INSTALLED -> "INSTALLED";
            case BundleEvent.STARTED -> "STARTED";
            case BundleEvent.STOPPED -> "STOPPED";
            case BundleEvent.UPDATED -> "UPDATED";
            case BundleEvent.UNINSTALLED -> "UNINSTALLED";
            case BundleEvent.RESOLVED -> "RESOLVED";
            case BundleEvent.UNRESOLVED -> "UNRESOLVED";
            case BundleEvent.STARTING -> "STARTING";
            case BundleEvent.STOPPING -> "STOPPING";
            case BundleEvent.LAZY_ACTIVATION -> "LAZY_ACTIVATION";
            default -> "UNKNOWN(" + type + ")";
        };
    }

    private record BundleState(String symbolicName, String version, String state) {
    }

    record BundleRecord(long id, String symbolicName, String version, String initialState, String finalState,
            List<String> events) {
    }

    record Report(int javaFeature, String javaVersion, String javaVendor, String osName, String osArch,
            List<BundleRecord> bundles, List<String> violations) {

        boolean passed() {
            return violations.isEmpty();
        }
    }
}
