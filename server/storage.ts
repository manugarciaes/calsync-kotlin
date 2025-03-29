import { users, calendars, events, freeSlots, bookings } from "@shared/schema";
import type { 
  User, InsertUser, Calendar, InsertCalendar, 
  Event, InsertEvent, FreeSlot, InsertFreeSlot,
  Booking, InsertBooking
} from "@shared/schema";
import session from "express-session";
import createMemoryStore from "memorystore";
import { randomBytes } from "crypto";

const MemoryStore = createMemoryStore(session);

export interface IStorage {
  // User methods
  getUser(id: number): Promise<User | undefined>;
  getUserByUsername(username: string): Promise<User | undefined>;
  getUserByEmail(email: string): Promise<User | undefined>;
  createUser(user: InsertUser): Promise<User>;
  
  // Calendar methods
  getCalendars(userId: number): Promise<Calendar[]>;
  getCalendar(id: number): Promise<Calendar | undefined>;
  createCalendar(calendar: InsertCalendar): Promise<Calendar>;
  updateCalendar(id: number, updates: Partial<InsertCalendar>): Promise<Calendar | undefined>;
  deleteCalendar(id: number): Promise<boolean>;
  
  // Event methods
  getEvents(calendarId: number): Promise<Event[]>;
  getEventsByCalendarIds(calendarIds: number[]): Promise<Event[]>;
  getEventsByTimeRange(calendarIds: number[], startTime: Date, endTime: Date): Promise<Event[]>;
  createEvent(event: InsertEvent): Promise<Event>;
  deleteEventsByCalendarId(calendarId: number): Promise<boolean>;
  
  // Free slot methods
  getFreeSlots(userId: number): Promise<FreeSlot[]>;
  getFreeSlot(id: number): Promise<FreeSlot | undefined>;
  getFreeSlotByShareUrl(shareUrl: string): Promise<FreeSlot | undefined>;
  createFreeSlot(freeSlot: InsertFreeSlot): Promise<FreeSlot>;
  updateFreeSlot(id: number, updates: Partial<InsertFreeSlot>): Promise<FreeSlot | undefined>;
  deleteFreeSlot(id: number): Promise<boolean>;
  
  // Booking methods
  getBookings(freeSlotId: number): Promise<Booking[]>;
  getBooking(id: number): Promise<Booking | undefined>;
  createBooking(booking: InsertBooking): Promise<Booking>;
  updateBookingStatus(id: number, status: string): Promise<Booking | undefined>;
  
  // Session store
  sessionStore: session.SessionStore;
}

export class MemStorage implements IStorage {
  private users: Map<number, User>;
  private calendars: Map<number, Calendar>;
  private events: Map<number, Event>;
  private freeSlots: Map<number, FreeSlot>;
  private bookings: Map<number, Booking>;
  sessionStore: session.SessionStore;
  private currentIds: {
    user: number;
    calendar: number;
    event: number;
    freeSlot: number;
    booking: number;
  };

  constructor() {
    this.users = new Map();
    this.calendars = new Map();
    this.events = new Map();
    this.freeSlots = new Map();
    this.bookings = new Map();
    this.sessionStore = new MemoryStore({
      checkPeriod: 86400000 // 24 hours
    });
    this.currentIds = {
      user: 1,
      calendar: 1,
      event: 1,
      freeSlot: 1,
      booking: 1
    };
  }

  // User methods
  async getUser(id: number): Promise<User | undefined> {
    return this.users.get(id);
  }

  async getUserByUsername(username: string): Promise<User | undefined> {
    return Array.from(this.users.values()).find(
      (user) => user.username === username
    );
  }

  async getUserByEmail(email: string): Promise<User | undefined> {
    return Array.from(this.users.values()).find(
      (user) => user.email === email
    );
  }

  async createUser(insertUser: InsertUser): Promise<User> {
    const id = this.currentIds.user++;
    const createdAt = new Date();
    const user: User = { ...insertUser, id, createdAt };
    this.users.set(id, user);
    return user;
  }

  // Calendar methods
  async getCalendars(userId: number): Promise<Calendar[]> {
    return Array.from(this.calendars.values()).filter(
      (calendar) => calendar.userId === userId
    );
  }

  async getCalendar(id: number): Promise<Calendar | undefined> {
    return this.calendars.get(id);
  }

  async createCalendar(insertCalendar: InsertCalendar): Promise<Calendar> {
    const id = this.currentIds.calendar++;
    const lastUpdated = new Date();
    const calendar: Calendar = { 
      ...insertCalendar, 
      id, 
      lastUpdated, 
      isActive: true, 
      lastSyncStatus: "pending" 
    };
    this.calendars.set(id, calendar);
    return calendar;
  }

