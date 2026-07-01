package com.sitewatch.app.ui.navigation

/** Type-safe-ish route definitions for the nav graph. */
object Routes {
    const val DASHBOARD = "dashboard"
    const val NOTIFICATIONS = "notifications"
    const val ARG_SITE_ID = "siteId"

    /** Add/Edit screen. A null/absent siteId means "add". */
    const val ADD_EDIT_BASE = "site"
    const val ADD_EDIT = "$ADD_EDIT_BASE?$ARG_SITE_ID={$ARG_SITE_ID}"

    /** Read-only detail screen for a single site. */
    const val DETAIL_BASE = "detail"
    const val DETAIL = "$DETAIL_BASE/{$ARG_SITE_ID}"

    fun addSite(): String = ADD_EDIT_BASE
    fun editSite(siteId: String): String = "$ADD_EDIT_BASE?$ARG_SITE_ID=$siteId"
    fun siteDetail(siteId: String): String = "$DETAIL_BASE/$siteId"
}
