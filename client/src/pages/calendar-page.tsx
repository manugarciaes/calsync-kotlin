import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Layout } from '@/components/layout';
import { CalendarView } from '@/components/calendar-view';
import { AddCalendarDialog } from '@/components/add-calendar-dialog';
import { Card } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { useTranslations } from '@/hooks/use-translations';
import { Calendar, Plus } from 'lucide-react';
import { EmptyState } from '@/components/empty-state';
import { CalendarSources } from '@/components/calendar-sources';
import { Button } from '@/components/ui/button';

export default function CalendarPage() {
  const { t } = useTranslations();
  const [activeTab, setActiveTab] = useState<string>('view');
  const [showAddCalendarDialog, setShowAddCalendarDialog] = useState(false);
  
  // Fetch calendars
  const { data: calendars = [], isLoading: isLoadingCalendars } = useQuery({
    queryKey: ['/api/calendars'],
  });
  
  // Fetch events
  const { data: events = [], isLoading: isLoadingEvents } = useQuery({
    queryKey: ['/api/calendars/events'],
  });

  // Event handler for adding a new calendar
  const handleAddCalendar = () => {
    setShowAddCalendarDialog(true);
  };

  return (
    <Layout>
      <Tabs defaultValue="view" value={activeTab} onValueChange={setActiveTab}>
        <div className="container mx-auto mb-6">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between mb-4">
            <div>
              <h2 className="text-2xl font-semibold text-gray-800">{t('calendar.title')}</h2>
              <p className="text-gray-600">{t('calendar.subtitle')}</p>
            </div>
            <div className="mt-4 sm:mt-0">
              <TabsList className="grid w-full grid-cols-2">
                <TabsTrigger value="view">{t('calendar.title')}</TabsTrigger>
                <TabsTrigger value="manage">{t('calendarSources.name')}</TabsTrigger>
              </TabsList>
            </div>
          </div>
        </div>
        
        <TabsContent value="view">
          {isLoadingCalendars || isLoadingEvents ? (
            <div className="text-center py-8 text-gray-500">{t('common.loading')}</div>
          ) : calendars.length > 0 ? (
            <CalendarView events={events} calendars={calendars} />
          ) : (
            <Card className="container mx-auto py-8">
              <EmptyState
                icon={<Calendar className="h-8 w-8" />}
                title={t('emptyStates.noCalendars.title')}
                description={t('emptyStates.noCalendars.description')}
                actionLabel={t('emptyStates.noCalendars.action')}
                onAction={handleAddCalendar}
              />
            </Card>
          )}
        </TabsContent>
        
        <TabsContent value="manage">
          <div className="container mx-auto">
            <Card>
              <div className="p-4 border-b border-gray-200">
                <div className="flex justify-between items-center">
                  <h3 className="text-lg font-medium text-gray-800">{t('dashboard.connectedCalendars')}</h3>
                  <Button onClick={handleAddCalendar} className="flex items-center">
                    <Plus className="h-4 w-4 mr-1" />
                    {t('dashboard.addCalendar')}
                  </Button>
                </div>
              </div>
              
              <div className="p-4">
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
                    onAction={handleAddCalendar}
                  />
                )}
              </div>
            </Card>
          </div>
        </TabsContent>
      </Tabs>
      
      <AddCalendarDialog
        open={showAddCalendarDialog}
        onOpenChange={setShowAddCalendarDialog}
      />
    </Layout>
  );
}
