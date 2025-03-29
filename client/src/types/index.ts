import { Calendar as SelectCalendar, Event as SelectEvent, FreeSlot as SelectFreeSlot, Booking as SelectBooking } from "@shared/schema";

export interface Event extends SelectEvent {}
export interface Calendar extends SelectCalendar {}
export interface FreeSlot extends SelectFreeSlot {}
export interface Booking extends SelectBooking {}

export interface AvailableSlot {
  date: string;
  slots: {
    start: string;
    end: string;
  }[];
}

export interface StatCard {
  title: string;
  value: number | string;
  icon: React.ReactNode;
  color: string;
}
