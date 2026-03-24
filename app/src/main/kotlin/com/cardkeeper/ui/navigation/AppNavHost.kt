package com.cardkeeper.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.cardkeeper.ui.carddetail.CardDetailScreen
import com.cardkeeper.ui.cardlist.CardListScreen
import com.cardkeeper.ui.scan.OcrReviewScreen
import com.cardkeeper.ui.scan.ScanScreen
import com.cardkeeper.ui.scan.ScanViewModel
import com.cardkeeper.ui.tags.TagManagerScreen

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = CardListRoute) {
        composable<CardListRoute> {
            CardListScreen(
                onCardClick = { cardId -> navController.navigate(CardDetailRoute(cardId)) },
                onScanClick = { navController.navigate(ScanFlowRoute) },
                onTagManagerClick = { navController.navigate(TagManagerRoute) }
            )
        }
        composable<CardDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<CardDetailRoute>()
            CardDetailScreen(cardId = route.cardId, onBack = { navController.popBackStack() })
        }
        navigation<ScanFlowRoute>(startDestination = ScanRoute) {
            composable<ScanRoute> {
                val viewModel: ScanViewModel = hiltViewModel(
                    navController.getBackStackEntry<ScanFlowRoute>()
                )
                ScanScreen(
                    viewModel = viewModel,
                    onPhotoReady = { imagePath ->
                        viewModel.setTempImagePath(imagePath)
                        navController.navigate(OcrReviewRoute)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable<OcrReviewRoute> {
                val viewModel: ScanViewModel = hiltViewModel(
                    navController.getBackStackEntry<ScanFlowRoute>()
                )
                OcrReviewScreen(
                    viewModel = viewModel,
                    onSaved = { navController.popBackStack(CardListRoute, inclusive = false) },
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable<TagManagerRoute> {
            TagManagerScreen(onBack = { navController.popBackStack() })
        }
    }
}
