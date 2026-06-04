package com.f1forhelp.ovo.menu

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.f1forhelp.ovo.menu.main.MenuMain

@Composable
fun AppNav() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MenuMain(navController)
        }

        //region Settings
        composable("notifications") {
            MenuNotifications(navController)
        }
        composable("calculations") {
            MenuCalculations(navController)
        }
        composable("import") {
            MenuImport(navController)
        }
        composable("backup") {
            MenuBackup(navController)
        }
        composable("viewData") {
            MenuViewData(navController)
        }
        //endregion
    }
}