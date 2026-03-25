package com.cardkeeper.ui.navigation

import kotlinx.serialization.Serializable

@Serializable object CardListRoute
@Serializable data class CardDetailRoute(val cardId: Long)
@Serializable object ScanRoute
@Serializable object ScanFlowRoute  // nested nav graph wrapper for scan flow
@Serializable object OcrReviewRoute
@Serializable object TagManagerRoute
