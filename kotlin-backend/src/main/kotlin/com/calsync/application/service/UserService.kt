package com.calsync.application.service

import com.calsync.domain.model.User
import com.calsync.domain.model.UserId
import com.calsync.domain.repository.UserRepository
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory

/**
 * Service for user management
 */
class UserService(
    private val userRepository: UserRepository
) {
    private val logger = LoggerFactory.getLogger(UserService::class.java)
    
    /**
     * Register a new user
     */
    suspend fun registerUser(request: RegisterRequest): Result<User> {
        try {
            // Check if username already exists
            if (userRepository.existsByUsername(request.username)) {
                return Result.failure(IllegalArgumentException("Username already exists"))
            }
            
            // Check if email already exists
            if (userRepository.existsByEmail(request.email)) {
                return Result.failure(IllegalArgumentException("Email already exists"))
            }
            
            // Hash password
            val hashedPassword = BCrypt.hashpw(request.password, BCrypt.gensalt())
            
            // Create user
            val user = User(
                id = UserId.generate(),
                username = request.username,
                email = request.email,
                password = hashedPassword,
                name = request.name
            )
            
            // Save user
            val savedUser = userRepository.save(user)
            return Result.success(savedUser)
        } catch (e: Exception) {
            logger.error("Error registering user", e)
            return Result.failure(e)
        }
    }
    
    /**
     * Authenticate a user
     */
    suspend fun authenticateUser(username: String, password: String): Result<User> {
        try {
            // Find user by username
            val user = userRepository.findByUsername(username)
                ?: return Result.failure(IllegalArgumentException("Invalid username or password"))
            
            // Check password
            if (!BCrypt.checkpw(password, user.password)) {
                return Result.failure(IllegalArgumentException("Invalid username or password"))
            }
            
            return Result.success(user)
        } catch (e: Exception) {
            logger.error("Error authenticating user", e)
            return Result.failure(e)
        }
    }
    
    /**
     * Get user by ID
     */
    suspend fun getUser(id: UserId): Result<User> {
        return try {
            val user = userRepository.findById(id)
                ?: return Result.failure(IllegalArgumentException("User not found: $id"))
            
            Result.success(user)
        } catch (e: Exception) {
            logger.error("Error retrieving user: $id", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update user profile
     */
    suspend fun updateUser(id: UserId, request: UpdateUserRequest): Result<User> {
        try {
            // Get existing user
            val existingUser = userRepository.findById(id)
                ?: return Result.failure(IllegalArgumentException("User not found: $id"))
            
            // Check if email is being changed and already exists
            if (request.email != null && request.email != existingUser.email &&
                userRepository.existsByEmail(request.email)) {
                return Result.failure(IllegalArgumentException("Email already exists"))
            }
            
            // Create updated user
            val updatedUser = existingUser.copy(
                name = request.name ?: existingUser.name,
                email = request.email ?: existingUser.email,
                password = if (request.password != null) {
                    BCrypt.hashpw(request.password, BCrypt.gensalt())
                } else {
                    existingUser.password
                }
            )
            
            // Save updated user
            val savedUser = userRepository.save(updatedUser)
            return Result.success(savedUser)
        } catch (e: Exception) {
            logger.error("Error updating user: $id", e)
            return Result.failure(e)
        }
    }
    
    /**
     * Delete user
     */
    suspend fun deleteUser(id: UserId): Result<Boolean> {
        return try {
            val success = userRepository.delete(id)
            if (!success) {
                return Result.failure(IllegalArgumentException("Failed to delete user: $id"))
            }
            Result.success(true)
        } catch (e: Exception) {
            logger.error("Error deleting user: $id", e)
            Result.failure(e)
        }
    }
}

/**
 * Data class for user registration
 */
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val name: String
)

/**
 * Data class for user profile updates
 */
data class UpdateUserRequest(
    val name: String? = null,
    val email: String? = null,
    val password: String? = null
)