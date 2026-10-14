package com.sanitizer.gui.navigation;

/**
 * Lifecycle contract for navigable GUI views.
 * Replaces static UI Node caching by allowing views to refresh dynamic telemetry,
 * attach hardware listeners when active, and cleanly detach listeners and cancel timers when deactivated.
 */
public interface ViewLifecycle {

    /**
     * Invoked when navigating to this tab / view.
     * Refreshes data, attaches hardware listeners, and starts active telemetry.
     */
    void onActivated();

    /**
     * Invoked when navigating away from this tab / view.
     * Detaches hardware listeners, stops animations, and cancels active polling.
     */
    void onDeactivated();
}
