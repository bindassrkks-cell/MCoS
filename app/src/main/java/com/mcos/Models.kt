package com.mcos

import java.io.File

data class ArticlePost(
    val id: String = "",
    val title: String = "",
    val summary: String = "",
    val htmlContent: String = "",
    val imageUrl: String = "",
    val videoUrl: String = "",
    val category: String = "TECH",
    val timeAgo: String = "Just now",
    val readTime: String = "3 min",
    val author: String = "MCoS Kernel"
)

data class AdminAdOffer(
    val id: String = "ad_1",
    val title: String = "Sponsored Video Ad",
    val description: String = "Watch to earn reward coins",
    val rewardCoins: Int = 50,
    val durationSec: Int = 10,
    val skipAfterSec: Int = 5,
    val bannerUrl: String = "",
    val targetUrl: String = "",
    val type: String = "VIDEO_AD"
)

data class WithdrawalRequest(
    val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val paymentType: String = "UPI",
    val paymentAddress: String = "",
    val coinsDebited: Int = 0,
    val amountInInr: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING"
)

data class EncryptedVaultItem(
    val file: File,
    val originalName: String,
    val formattedSize: String,
    val type: String
)
