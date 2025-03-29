import { pgTable, text, serial, integer, boolean, timestamp, jsonb } from "drizzle-orm/pg-core";
import { createInsertSchema } from "drizzle-zod";
import { z } from "zod";

export const users = pgTable("users", {
  id: serial("id").primaryKey(),
  username: text("username").notNull().unique(),
  password: text("password").notNull(),
  email: text("email").notNull().unique(),
  name: text("name"),
  createdAt: timestamp("created_at").defaultNow().notNull(),
});

export const calendars = pgTable("calendars", {
  id: serial("id").primaryKey(),
  userId: integer("user_id").notNull().references(() => users.id),
  name: text("name").notNull(),
  source: text("source").notNull(), // "google", "outlook", "ics_url", "ics_file"
  url: text("url"), // URL for ICS calendar
  fileData: text("file_data"), // For uploaded ICS files
  color: text("color").notNull().default("#2563eb"),
  isActive: boolean("is_active").notNull().default(true),
  lastUpdated: timestamp("last_updated").defaultNow().notNull(),
  lastSyncStatus: text("last_sync_status").default("pending"),
});

export const events = pgTable("events", {
  id: serial("id").primaryKey(),
  calendarId: integer("calendar_id").notNull().references(() => calendars.id),
  uid: text("uid").notNull(),
  title: text("title").notNull(),
  startTime: timestamp("start_time").notNull(),
  endTime: timestamp("end_time").notNull(),
  location: text("location"),
  description: text("description"),
  isAllDay: boolean("is_all_day").default(false),
  recurrenceRule: text("recurrence_rule"),
  createdAt: timestamp("created_at").defaultNow().notNull(),
  updatedAt: timestamp("updated_at").defaultNow().notNull(),
});

export const freeSlots = pgTable("free_slots", {
  id: serial("id").primaryKey(),
  userId: integer("user_id").notNull().references(() => users.id),
  name: text("name").notNull(),
  calendarIds: text("calendar_ids").notNull(), // Comma-separated list of calendar IDs
  slotDuration: integer("slot_duration").notNull(), // in minutes
  bufferTime: integer("buffer_time").notNull().default(0), // in minutes
  startTime: text("start_time").notNull(), // Format: HH:MM
  endTime: text("end_time").notNull(), // Format: HH:MM
  availableDays: text("available_days").notNull(), // Format: "0,1,2,3,4,5,6" (0=Sunday)
  dateRange: jsonb("date_range").$type<{ start: string; end: string }>(),
  isActive: boolean("is_active").notNull().default(true),
  shareUrl: text("share_url").notNull().unique(),
  createdAt: timestamp("created_at").defaultNow().notNull(),
  updatedAt: timestamp("updated_at").defaultNow().notNull(),
});

export const bookings = pgTable("bookings", {
  id: serial("id").primaryKey(),
  freeSlotId: integer("free_slot_id").notNull().references(() => freeSlots.id),
  customerName: text("customer_name").notNull(),
  customerEmail: text("customer_email").notNull(),
  startTime: timestamp("start_time").notNull(),
  endTime: timestamp("end_time").notNull(),
  status: text("status").notNull().default("confirmed"), // confirmed, canceled, completed
  notes: text("notes"),
  createdAt: timestamp("created_at").defaultNow().notNull(),
});

// Insert schemas
export const insertUserSchema = createInsertSchema(users).pick({
  username: true,
  password: true,
  email: true,
  name: true,
});

export const insertCalendarSchema = createInsertSchema(calendars).pick({
  userId: true,
  name: true,
  source: true,
  url: true,
  fileData: true,
  color: true,
});

export const insertEventSchema = createInsertSchema(events).pick({
  calendarId: true,
  uid: true,
  title: true,
  startTime: true,
  endTime: true,
  location: true,
  description: true,
  isAllDay: true,
  recurrenceRule: true,
});

export const insertFreeSlotSchema = createInsertSchema(freeSlots).pick({
  userId: true,
  name: true,
  calendarIds: true,
  slotDuration: true,
  bufferTime: true,
  startTime: true,
  endTime: true,
  availableDays: true,
  dateRange: true,
  shareUrl: true,
});

export const insertBookingSchema = createInsertSchema(bookings).pick({
  freeSlotId: true,
  customerName: true,
  customerEmail: true,
  startTime: true,
  endTime: true,
  notes: true,
});

// Types for TypeScript
export type User = typeof users.$inferSelect;
export type InsertUser = z.infer<typeof insertUserSchema>;

export type Calendar = typeof calendars.$inferSelect;
export type InsertCalendar = z.infer<typeof insertCalendarSchema>;

export type Event = typeof events.$inferSelect;
export type InsertEvent = z.infer<typeof insertEventSchema>;

export type FreeSlot = typeof freeSlots.$inferSelect;
export type InsertFreeSlot = z.infer<typeof insertFreeSlotSchema>;

export type Booking = typeof bookings.$inferSelect;
export type InsertBooking = z.infer<typeof insertBookingSchema>;

// Extended schemas for validation
export const bookSlotSchema = z.object({
  freeSlotId: z.number(),
  startTime: z.string(),
  endTime: z.string(),
  customerName: z.string().min(2),
  customerEmail: z.string().email(),
  notes: z.string().optional(),
});

export type BookSlotInput = z.infer<typeof bookSlotSchema>;
