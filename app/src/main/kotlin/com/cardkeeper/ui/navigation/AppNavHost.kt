package com.cardkeeper.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.cardkeeper.ui.carddetail.CardDetailScreen
import com.cardkeeper.ui.carddetail.CardDetailViewModel
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
        composable<CardDetailRoute> {
            val viewModel: CardDetailViewModel = hiltViewModel()
            val route = it.toRoute<CardDetailRoute>()
            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(route.cardId) {
                viewModel.loadCard(route.cardId)
            }

            LaunchedEffect(uiState.card) {
                if (uiState.card == null && !uiState.isLoading) {
                    navController.popBackStack()
                }
            }

            CardDetailScreen(
                cardId = route.cardId,
                onDeleted = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
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
                    onGalleryImageReady = { imagePath ->
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
