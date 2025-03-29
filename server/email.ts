import nodemailer from "nodemailer";
import { Booking, FreeSlot } from "@shared/schema";
import { storage } from "./storage";

// Configure nodemailer
const transporter = nodemailer.createTransport({
  host: process.env.SMTP_HOST || "smtp.example.com",
  port: parseInt(process.env.SMTP_PORT || "587"),
  secure: process.env.SMTP_SECURE === "true",
  auth: {
    user: process.env.SMTP_USER || "user@example.com",
    pass: process.env.SMTP_PASSWORD || "password"
  }
});

// Function to format date and time
function formatDateTime(date: Date) {
  return new Intl.DateTimeFormat('en-US', {
    weekday: 'long',
    year: 'numeric', 
    month: 'long', 
    day: 'numeric',
    hour: 'numeric',
    minute: 'numeric',
    hour12: true
  }).format(date);
}

// Send booking confirmation to customer
export async function sendBookingConfirmation(booking: Booking, freeSlot: FreeSlot) {
  try {
    // Get the owner of the free slot
    const owner = await storage.getUser(freeSlot.userId);
    if (!owner) {
      throw new Error(`User with ID ${freeSlot.userId} not found`);
    }
    
    // Format dates
    const startFormatted = formatDateTime(booking.startTime);
    const endFormatted = formatDateTime(booking.endTime);
    
    // Email to customer
    const customerEmail = {
      from: process.env.EMAIL_FROM || "calsync@example.com",
      to: booking.customerEmail,
      subject: `Booking Confirmation: ${freeSlot.name}`,
      html: `
        <div style="font-family: Arial, sans-serif; line-height: 1.6; max-width: 600px;">
          <h2>Booking Confirmation</h2>
          <p>Hello ${booking.customerName},</p>
          <p>Your booking for <strong>${freeSlot.name}</strong> has been confirmed.</p>
          
          <div style="background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin: 20px 0;">
            <h3 style="margin-top: 0;">Appointment Details</h3>
            <p><strong>Date and Time:</strong> ${startFormatted} - ${endFormatted}</p>
            <p><strong>With:</strong> ${owner.name || owner.username}</p>
            ${booking.notes ? `<p><strong>Notes:</strong> ${booking.notes}</p>` : ''}
          </div>
          
          <p>You can add this event to your calendar by using the attached calendar invitation.</p>
          <p>If you need to cancel or reschedule, please contact us directly.</p>
          
          <p>Thank you for using CalSync!</p>
        </div>
      `
      // In a production app, would also add ICS attachment
    };
    
    // Email to owner
    const ownerEmail = {
      from: process.env.EMAIL_FROM || "calsync@example.com",
      to: owner.email,
      subject: `New Booking: ${freeSlot.name}`,
      html: `
        <div style="font-family: Arial, sans-serif; line-height: 1.6; max-width: 600px;">
          <h2>New Booking Notification</h2>
          <p>Hello ${owner.name || owner.username},</p>
          <p>You have a new booking for <strong>${freeSlot.name}</strong>.</p>
          
          <div style="background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin: 20px 0;">
            <h3 style="margin-top: 0;">Appointment Details</h3>
            <p><strong>Date and Time:</strong> ${startFormatted} - ${endFormatted}</p>
            <p><strong>Customer:</strong> ${booking.customerName} (${booking.customerEmail})</p>
            ${booking.notes ? `<p><strong>Notes:</strong> ${booking.notes}</p>` : ''}
          </div>
          
          <p>This event has been added to your calendar.</p>
        </div>
      `
      // In a production app, would also add ICS attachment
    };
    
    // Send emails
    // In development, we'll just log them
    if (process.env.NODE_ENV === "production") {
      await transporter.sendMail(customerEmail);
      await transporter.sendMail(ownerEmail);
    } else {
      console.log("Would send customer email:", customerEmail);
      console.log("Would send owner email:", ownerEmail);
    }
    
    return true;
  } catch (error) {
    console.error("Error sending booking confirmation emails:", error);
    // Don't throw error to prevent booking failure
    return false;
  }
}
