package com.sitewatch.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sitewatch.app.ui.addsite.AddSiteScreen
import com.sitewatch.app.ui.dashboard.DashboardScreen
import com.sitewatch.app.ui.detail.SiteDetailScreen
import com.sitewatch.app.ui.notifications.NotificationHistoryScreen

@Composable
fun SiteWatchNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onAddSite = { navController.navigate(Routes.addSite()) },
                onOpenSite = { siteId -> navController.navigate(Routes.siteDetail(siteId)) },
                onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument(Routes.ARG_SITE_ID) { type = NavType.StringType }),
        ) {
            SiteDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { siteId -> navController.navigate(Routes.editSite(siteId)) },
                onDeleted = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.ADD_EDIT,
            arguments = listOf(
                navArgument(Routes.ARG_SITE_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            AddSiteScreen(onDone = { navController.popBackStack() })
        }

        composable(Routes.NOTIFICATIONS) {
            NotificationHistoryScreen(onBack = { navController.popBackStack() })
        }
    }
}
