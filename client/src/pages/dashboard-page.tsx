import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Layout } from '@/components/layout';
import { StatCard } from '@/components/stat-card';
import { CalendarSources } from '@/components/calendar-sources';
import { AddCalendarDialog } from '@/components/add-calendar-dialog';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { useTranslations } from '@/hooks/use-translations';
import { Calendar, Clock, Users, CheckSquare, Plus, Share2, CalendarCheck } from 'lucide-react';
import { format } from 'date-fns';
import { EmptyState } from '@/components/empty-state';
import { Link } from 'wouter';

export default function DashboardPage() {
  const { t } = useTranslations();
  const [showAddCalendarDialog, setShowAddCalendarDialog] = useState(false);
  
  // Fetch calendars
  const { data: calendars = [], isLoading: isLoadingCalendars } = useQuery({
    queryKey: ['/api/calendars'],
  });
  
  // Fetch events for today
  const { data: todayEvents = [], isLoading: isLoadingEvents } = useQuery({
    queryKey: ['/api/calendars/events/today'],
  });
  
  // Fetch free slots
  const { data: freeSlots = [], isLoading: isLoadingFreeSlots } = useQuery({
    queryKey: ['/api/free-slots'],
  });
  
  // Fetch bookings
  const { data: bookings = [], isLoading: isLoadingBookings } = useQuery({
    queryKey: ['/api/bookings'],
  });
  
  // Stats for the top cards
  const stats = [
    {
      title: t('dashboard.connectedCalendars'),
      value: calendars.length,
      icon: <Calendar />,
      color: '#2563eb'
    },
    {
      title: t('dashboard.freeSlotsToday'),
      value: 5, // This would ideally be calculated from the available slots data
      icon: <Clock />,
      color: '#4f46e5'
    },
    {
      title: t('dashboard.upcomingEvents'),
      value: todayEvents.length,
      icon: <Users />,
      color: '#0ea5e9'
    },
    {
      title: t('dashboard.bookedAppointments'),
      value: bookings.length,
      icon: <CheckSquare />,
      color: '#22c55e'
    }
  ];
  
  // Format event time
  const formatEventTime = (start: string, end: string) => {
    return `${format(new Date(start), 'HH:mm')} - ${format(new Date(end), 'HH:mm')}`;
  };

  return (
    <Layout>
      <div className="container mx-auto">
        <div className="mb-6">
          <h2 className="text-2xl font-semibold text-gray-800">{t('dashboard.welcome')}</h2>
          <p className="text-gray-600">{t('dashboard.subtitle')}</p>
        </div>
        
        {/* Quick Stats */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
          {stats.map((stat, index) => (
            <StatCard key={index} stat={stat} />
          ))}
        </div>
        
        {/* Connected Calendars */}
        <Card className="mb-6">
          <CardHeader className="border-b border-gray-200">
            <div className="flex justify-between items-center">
              <CardTitle>{t('dashboard.connectedCalendars')}</CardTitle>
              <Button onClick={() => setShowAddCalendarDialog(true)} className="flex items-center">
                <Plus className="h-4 w-4 mr-1" />
                {t('dashboard.addCalendar')}
              </Button>
            </div>
          </CardHeader>
          
          <CardContent className="p-4">
            {isLoadingCalendars ? (
              <div className="text-center py-8 text-gray-500">{t('common.loading')}</div>
            ) : calendars.length > 0 ? (
              <CalendarSources calendars={calendars} />
            ) : (
              <EmptyState
                icon={<Calendar className="h-8 w-8" />}
                title={t('emptyStates.noCalendars.title')}
                description={t('emptyStates.noCalendars.description')}
                actionLabel={t('emptyStates.noCalendars.action')}
                onAction={() => setShowAddCalendarDialog(true)}
              />
            )}
          </CardContent>
        </Card>
        
        {/* Today's Schedule and Shared Calendars */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Card>
            <CardHeader className="border-b border-gray-200">
              <CardTitle>{t('dashboard.todaySchedule')}</CardTitle>
            </CardHeader>
            <CardContent className="p-4">
              {isLoadingEvents ? (
                <div className="text-center py-8 text-gray-500">{t('common.loading')}</div>
              ) : todayEvents.length > 0 ? (
                <div className="space-y-4">
                  {todayEvents.map((event, index) => (
                    <div key={index} className="flex items-start space-x-3 p-3 hover:bg-gray-50 rounded-md">
                      <div className="bg-primary/10 text-primary rounded p-2 text-center w-16">
                        <div className="text-sm font-semibold">
                          {format(new Date(event.startTime), 'HH:mm')}
                        </div>
                        <div className="text-xs">
                          {format(new Date(event.endTime), 'HH:mm')}
                        </div>
                      </div>
                      <div className="flex-1">
                        <div className="flex items-center">
                          <h4 className="text-sm font-medium text-gray-900">{event.title}</h4>
                          <span className="ml-2 flex-shrink-0 inline-block h-2 w-2 rounded-full bg-primary"></span>
                        </div>
                        {event.location && (
                          <p className="text-xs text-gray-500 mt-1">{event.location}</p>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <EmptyState
                  icon={<Calendar className="h-8 w-8" />}
                  title={t('emptyStates.noEvents.title')}
                  description={t('emptyStates.noEvents.description')}
                />
              )}
            </CardContent>
          </Card>
          
          <Card>
            <CardHeader className="border-b border-gray-200">
              <div className="flex justify-between items-center">
                <CardTitle>{t('dashboard.sharedCalendarStatus')}</CardTitle>
                <Link href="/free-slots">
                  <Button variant="link" className="text-sm">
                    {t('dashboard.viewAll')}
                  </Button>
                </Link>
              </div>
            </CardHeader>
            <CardContent className="p-4">
              {isLoadingFreeSlots ? (
                <div className="text-center py-8 text-gray-500">{t('common.loading')}</div>
              ) : freeSlots.length > 0 ? (
                <div className="space-y-4">
                  {freeSlots.slice(0, 2).map((slot, index) => (
                    <div key={index} className="bg-gray-50 rounded-md p-4">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center">
                          <Share2 className="text-primary mr-2 h-5 w-5" />
                          <h4 className="text-sm font-medium text-gray-900">{slot.name}</h4>
                        </div>
                        <span className="bg-green-100 text-success text-xs px-2 py-1 rounded-full">
                          {t('dashboard.active')}
                        </span>
                      </div>
                      <div className="mt-3 text-sm text-gray-600">
                        <p>{index === 0 ? t('dashboard.sharedCalendar') : t('dashboard.publicBooking')}</p>
                        <div className="mt-2 flex items-center text-xs text-gray-500">
                          <Share2 className="h-3 w-3 mr-1" />
                          <a 
                            href={`/s/${slot.shareUrl}`} 
                            target="_blank" 
                            className="text-primary truncate"
                          >
                            {window.location.origin}/s/{slot.shareUrl}
                          </a>
                        </div>
                        <div className="mt-3 flex space-x-2">
                          {slot.calendarIds.split(',').map((calId, idx) => {
                            const calendar = calendars.find(c => c.id === parseInt(calId));
                            if (!calendar) return null;
                            
                            return (
                              <span
                                key={idx}
                                className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium"
                                style={{ 
                                  backgroundColor: `${calendar.color}20`,
                                  color: calendar.color
                                }}
                              >
                                {calendar.name}
                              </span>
                            );
                          })}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <EmptyState
                  icon={<CalendarCheck className="h-8 w-8" />}
                  title={t('emptyStates.noFreeSlots.title')}
                  description={t('emptyStates.noFreeSlots.description')}
                  actionLabel={t('emptyStates.noFreeSlots.action')}
                  onAction={() => window.location.href = '/free-slots'}
                />
              )}
            </CardContent>
          </Card>
        </div>
      </div>
      
      <AddCalendarDialog
        open={showAddCalendarDialog}
        onOpenChange={setShowAddCalendarDialog}
      />
    </Layout>
  );
}
