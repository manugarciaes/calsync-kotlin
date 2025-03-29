package com.calsync.application.service

import com.calsync.domain.model.Booking
import com.calsync.domain.model.FreeSlot
import com.calsync.domain.model.User
import com.calsync.domain.repository.FreeSlotRepository
import com.calsync.domain.repository.UserRepository
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.slf4j.LoggerFactory
import java.time.format.DateTimeFormatter
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import java.util.Properties

/**
 * Service for sending email notifications
 */
class EmailService(
    private val userRepository: UserRepository,
    private val freeSlotRepository: FreeSlotRepository
) {
    private val logger = LoggerFactory.getLogger(EmailService::class.java)
    
    // Email configuration
    private val host = System.getenv("MAIL_HOST") ?: "smtp.gmail.com"
    private val port = System.getenv("MAIL_PORT")?.toIntOrNull() ?: 587
    private val username = System.getenv("MAIL_USERNAME") ?: ""
    private val password = System.getenv("MAIL_PASSWORD") ?: ""
    private val fromEmail = System.getenv("MAIL_FROM") ?: "calsync@example.com"
    private val fromName = System.getenv("MAIL_FROM_NAME") ?: "CalSync"
    
    /**
     * Send booking confirmation to the customer
     */
    suspend fun sendBookingConfirmationToCustomer(booking: Booking): Result<Boolean> {
        try {
            val freeSlot = freeSlotRepository.findById(booking.freeSlotId)
                ?: return Result.failure(IllegalArgumentException("Free slot not found: ${booking.freeSlotId}"))
                
            val subject = "Booking Confirmation: ${freeSlot.name}"
            
            val bodyText = generateBookingConfirmationEmail(booking, freeSlot)
            
            return sendEmail(booking.email, subject, bodyText)
        } catch (e: Exception) {
            logger.error("Error sending booking confirmation to customer", e)
            return Result.failure(e)
        }
    }
    
    /**
     * Send booking notification to the owner of the free slot
     */
    suspend fun sendBookingNotificationToOwner(booking: Booking): Result<Boolean> {
        try {
            val freeSlot = freeSlotRepository.findById(booking.freeSlotId)
                ?: return Result.failure(IllegalArgumentException("Free slot not found: ${booking.freeSlotId}"))
                
            val owner = userRepository.findById(freeSlot.userId)
                ?: return Result.failure(IllegalArgumentException("User not found: ${freeSlot.userId}"))
                
            val subject = "New Booking: ${freeSlot.name}"
            
            val bodyText = generateBookingNotificationEmail(booking, freeSlot, owner)
            
            return sendEmail(owner.email, subject, bodyText)
        } catch (e: Exception) {
            logger.error("Error sending booking notification to owner", e)
            return Result.failure(e)
        }
    }
    
    /**
     * Send booking cancellation notification
     */
    suspend fun sendBookingCancellationNotification(booking: Booking): Result<Boolean> {
        try {
            val freeSlot = freeSlotRepository.findById(booking.freeSlotId)
                ?: return Result.failure(IllegalArgumentException("Free slot not found: ${booking.freeSlotId}"))
                
            val subject = "Booking Cancelled: ${freeSlot.name}"
            
            val bodyText = generateCancellationEmail(booking, freeSlot)
            
            // Send to customer
            val customerResult = sendEmail(booking.email, subject, bodyText)
            if (customerResult.isFailure) {
                return customerResult
            }
            
            // Get owner and send to them as well
            val owner = userRepository.findById(freeSlot.userId)
                ?: return Result.failure(IllegalArgumentException("User not found: ${freeSlot.userId}"))
                
            return sendEmail(owner.email, "Booking Cancelled by Customer: ${freeSlot.name}", bodyText)
        } catch (e: Exception) {
            logger.error("Error sending cancellation notification", e)
            return Result.failure(e)
        }
    }
    
    /**
     * Generate email for booking confirmation
     */
    private fun generateBookingConfirmationEmail(booking: Booking, freeSlot: FreeSlot): String {
        val formattedStartTime = formatDateTime(booking.startTime, booking.timeZone)
        val formattedEndTime = formatDateTime(booking.endTime, booking.timeZone)
        
        return """
            Hello ${booking.name},
            
            Your booking has been confirmed!
            
            Booking Details:
            - Event: ${freeSlot.name}
            - Date and Time: $formattedStartTime - $formattedEndTime
            - Location: Online
            
            ${freeSlot.description?.let { "Description: $it\n\n" } ?: ""}
            
            If you need to cancel or reschedule, please use the link in your original confirmation.
            
            Thank you for booking with us!
            
            Regards,
            CalSync
        """.trimIndent()
    }
    
    /**
     * Generate email for booking notification to slot owner
     */
    private fun generateBookingNotificationEmail(booking: Booking, freeSlot: FreeSlot, owner: User): String {
        val formattedStartTime = formatDateTime(booking.startTime, booking.timeZone)
        val formattedEndTime = formatDateTime(booking.endTime, booking.timeZone)
        
        return """
            Hello ${owner.name},
            
            You have a new booking for "${freeSlot.name}"!
            
            Booking Details:
            - Customer: ${booking.name} (${booking.email})
            - Date and Time: $formattedStartTime - $formattedEndTime
            ${booking.notes?.let { "- Notes: $it" } ?: ""}
            
            You can manage this booking from your CalSync dashboard.
            
            Regards,
            CalSync
        """.trimIndent()
    }
    
    /**
     * Generate email for booking cancellation
     */
    private fun generateCancellationEmail(booking: Booking, freeSlot: FreeSlot): String {
        val formattedStartTime = formatDateTime(booking.startTime, booking.timeZone)
        val formattedEndTime = formatDateTime(booking.endTime, booking.timeZone)
        
        return """
            Hello,
            
            A booking has been cancelled.
            
            Cancelled Booking Details:
            - Event: ${freeSlot.name}
            - Date and Time: $formattedStartTime - $formattedEndTime
            - Customer: ${booking.name} (${booking.email})
            ${booking.cancelReason?.let { "- Cancellation Reason: $it" } ?: ""}
            
            This time slot is now available again for booking.
            
            Regards,
            CalSync
        """.trimIndent()
    }
    
    /**
     * Format a date and time for display in emails
     */
    private fun formatDateTime(instant: kotlinx.datetime.Instant, timezone: String): String {
        val localDateTime = instant.toLocalDateTime(TimeZone.of(timezone))
        val javaLocalDateTime = localDateTime.toJavaLocalDateTime()
        
        val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a (z)")
        return javaLocalDateTime.format(formatter)
    }
    
    /**
     * Send an email
     */
    private fun sendEmail(to: String, subject: String, body: String): Result<Boolean> {
        return try {
            // Skip sending emails if configuration is missing
            if (username.isEmpty() || password.isEmpty()) {
                logger.warn("Email configuration incomplete. Email not sent to $to")
                return Result.success(false)
            }
            
            // Setup mail server properties
            val properties = Properties()
            properties["mail.smtp.host"] = host
            properties["mail.smtp.port"] = port.toString()
            properties["mail.smtp.auth"] = "true"
            properties["mail.smtp.starttls.enable"] = "true"
            
            // Create session
            val session = Session.getInstance(properties, object : javax.mail.Authenticator() {
                override fun getPasswordAuthentication(): javax.mail.PasswordAuthentication {
                    return javax.mail.PasswordAuthentication(username, password)
                }
            })
            
            // Create message
            val message = MimeMessage(session)
            message.setFrom(InternetAddress(fromEmail, fromName))
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
            message.subject = subject
            message.setText(body)
            
            // Send message
            Transport.send(message)
            logger.info("Email sent successfully to $to")
            
            Result.success(true)
        } catch (e: MessagingException) {
            logger.error("Error sending email to $to", e)
            Result.failure(e)
        } catch (e: Exception) {
            logger.error("Unexpected error sending email to $to", e)
            Result.failure(e)
        }
    }
}