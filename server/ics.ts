import { Express, Request, Response } from "express";
import { storage } from "./storage";
import ical from "node-ical";
import { InsertEvent } from "@shared/schema";
import { z } from "zod";
import multer from "multer";
import fetch from "node-fetch";

// Multer configuration for file uploads
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 5 * 1024 * 1024 } // 5MB
});

export function setupIcsRoutes(app: Express) {
  // Route to sync a calendar from an ICS URL
  app.post("/api/calendars/:id/sync", async (req: Request, res: Response) => {
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
      
      // Handle different calendar sources
      if (calendar.source === "ics_url" && calendar.url) {
        await syncCalendarFromUrl(calendar.id, calendar.url);
        
        // Update calendar sync status
        await storage.updateCalendar(calendarId, {
          lastSyncStatus: "synced",
          lastUpdated: new Date()
        });
        
        res.json({ message: "Calendar synced successfully" });
      } else if (calendar.source === "ics_file" && calendar.fileData) {
        await syncCalendarFromData(calendar.id, calendar.fileData);
        
        // Update calendar sync status
        await storage.updateCalendar(calendarId, {
          lastSyncStatus: "synced",
          lastUpdated: new Date()
        });
        
        res.json({ message: "Calendar synced successfully" });
      } else {
        res.status(400).json({ message: "Calendar source not supported for manual sync" });
      }
    } catch (error) {
      console.error("Calendar sync error:", error);
      res.status(500).json({ message: "Failed to sync calendar", error: (error as Error).message });
    }
  });

  // Route to upload an ICS file
  app.post("/api/calendars/upload", upload.single("file"), async (req: Request, res: Response) => {
    if (!req.isAuthenticated()) return res.status(401).json({ message: "Unauthorized" });
    
    try {
      if (!req.file) {
        return res.status(400).json({ message: "No file uploaded" });
      }
      
      // Get the file content as string
      const fileData = req.file.buffer.toString("utf-8");
      
      // Parse calendar name from form data
      const calendarName = req.body.name || "Uploaded Calendar";
      
      // Create a new calendar
      const calendar = await storage.createCalendar({
        userId: req.user!.id,
        name: calendarName,
        source: "ics_file",
        fileData,
        color: req.body.color || "#2563eb",
      });
      
      // Sync calendar events
      await syncCalendarFromData(calendar.id, fileData);
      
      // Update calendar sync status
      await storage.updateCalendar(calendar.id, {
        lastSyncStatus: "synced",
        lastUpdated: new Date()
      });
      
      res.status(201).json(calendar);
    } catch (error) {
      console.error("ICS upload error:", error);
      res.status(500).json({ message: "Failed to upload and process ICS file", error: (error as Error).message });
    }
  });

  // Route to add a calendar from URL
  app.post("/api/calendars/url", async (req: Request, res: Response) => {
    if (!req.isAuthenticated()) return res.status(401).json({ message: "Unauthorized" });
    
    try {
      const { url, name, color } = req.body;
      
      if (!url || !name) {
        return res.status(400).json({ message: "URL and name are required" });
      }
      
      // Validate URL
      try {
        new URL(url);
      } catch (error) {
        return res.status(400).json({ message: "Invalid URL" });
      }
      
      // Create a new calendar
      const calendar = await storage.createCalendar({
        userId: req.user!.id,
        name,
        source: "ics_url",
        url,
        color: color || "#2563eb",
      });
      
      // Sync calendar events
      await syncCalendarFromUrl(calendar.id, url);
      
      // Update calendar sync status
      await storage.updateCalendar(calendar.id, {
        lastSyncStatus: "synced",
        lastUpdated: new Date()
      });
      
      res.status(201).json(calendar);
    } catch (error) {
      console.error("Add calendar from URL error:", error);
      res.status(500).json({ message: "Failed to add calendar from URL", error: (error as Error).message });
    }
  });
}

// Helper function to sync a calendar from a URL
async function syncCalendarFromUrl(calendarId: number, url: string) {
  try {
    // Fetch ICS data from URL
    const response = await fetch(url);
    if (!response.ok) {
      throw new Error(`Failed to fetch ICS data: ${response.statusText}`);
    }
    
    const icsData = await response.text();
    await syncCalendarFromData(calendarId, icsData);
  } catch (error) {
    console.error("Error syncing calendar from URL:", error);
    throw error;
  }
}

// Helper function to sync a calendar from ICS data
async function syncCalendarFromData(calendarId: number, icsData: string) {
  try {
    // Clear existing events for this calendar
    await storage.deleteEventsByCalendarId(calendarId);
    
    // Parse ICS data
    const parsedEvents = await ical.parseICS(icsData);
    
    // Process and store events
    for (const [uid, event] of Object.entries(parsedEvents)) {
      if (event.type !== 'VEVENT') continue;
      
      // Skip events without required properties
      if (!event.start || !event.end || !event.summary) continue;
      
      const newEvent: InsertEvent = {
        calendarId,
        uid,
        title: event.summary,
        startTime: event.start,
        endTime: event.end,
        location: event.location || "",
        description: event.description || "",
        isAllDay: Boolean(event.allDay),
        recurrenceRule: event.rrule?.toString() || ""
      };
      
      await storage.createEvent(newEvent);
    }
  } catch (error) {
    console.error("Error syncing calendar from data:", error);
    throw error;
  }
}
