package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val phoneNumber: String,
    val role: String, // "CUSTOMER", "TECHNICIAN", "ADMIN"
    val city: String,
    val neighborhood: String,
    val lat: Double,
    val lng: Double,
    val avatarColor: Int, // Index of predefined colors for visual flair
    val bio: String = "",
    val profession: String = "", // "سباكة", "كهرباء", etc.
    val experienceYears: Int = 0,
    val isOnline: Boolean = true,
    val rating: Double = 4.5,
    val completedOrders: Int = 0,
    val avatarUri: String? = null
)

@Entity(tableName = "service_requests")
data class ServiceRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val techId: String? = null,
    val techName: String? = null,
    val techPhone: String? = null,
    val serviceType: String,
    val description: String,
    val status: String, // "NEW", "PENDING", "ACCEPTED", "ON_WAY", "STARTED", "COMPLETED", "CANCELLED"
    val urgency: String, // "عاجل", "متوسط", "عادي"
    val timeRequired: String, // e.g. "في أقرب وقت", "اليوم مساءً"
    val neighborhood: String,
    val lat: Double,
    val lng: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val ratingStars: Int = 0,
    val ratingComment: String? = null
)

@Entity(tableName = "neighborhood_posts")
data class PostEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val authorId: String,
    val authorName: String,
    val authorRole: String, // "CUSTOMER" or "TECHNICIAN" or "ADMIN"
    val authorAvatarColor: Int,
    val content: String,
    val neighborhood: String,
    val createdAt: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val authorAvatarUri: String? = null
)

@Entity(tableName = "offers")
data class OfferEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val techId: String,
    val techName: String,
    val techProfession: String,
    val techAvatarColor: Int,
    val title: String,
    val description: String,
    val price: String,
    val createdAt: Long = System.currentTimeMillis(),
    val techAvatarUri: String? = null
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val requestId: Int,
    val senderId: String,
    val senderName: String,
    val content: String,
    val type: String = "TEXT", // "TEXT", "LOCATION", "IMAGE_PLACEHOLDER"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val recipientId: String,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "jobs")
data class JobEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String, // "صيانة", "توصيل", "تعليم", "مساعدة في الحي", "أخرى"
    val description: String,
    val postedBy: String,
    val posterId: String,
    val posterPhone: String,
    val payment: String,
    val neighborhood: String,
    val jobType: String, // "دوام جزئي", "عمل حر / مؤقت", "مكافأة تقديرية"
    val createdAt: Long = System.currentTimeMillis(),
    val applicantsCount: Int = 0,
    val isAppliedByMe: Boolean = false,
    val posterAvatarUri: String? = null
)

@Entity(tableName = "channel_messages")
data class ChannelMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val channelId: String, // "news", "majlis", "maintenance", "market"
    val senderId: String,
    val senderName: String,
    val senderAvatarColor: Int,
    val senderRole: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val senderAvatarUri: String? = null
)

