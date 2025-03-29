import type { Express, Request, Response } from "express";
import { createServer, type Server } from "http";
import { storage } from "./storage";
import { setupAuth } from "./auth";
import { setupIcsRoutes } from "./ics";
import { setupScheduler } from "./scheduler";
import { z } from "zod";
import {
  insertCalendarSchema,
  insertFreeSlotSchema,
  bookSlotSchema,
  Calendar,
  FreeSlot,
  Booking
} from "@shared/schema";
import { randomBytes } from "crypto";
import { sendBookingConfirmation } from "./email";

export async function registerRoutes(app: Express): Promise<Server> {
  // Initialize authentication routes
  setupAuth(app);
  
  // Initialize ICS routes
  setupIcsRoutes(app);
  
  // Start background task scheduler
  setupScheduler();

  // Calendar API routes
  app.get("/api/calendars", async (req: Request, res: Response) => {
    if (!req.isAuthenticated()) return res.status(401).json({ message: "Unauthorized" });
    
    try {
      const userId = req.user!.id;
      const calendars = await storage.getCalendars(userId);
      res.json(calendars);
    } catch (error) {
      res.status(500).json({ message: "Failed to fetch calendars", error: (error as Error).message });
    }
  });

  app.post("/api/calendars", async (req: Request, res: Response) => {
    if (!req.isAuthenticated()) return res.status(401).json({ message: "Unauthorized" });
    
    try {
      const userId = req.user!.id;
      const validatedData = insertCalendarSchema.parse({
        ...req.body,
        userId
      });
      
      const calendar = await storage.createCalendar(validatedData);
      res.status(201).json(calendar);
    } catch (error) {
      if (error instanceof z.ZodError) {
        return res.status(400).json({ message: "Validation error", errors: error.errors });
      }
      res.status(500).json({ message: "Failed to create calendar", error: (error as Error).message });
    }
  });

  app.put("/api/calendars/:id", async (req: Request, res: Response) => {
    if (!req.isAuthenticated()) return res.status(401).json({ message: "Unauthorized" });
    
    try {
      const calendarId = parseInt(req.params.id);
      const calendar = await storage.getCalendar(calendarId);
      
      if (!calendar) {
        return res.status(404).json({ message: "Calendar not found" });
      }
      
      if (calendar.userId !== req.user!.id) {
        return res.status(403).json({ message: "Forbidden" });
      }
      
      const updates = req.body;
      const updatedCalendar = await storage.updateCalendar(calendarId, updates);
      res.json(updatedCalendar);
    } catch (error) {
      res.status(500).json({ message: "Failed to update calendar", error: (error as Error).message });
    }
  });

  app.delete("/api/calendars/:id", async (req: Request, res: Response) => {
    if (!req.isAuthenticated()) return res.status(401).json({ message: "Unauthorized" });
    
    try {
      const calendarId = parseInt(req.params.id);
      const calendar = await storage.getCalendar(calendarId);
      
      if (!calendar) {
        return res.status(404).json({ message: "Calendar not found" });
      }
      
      if (calendar.userId !== req.user!.id) {
        return res.status(403).json({ message: "Forbidden" });
      }
      
      // Delete all events for this calendar
      await storage.deleteEventsByCalendarId(calendarId);
      
      // Delete the calendar
      await storage.deleteCalendar(calendarId);
      res.status(204).send();
    } catch (error) {
      res.status(500).json({ message: "Failed to delete calendar", error: (error as Error).message });
    }
  });

  // Free Slots API routes
  app.get("/api/free-slots", async (req: Request, res: Response) => {
    if (!req.isAuthenticated()) return res.status(401).json({ message: "Unauthorized" });
    
    try {
      const userId = req.user!.id;
      const freeSlots = await storage.getFreeSlots(userId);
      res.json(freeSlots);
    } catch (error) {
      res.status(500).json({ message: "Failed to fetch free slots", error: (error as Error).message });
    }
  });

  app.get("/api/free-slots/:id", async (req: Request, res: Response) => {
    if (!req.isAuthenticated()) return res.status(401).json({ message: "Unauthorized" });
    
    try {
      const slotId = parseInt(req.params.id);
      const freeSlot = await storage.getFreeSlot(slotId);
      
      if (!freeSlot) {
        return res.status(404).json({ message: "Free slot not found" });
      }
      
      if (freeSlot.userId !== req.user!.id) {
        return res.status(403).json({ message: "Forbidden" });
      }
      
      res.json(freeSlot);
    } catch (error) {
      res.status(500).json({ message: "Failed to fetch free slot", error: (error as Error).message });
    }
  });

  app.post("/api/free-slots", async (req: Request, res: Response) => {
    if (!req.isAuthenticated()) return res.status(401).json({ message: "Unauthorized" });
    
    try {
      const userId = req.user!.id;
      const shareUrl = randomBytes(8).toString('hex');
      
      const validatedData = insertFreeSlotSchema.parse({
        ...req.body,
        userId,
        shareUrl
      });
      
      const freeSlot = await storage.createFreeSlot(validatedData);
      res.status(201).json(freeSlot);
    } catch (error) {
      if (error instanceof z.ZodError) {
        return res.status(400).json({ message: "Validation error", errors: error.errors });
      }
      res.status(500).json({ message: "Failed to create free slot", error: (error as Error).message });
    }
  });

  app.put("/api/free-slots/:id", async (req: Request, res: Response) => {
    if (!req.isAuthenticated()) return res.status(401).json({ message: "Unauthorized" });
    
    try {
      const slotId = parseInt(req.params.id);
      const freeSlot = await storage.getFreeSlot(slotId);
      
      if (!freeSlot) {
        return res.status(404).json({ message: "Free slot not found" });
      }
      
      if (freeSlot.userId !== req.user!.id) {
        return res.status(403).json({ message: "Forbidden" });
      }
      
      const updates = req.body;
      const updatedFreeSlot = await storage.updateFreeSlot(slotId, updates);
      res.json(updatedFreeSlot);
    } catch (error) {
      res.status(500).json({ message: "Failed to update free slot", error: (error as Error).message });
    }
  });

  app.delete("/api/free-slots/:id", async (req: Request, res: Response) => {
    if (!req.isAuthenticated()) return res.status(401).json({ message: "Unauthorized" });
    
    try {
      const slotId = parseInt(req.params.id);
      const freeSlot = await storage.getFreeSlot(slotId);
      
      if (!freeSlot) {
        return res.status(404).json({ message: "Free slot not found" });
      }
      
      if (freeSlot.userId !== req.user!.id) {
        return res.status(403).json({ message: "Forbidden" });
      }
      
      await storage.deleteFreeSlot(slotId);
      res.status(204).send();
    } catch (error) {
      res.status(500).json({ message: "Failed to delete free slot", error: (error as Error).message });
    }
  });

  // Public API routes (no authentication required)
  app.get("/api/public/slots/:shareUrl", async (req: Request, res: Response) => {
    try {
      const { shareUrl } = req.params;
      const freeSlot = await storage.getFreeSlotByShareUrl(shareUrl);
      
      if (!freeSlot || !freeSlot.isActive) {
        return res.status(404).json({ message: "Free slot not found or inactive" });
      }
      
      res.json(freeSlot);
    } catch (error) {
      res.status(500).json({ message: "Failed to fetch free slot", error: (error as Error).message });
    }
  });

  app.get("/api/public/slots/:shareUrl/available-times", async (req: Request, res: Response) => {
    try {
      const { shareUrl } = req.params;
      const freeSlot = await storage.getFreeSlotByShareUrl(shareUrl);
      
      if (!freeSlot || !freeSlot.isActive) {
        return res.status(404).json({ message: "Free slot not found or inactive" });
      }
      
      // Parse date range
      const startDate = req.query.start ? new Date(req.query.start as string) : new Date();
      const endDate = req.query.end ? new Date(req.query.end as string) : new Date(startDate.getTime() + 7 * 24 * 60 * 60 * 1000); // Default 7 days from start
      
      // Parse calendar IDs
      const calendarIds = freeSlot.calendarIds.split(',').map(id => parseInt(id));
      
      // Get all events in the date range from the specified calendars
      const events = await storage.getEventsByTimeRange(calendarIds, startDate, endDate);
      
      // Get all bookings for this free slot
      const bookings = await storage.getBookings(freeSlot.slotId);
      
      // Create array of busy times (events + bookings + buffer times)
      const busyTimes = [
        ...events.map(event => ({ start: event.startTime, end: event.endTime })),
        ...bookings
          .filter(booking => booking.status === "confirmed")
          .map(booking => ({ start: booking.startTime, end: booking.endTime }))
      ];
      
      // Calculate available slots based on configuration and busy times
      // This is a simplified implementation - would need more logic for a production app
      const availableSlots = calculateAvailableSlots(freeSlot, busyTimes, startDate, endDate);
      
      res.json(availableSlots);
    } catch (error) {
      res.status(500).json({ message: "Failed to calculate available times", error: (error as Error).message });
    }
  });

  app.post("/api/public/book", async (req: Request, res: Response) => {
    try {
      const bookingData = bookSlotSchema.parse(req.body);
      const { freeSlotId, startTime, endTime, customerName, customerEmail, notes } = bookingData;
      
      // Verify the free slot exists and is active
      const freeSlot = await storage.getFreeSlot(freeSlotId);
      if (!freeSlot || !freeSlot.isActive) {
        return res.status(404).json({ message: "Free slot not found or inactive" });
      }
      
      // Verify the time slot is available (simplified)
      const startDateTime = new Date(startTime);
      const endDateTime = new Date(endTime);
      
      // Create the booking
      const booking = await storage.createBooking({
        freeSlotId,
        customerName,
        customerEmail,
        startTime: startDateTime,
        endTime: endDateTime,
        notes: notes || ""
      });
      
      // Send confirmation email
      await sendBookingConfirmation(booking, freeSlot);
      
      res.status(201).json(booking);
    } catch (error) {
      if (error instanceof z.ZodError) {
        return res.status(400).json({ message: "Validation error", errors: error.errors });
      }
      res.status(500).json({ message: "Failed to book slot", error: (error as Error).message });
    }
  });

  const httpServer = createServer(app);
  return httpServer;
}

