package com.app.backend.security;

/**
 * Central permission codes (stored in DB and enforced server-side).
 */
public final class PermissionCodes {

    private PermissionCodes() {
    }

    public static final String OWNER_VIEW = "OWNER_VIEW";
    public static final String OWNER_EDIT = "OWNER_EDIT";
    public static final String PLOT_DELETE = "PLOT_DELETE";
    public static final String USER_MANAGE = "USER_MANAGE";
    public static final String ASSIGN_AGENT = "ASSIGN_AGENT";
    public static final String VIEW_ACTIVITY_LOGS = "VIEW_ACTIVITY_LOGS";
}
