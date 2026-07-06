package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SouqDao {

    // --- Users ---
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    suspend fun getUserByIdSuspend(uid: String): UserEntity?

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    fun getUserById(uid: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE role = 'TECHNICIAN'")
    fun getAllTechnicians(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE role = 'TECHNICIAN' AND profession = :profession")
    fun getTechniciansByProfession(profession: String): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)


    // --- Service Requests ---
    @Query("SELECT * FROM service_requests ORDER BY createdAt DESC")
    fun getAllRequests(): Flow<List<ServiceRequestEntity>>

    @Query("SELECT * FROM service_requests WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getRequestsByCustomer(customerId: String): Flow<List<ServiceRequestEntity>>

    @Query("SELECT * FROM service_requests WHERE techId = :techId ORDER BY createdAt DESC")
    fun getRequestsByTech(techId: String): Flow<List<ServiceRequestEntity>>

    @Query("SELECT * FROM service_requests WHERE id = :id LIMIT 1")
    fun getRequestById(id: Int): Flow<ServiceRequestEntity?>

    @Query("SELECT * FROM service_requests WHERE id = :id LIMIT 1")
    suspend fun getRequestByIdSuspend(id: Int): ServiceRequestEntity?

    @Query("SELECT * FROM service_requests WHERE status IN ('NEW', 'PENDING') AND serviceType = :profession ORDER BY createdAt DESC")
    fun getPendingRequestsForTech(profession: String): Flow<List<ServiceRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: ServiceRequestEntity): Long

    @Update
    suspend fun updateRequest(request: ServiceRequestEntity)


    // --- Neighborhood Posts ---
    @Query("SELECT * FROM neighborhood_posts ORDER BY createdAt DESC")
    fun getAllPosts(): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Update
    suspend fun updatePost(post: PostEntity)


    // --- Offers ---
    @Query("SELECT * FROM offers ORDER BY createdAt DESC")
    fun getAllOffers(): Flow<List<OfferEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffer(offer: OfferEntity)

    @Query("DELETE FROM offers WHERE id = :id")
    suspend fun deleteOfferById(id: Int)


    // --- Messages ---
    @Query("SELECT * FROM messages WHERE requestId = :requestId ORDER BY createdAt ASC")
    fun getMessagesForRequest(requestId: Int): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)


    // --- Notifications ---
    @Query("SELECT * FROM notifications WHERE recipientId = :recipientId ORDER BY createdAt DESC")
    fun getNotificationsForUser(recipientId: String): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE recipientId = :recipientId")
    suspend fun markNotificationsAsRead(recipientId: String)
}
