package com.cardkeeper.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.cardkeeper.ui.carddetail.CardDetailScreen
import com.cardkeeper.ui.cardlist.CardListScreen
import com.cardkeeper.ui.scan.OcrReviewScreen
import com.cardkeeper.ui.scan.ScanScreen
import com.cardkeeper.ui.tags.TagManagerScreen

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = CardListRoute) {
        composable<CardListRoute> {
            CardListScreen(
                onCardClick = { cardId -> navController.navigate(CardDetailRoute(cardId)) },
                onScanClick = { navController.navigate(ScanRoute) },
                onTagManagerClick = { navController.navigate(TagManagerRoute) }
            )
        }
        composable<CardDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<CardDetailRoute>()
            CardDetailScreen(cardId = route.cardId, onBack = { navController.popBackStack() })
        }
        composable<ScanRoute> {
            ScanScreen(
                onPhotoReady = { navController.navigate(OcrReviewRoute) },
                onBack = { navController.popBackStack() }
            )
        }
        composable<OcrReviewRoute> {
            OcrReviewScreen(
                onSaved = { navController.popBackStack(CardListRoute, inclusive = false) },
                onBack = { navController.popBackStack() }
            )
        }
        composable<TagManagerRoute> {
            TagManagerScreen(onBack = { navController.popBackStack() })
        }
    }
}
