package com.calsync.infrastructure.repository

import com.calsync.domain.model.User
import com.calsync.domain.model.UserId
import com.calsync.domain.repository.UserRepository
import com.calsync.infrastructure.database.UsersTable
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.util.UUID

/**
 * Implementation of UserRepository using Exposed
 */
class UserRepositoryImpl : UserRepository {
    
    override suspend fun findById(id: UserId): User? = dbQuery {
        UsersTable.select { UsersTable.id eq id.value }
            .singleOrNull()
            ?.toUser()
    }
    
    override suspend fun findByUsername(username: String): User? = dbQuery {
        UsersTable.select { UsersTable.username eq username }
            .singleOrNull()
            ?.toUser()
    }
    
    override suspend fun findByEmail(email: String): User? = dbQuery {
        UsersTable.select { UsersTable.email eq email }
            .singleOrNull()
            ?.toUser()
    }
    
    override suspend fun save(user: User): User = dbQuery {
        // Check if user exists
        val exists = UsersTable.select { UsersTable.id eq user.id.value }.singleOrNull() != null
        
        if (exists) {
            // Update existing user
            UsersTable.update({ UsersTable.id eq user.id.value }) { stmt ->
                stmt[UsersTable.username] = user.username
                stmt[UsersTable.email] = user.email
                stmt[UsersTable.password] = user.password
                stmt[UsersTable.name] = user.name
                stmt[UsersTable.updatedAt] = user.updatedAt.toJavaInstant()
            }
        } else {
            // Insert new user
            UsersTable.insert { stmt ->
                stmt[UsersTable.id] = user.id.value
                stmt[UsersTable.username] = user.username
                stmt[UsersTable.email] = user.email
                stmt[UsersTable.password] = user.password
                stmt[UsersTable.name] = user.name
                stmt[UsersTable.createdAt] = user.createdAt.toJavaInstant()
                stmt[UsersTable.updatedAt] = user.updatedAt.toJavaInstant()
            }
        }
        
        // Return updated user
        UsersTable.select { UsersTable.id eq user.id.value }
            .single()
            .toUser()
    }
    
    override suspend fun delete(id: UserId): Boolean = dbQuery {
        UsersTable.deleteWhere { UsersTable.id eq id.value } > 0
    }
    
    override suspend fun existsByUsername(username: String): Boolean = dbQuery {
        UsersTable.select { UsersTable.username eq username }
            .singleOrNull() != null
    }
    
    override suspend fun existsByEmail(email: String): Boolean = dbQuery {
        UsersTable.select { UsersTable.email eq email }
            .singleOrNull() != null
    }
    
    /**
     * Extension function to convert ResultRow to User domain entity
     */
    private fun ResultRow.toUser(): User {
        return User(
            id = UserId(this[UsersTable.id].toString()),
            username = this[UsersTable.username],
            email = this[UsersTable.email],
            password = this[UsersTable.password],
            name = this[UsersTable.name],
            createdAt = this[UsersTable.createdAt].toKotlinInstant(),
            updatedAt = this[UsersTable.updatedAt].toKotlinInstant()
        )
    }
    
    /**
     * Helper function to execute database operations in a transaction
     */
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}