  async updateCalendar(id: number, updates: Partial<InsertCalendar>): Promise<Calendar | undefined> {
    const calendar = this.calendars.get(id);
    if (!calendar) return undefined;
    
    const updatedCalendar: Calendar = { 
      ...calendar, 
      ...updates, 
      lastUpdated: new Date() 
    };
    this.calendars.set(id, updatedCalendar);
    return updatedCalendar;
  }

  async deleteCalendar(id: number): Promise<boolean> {
    return this.calendars.delete(id);
  }

  // Event methods
  async getEvents(calendarId: number): Promise<Event[]> {
    return Array.from(this.events.values()).filter(
      (event) => event.calendarId === calendarId
    );
  }

  async getEventsByCalendarIds(calendarIds: number[]): Promise<Event[]> {
    return Array.from(this.events.values()).filter(
      (event) => calendarIds.includes(event.calendarId)
    );
  }

  async getEventsByTimeRange(calendarIds: number[], startTime: Date, endTime: Date): Promise<Event[]> {
    return Array.from(this.events.values()).filter(
      (event) => 
        calendarIds.includes(event.calendarId) && 
        event.startTime <= endTime && 
        event.endTime >= startTime
    );
  }

  async createEvent(insertEvent: InsertEvent): Promise<Event> {
    const id = this.currentIds.event++;
    const createdAt = new Date();
    const updatedAt = new Date();
    const event: Event = { ...insertEvent, id, createdAt, updatedAt };
    this.events.set(id, event);
    return event;
  }

  async deleteEventsByCalendarId(calendarId: number): Promise<boolean> {
    const events = await this.getEvents(calendarId);
    events.forEach(event => {
      this.events.delete(event.id);
    });
    return true;
  }

  // Free slot methods
  async getFreeSlots(userId: number): Promise<FreeSlot[]> {
    return Array.from(this.freeSlots.values()).filter(
      (slot) => slot.userId === userId
    );
  }

  async getFreeSlot(id: number): Promise<FreeSlot | undefined> {
    return this.freeSlots.get(id);
  }

  async getFreeSlotByShareUrl(shareUrl: string): Promise<FreeSlot | undefined> {
    return Array.from(this.freeSlots.values()).find(
      (slot) => slot.shareUrl === shareUrl
    );
  }

  async createFreeSlot(insertFreeSlot: InsertFreeSlot): Promise<FreeSlot> {
    const id = this.currentIds.freeSlot++;
    const createdAt = new Date();
    const updatedAt = new Date();
    
    // If no shareUrl is provided, generate one
    if (!insertFreeSlot.shareUrl) {
      insertFreeSlot.shareUrl = randomBytes(8).toString('hex');
    }
    
    const freeSlot: FreeSlot = { 
      ...insertFreeSlot, 
      id, 
      isActive: true,
      createdAt, 
      updatedAt 
    };
    this.freeSlots.set(id, freeSlot);
    return freeSlot;
  }

  async updateFreeSlot(id: number, updates: Partial<InsertFreeSlot>): Promise<FreeSlot | undefined> {
    const freeSlot = this.freeSlots.get(id);
    if (!freeSlot) return undefined;
    
    const updatedFreeSlot: FreeSlot = { 
      ...freeSlot, 
      ...updates, 
      updatedAt: new Date() 
    };
    this.freeSlots.set(id, updatedFreeSlot);
    return updatedFreeSlot;
  }

  async deleteFreeSlot(id: number): Promise<boolean> {
    return this.freeSlots.delete(id);
  }

  // Booking methods
  async getBookings(freeSlotId: number): Promise<Booking[]> {
    return Array.from(this.bookings.values()).filter(
      (booking) => booking.freeSlotId === freeSlotId
    );
  }

  async getBooking(id: number): Promise<Booking | undefined> {
    return this.bookings.get(id);
  }

  async createBooking(insertBooking: InsertBooking): Promise<Booking> {
    const id = this.currentIds.booking++;
    const createdAt = new Date();
    const booking: Booking = { 
      ...insertBooking, 
      id, 
      status: "confirmed", 
      createdAt 
    };
    this.bookings.set(id, booking);
    return booking;
  }

  async updateBookingStatus(id: number, status: string): Promise<Booking | undefined> {
    const booking = this.bookings.get(id);
    if (!booking) return undefined;
    
    const updatedBooking: Booking = { ...booking, status };
    this.bookings.set(id, updatedBooking);
    return updatedBooking;
  }
}

export const storage = new MemStorage();
