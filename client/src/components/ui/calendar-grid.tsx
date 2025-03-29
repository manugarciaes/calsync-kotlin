import React, { useMemo } from "react";
import { 
  startOfWeek,
  endOfWeek,
  eachDayOfInterval,
  format,
  isSameDay,
  addHours,
  setHours,
  setMinutes,
  getHours,
  getMinutes,
  isWithinInterval,
} from "date-fns";
import { cn } from "@/lib/utils";
import { Event } from "@/types";

interface CalendarGridProps {
  date: Date;
  events: Event[];
  view: "day" | "week";
  hourStart?: number;
  hourEnd?: number;
  calendars: { id: number; name: string; color: string }[];
}

const hours = Array.from({ length: 24 }, (_, i) => i);

export function CalendarGrid({
  date,
  events,
  view = "day",
  hourStart = 8,
  hourEnd = 18,
  calendars
}: CalendarGridProps) {
  const days = useMemo(() => {
    if (view === "day") {
      return [date];
    } else {
      const start = startOfWeek(date, { weekStartsOn: 1 }); // start on Monday
      const end = endOfWeek(date, { weekStartsOn: 1 });
      return eachDayOfInterval({ start, end });
    }
  }, [date, view]);

  const visibleHours = useMemo(() => {
    return hours.slice(hourStart, hourEnd + 1);
  }, [hourStart, hourEnd]);

  const getCalendarColor = (calendarId: number) => {
    const calendar = calendars.find(cal => cal.id === calendarId);
    return calendar?.color || "#2563eb";
  };

  const renderEvent = (event: Event, dayIndex: number) => {
    const startHour = getHours(event.startTime);
    const startMinute = getMinutes(event.startTime);
    const endHour = getHours(event.endTime);
    const endMinute = getMinutes(event.endTime);

    // Check if event is within visible hours
    if (startHour > hourEnd || endHour < hourStart) return null;

    // Calculate position and height
    const topPercentage = ((startHour - hourStart) + startMinute / 60) * (100 / (hourEnd - hourStart + 1));
    const heightPercentage = ((endHour - startHour) + (endMinute - startMinute) / 60) * (100 / (hourEnd - hourStart + 1));

    const color = getCalendarColor(event.calendarId);
    const bgColor = `${color}20`; // 20% opacity version of color

    const style = {
      top: `${topPercentage}%`,
      height: `${heightPercentage}%`,
      left: view === "week" ? `${(100 / 7) * dayIndex}%` : "0%",
      width: view === "week" ? `${100 / 7}%` : "100%",
      backgroundColor: bgColor,
      borderLeftColor: color,
    };

    return (
      <div
        key={event.id}
        className="absolute rounded-md p-1 text-xs overflow-hidden border-l-4"
        style={style}
      >
        <div className="font-medium" style={{ color }}>
          {event.title}
        </div>
        <div className="text-gray-600 text-xs mt-1">
          {format(event.startTime, "h:mm a")} - {format(event.endTime, "h:mm a")}
        </div>
        {event.location && (
          <div className="text-gray-500 text-xs mt-1 truncate">{event.location}</div>
        )}
      </div>
    );
  };

  return (
    <div className="bg-white rounded-lg shadow overflow-hidden">
      <div className="p-4 border-b border-gray-200 flex justify-between items-center">
        <h3 className="text-lg font-medium text-gray-800">
          {view === "day"
            ? format(date, "EEEE, MMMM d, yyyy")
            : `${format(days[0], "MMM d")} - ${format(days[days.length - 1], "MMM d, yyyy")}`}
        </h3>

        <div className="flex items-center space-x-2">
          {calendars.map(calendar => (
            <div key={calendar.id} className="flex items-center space-x-1">
              <input
                type="checkbox"
                id={`calendar-${calendar.id}`}
                defaultChecked
                className="h-4 w-4 rounded border-gray-300 focus:ring-primary"
                style={{ accentColor: calendar.color }}
              />
              <label htmlFor={`calendar-${calendar.id}`} className="text-sm text-gray-700">
                {calendar.name}
              </label>
            </div>
          ))}
        </div>
      </div>

      <div className="overflow-x-auto">
        <div className={cn(
          "min-w-full bg-white grid",
          view === "day" 
            ? "grid-cols-[80px_1fr]" 
            : "grid-cols-[80px_repeat(7,1fr)]"
        )}>
          {/* Header row for week view */}
          {view === "week" && (
            <>
              <div className="sticky left-0 z-10 bg-gray-50 border-b border-r border-gray-200"></div>
              {days.map((day, i) => (
                <div key={i} className="text-center py-2 border-b border-gray-200 font-medium">
                  <div className="text-sm text-gray-500">{format(day, "EEE")}</div>
                  <div className={cn(
                    "text-lg",
                    isSameDay(day, new Date()) ? "text-primary font-bold" : "text-gray-900"
                  )}>
                    {format(day, "d")}
                  </div>
                </div>
              ))}
            </>
          )}

          {/* Hour labels */}
          <div className="sticky left-0 z-10 bg-gray-50 space-y-[60px]">
            {visibleHours.map(hour => (
              <div key={hour} className="border-r border-t border-gray-200 p-2 text-sm text-gray-500 text-right h-[60px]">
                {format(setHours(new Date(), hour), "h a")}
              </div>
            ))}
          </div>

          {/* Day columns with events */}
          {view === "day" ? (
            <div className="relative border-t border-gray-200 h-[660px]">
              {events
                .filter(event => isSameDay(event.startTime, date))
                .map(event => renderEvent(event, 0))}
            </div>
          ) : (
            days.map((day, dayIndex) => (
              <div key={dayIndex} className="relative border-t border-l border-gray-200 h-[660px]">
                {events
                  .filter(event => isSameDay(event.startTime, day))
                  .map(event => renderEvent(event, dayIndex))}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
