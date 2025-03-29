import schedule from "node-schedule";
import { storage } from "./storage";
import { syncCalendarFromUrl } from "./ics"; // Import the function from ics.ts

// Default sync interval in minutes
const DEFAULT_SYNC_INTERVAL = 60;

export function setupScheduler() {
  // Get sync interval from environment variable or use default
  const syncIntervalMinutes = parseInt(process.env.CALENDAR_SYNC_INTERVAL || `${DEFAULT_SYNC_INTERVAL}`);
  
  console.log(`Setting up background sync every ${syncIntervalMinutes} minutes`);
  
  // Schedule calendar sync job
  schedule.scheduleJob(`*/${syncIntervalMinutes} * * * *`, async () => {
    try {
      console.log(`Starting scheduled calendar sync at ${new Date().toISOString()}`);
      
      // Get all calendars with URLs
      const allUsers = Array.from((storage as any).users.values()); // Type assertion to access private property
      
      for (const user of allUsers) {
        // Get all calendars for this user
        const calendars = await storage.getCalendars(user.id);
        
        // Filter to only URL-based calendars
        const urlCalendars = calendars.filter(cal => cal.source === "ics_url" && cal.url && cal.isActive);
        
        // Sync each calendar
        for (const calendar of urlCalendars) {
          try {
            console.log(`Syncing calendar ${calendar.id} (${calendar.name}) for user ${user.id}`);
            
            // Update calendar status to "syncing"
            await storage.updateCalendar(calendar.id, { lastSyncStatus: "syncing" });
            
            // Sync calendar
            await syncCalendarFromUrl(calendar.id, calendar.url!);
            
            // Update calendar status to "synced"
            await storage.updateCalendar(calendar.id, { 
              lastSyncStatus: "synced",
              lastUpdated: new Date()
            });
            
            console.log(`Calendar ${calendar.id} synced successfully`);
          } catch (error) {
            console.error(`Error syncing calendar ${calendar.id}:`, error);
            
            // Update calendar status to "error"
            await storage.updateCalendar(calendar.id, { 
              lastSyncStatus: "error",
              lastUpdated: new Date()
            });
          }
        }
      }
      
      console.log(`Finished scheduled calendar sync at ${new Date().toISOString()}`);
    } catch (error) {
      console.error("Error in calendar sync job:", error);
    }
  });
  
  return true;
}

// Helper function to sync a calendar from a URL
export async function syncCalendarFromUrl(calendarId: number, url: string) {
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
    // In a real implementation, this would parse the ICS data and update the events in the database
    console.log(`Syncing calendar ${calendarId} with data length: ${icsData.length}`);
    
    // For simplicity, we'll just log that we would sync the calendar
    console.log(`Calendar ${calendarId} would be synced with ${icsData.length} bytes of ICS data`);
    
    return true;
  } catch (error) {
    console.error("Error syncing calendar from data:", error);
    throw error;
  }
}