// Helper function to calculate available slots
function calculateAvailableSlots(
  freeSlot: FreeSlot, 
  busyTimes: { start: Date, end: Date }[], 
  startDate: Date, 
  endDate: Date
): { date: string, slots: { start: string, end: string }[] }[] {
  // This is a simplified implementation that would need to be expanded for a real app
  const result: { date: string, slots: { start: string, end: string }[] }[] = [];
  
  // Parse available days
  const availableDays = freeSlot.availableDays.split(',').map(day => parseInt(day));
  
  // Parse time bounds
  const [startHour, startMinute] = freeSlot.startTime.split(':').map(part => parseInt(part));
  const [endHour, endMinute] = freeSlot.endTime.split(':').map(part => parseInt(part));
  
  // Slot duration in milliseconds
  const slotDurationMs = freeSlot.slotDuration * 60 * 1000;
  
  // Buffer time in milliseconds
  const bufferTimeMs = freeSlot.bufferTime * 60 * 1000;
  
  // Iterate over each day in the range
  const currentDate = new Date(startDate);
  while (currentDate <= endDate) {
    // Check if this day of week is available
    const dayOfWeek = currentDate.getDay(); // 0 = Sunday, 1 = Monday, etc.
    
    if (availableDays.includes(dayOfWeek)) {
      // Create date string in YYYY-MM-DD format
      const dateStr = currentDate.toISOString().split('T')[0];
      
      // Set start and end times for this day
      const dayStart = new Date(currentDate);
      dayStart.setHours(startHour, startMinute, 0, 0);
      
      const dayEnd = new Date(currentDate);
      dayEnd.setHours(endHour, endMinute, 0, 0);
      
      // Generate all possible slots for this day
      const daySlots: { start: string, end: string }[] = [];
      
      let slotStart = new Date(dayStart);
      while (slotStart.getTime() + slotDurationMs <= dayEnd.getTime()) {
        const slotEnd = new Date(slotStart.getTime() + slotDurationMs);
        
        // Check if this slot overlaps with any busy time
        const isOverlapping = busyTimes.some(busy => {
          const bufferStart = new Date(slotStart.getTime() - bufferTimeMs);
          const bufferEnd = new Date(slotEnd.getTime() + bufferTimeMs);
          return (
            (bufferStart < busy.end && bufferEnd > busy.start) // Slot + buffer overlaps with busy time
          );
        });
        
        if (!isOverlapping) {
          daySlots.push({
            start: slotStart.toISOString(),
            end: slotEnd.toISOString()
          });
        }
        
        // Move to next slot
        slotStart = new Date(slotStart.getTime() + slotDurationMs + bufferTimeMs);
      }
      
      if (daySlots.length > 0) {
        result.push({
          date: dateStr,
          slots: daySlots
        });
      }
    }
    
    // Move to next day
    currentDate.setDate(currentDate.getDate() + 1);
  }
  
  return result;
}
