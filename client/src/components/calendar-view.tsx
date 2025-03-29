import React, { useState } from 'react';
import { format, addDays, subDays, startOfWeek, endOfWeek, addWeeks, subWeeks } from 'date-fns';
import { ChevronLeft, ChevronRight, Plus } from 'lucide-react';
import { Calendar, Event } from '@/types';
import { useTranslations } from '@/hooks/use-translations';
import { Button } from '@/components/ui/button';
import { CalendarGrid } from '@/components/ui/calendar-grid';

interface CalendarViewProps {
  events: Event[];
  calendars: Calendar[];
  onAddEvent?: () => void;
}

export function CalendarView({ events, calendars, onAddEvent }: CalendarViewProps) {
  const { t } = useTranslations();
  const [selectedDate, setSelectedDate] = useState<Date>(new Date());
  const [view, setView] = useState<'day' | 'week'>('day');

  // Navigation functions
  const goToToday = () => setSelectedDate(new Date());
  
  const goToPrevious = () => {
    if (view === 'day') {
      setSelectedDate(subDays(selectedDate, 1));
    } else {
      setSelectedDate(subWeeks(selectedDate, 1));
    }
  };
  
  const goToNext = () => {
    if (view === 'day') {
      setSelectedDate(addDays(selectedDate, 1));
    } else {
      setSelectedDate(addWeeks(selectedDate, 1));
    }
  };

  // Date range for the header
  const getDateRangeText = () => {
    if (view === 'day') {
      return format(selectedDate, 'EEEE, MMMM d, yyyy');
    } else {
      const start = startOfWeek(selectedDate, { weekStartsOn: 1 });
      const end = endOfWeek(selectedDate, { weekStartsOn: 1 });
      return `${format(start, 'MMM d')} - ${format(end, 'MMM d, yyyy')}`;
    }
  };

  return (
    <div className="container mx-auto">
      <div className="mb-6 flex flex-col sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-2xl font-semibold text-gray-800">{t('calendar.title')}</h2>
          <p className="text-gray-600">{t('calendar.subtitle')}</p>
        </div>
        
        <div className="mt-4 sm:mt-0 flex flex-wrap items-center space-x-2">
          <div className="flex space-x-1 bg-white p-1 rounded-md shadow-sm">
            <Button
              variant={view === 'day' ? 'default' : 'ghost'}
              size="sm"
              onClick={() => setView('day')}
            >
              {t('calendar.day')}
            </Button>
            <Button
              variant={view === 'week' ? 'default' : 'ghost'}
              size="sm"
              onClick={() => setView('week')}
            >
              {t('calendar.week')}
            </Button>
          </div>
          
          <div className="flex items-center bg-white p-1 rounded-md shadow-sm">
            <Button variant="ghost" size="icon" onClick={goToPrevious}>
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <Button variant="ghost" size="sm" onClick={goToToday}>
              {t('calendar.today')}
            </Button>
            <Button variant="ghost" size="icon" onClick={goToNext}>
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
          
          {onAddEvent && (
            <Button size="sm" onClick={onAddEvent} className="flex items-center">
              <Plus className="h-4 w-4 mr-1" />
              {t('calendar.newEvent')}
            </Button>
          )}
        </div>
      </div>
      
      <CalendarGrid
        date={selectedDate}
        events={events}
        view={view}
        calendars={calendars}
      />
    </div>
  );
}
