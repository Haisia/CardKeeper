package com.cardkeeper.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.cardkeeper.ui.carddetail.CardDetailScreen
import com.cardkeeper.ui.cardlist.CardListScreen
import com.cardkeeper.ui.scan.OcrReviewScreen
import com.cardkeeper.ui.scan.ScanScreen
import com.cardkeeper.ui.scan.ScanViewModel
import com.cardkeeper.ui.tags.TagManagerScreen
import androidx.hilt.navigation.compose.hiltViewModel

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
        composable<ScanRoute> {
            val viewModel: ScanViewModel = hiltViewModel()
            ScanScreen(
                viewModel = viewModel,
                onPhotoReady = {},
                onGalleryImageReady = { imagePath ->
                    viewModel.setTempImagePath(imagePath)
                    navController.navigate(OcrReviewRoute)
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable<OcrReviewRoute> {
            val navBackStackEntry = remember {
                navController.getBackStackEntry<ScanRoute>()
            }
            val viewModel: ScanViewModel = hiltViewModel(navBackStackEntry)
            OcrReviewScreen(
                viewModel = viewModel,
                onSaved = { navController.popBackStack(CardListRoute, inclusive = false) },
                onBack = { navController.popBackStack() }
            )
        }
        composable<CardDetailRoute> {
            val route = it.toRoute<CardDetailRoute>()
            CardDetailScreen(
                cardId = route.cardId,
                onDeleted = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable<TagManagerRoute> {
            TagManagerScreen(onBack = { navController.popBackStack() })
        }
    }
}
