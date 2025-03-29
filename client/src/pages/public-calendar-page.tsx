import React, { useState } from 'react';
import { useParams, useLocation } from 'wouter';
import { useQuery } from '@tanstack/react-query';
import { Calendar } from '@/components/ui/calendar';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { useTranslations } from '@/hooks/use-translations';
import { BookingForm } from '@/components/booking-form';
import { format } from 'date-fns';
import { Calendar as CalendarIcon, Clock } from 'lucide-react';

export default function PublicCalendarPage() {
  const { shareUrl } = useParams<{ shareUrl: string }>();
  const [, navigate] = useLocation();
  const { t } = useTranslations();
  const [selectedDate, setSelectedDate] = useState<Date | undefined>(undefined);
  
  // Fetch free slot configuration
  const { data: freeSlot, isLoading: isLoadingFreeSlot } = useQuery({
    queryKey: [`/api/public/slots/${shareUrl}`],
  });
  
  // Fetch available slots for selected date
  const { data: availableSlots = [], isLoading: isLoadingSlots } = useQuery({
    queryKey: [`/api/public/slots/${shareUrl}/available-times`, { start: selectedDate?.toISOString() }],
    enabled: !!selectedDate,
  });
  
  const handleDateSelect = (date: Date | undefined) => {
    setSelectedDate(date);
  };
  
  const handleBookingSuccess = () => {
    // Refresh available slots
    setSelectedDate(undefined);
  };

  // Show loading state
  if (isLoadingFreeSlot) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <Clock className="h-12 w-12 text-primary mx-auto mb-4 animate-pulse" />
          <h2 className="text-xl font-semibold text-gray-800">{t('common.loading')}</h2>
        </div>
      </div>
    );
  }
  
  // Show error if slot not found
  if (!freeSlot) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <Card className="max-w-md w-full mx-4">
          <CardHeader>
            <CardTitle className="text-center text-red-600">
              Slot not found
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-center text-gray-600 mb-6">
              The booking slot you're looking for doesn't exist or has been deactivated.
            </p>
            <div className="flex justify-center">
              <Button onClick={() => navigate('/')}>Return to Home</Button>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-12 px-4">
      <div className="max-w-6xl mx-auto">
        <div className="text-center mb-10">
          <h1 className="text-3xl font-bold text-gray-900">{freeSlot.name}</h1>
          <p className="text-gray-600 mt-2">{t('publicCalendar.subtitle')}</p>
        </div>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          <Card>
            <CardHeader>
              <CardTitle>{t('publicCalendar.chooseDate')}</CardTitle>
              <CardDescription>
                {selectedDate 
                  ? format(selectedDate, 'EEEE, MMMM d, yyyy')
                  : t('publicCalendar.selectDate')}
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Calendar
                mode="single"
                selected={selectedDate}
                onSelect={handleDateSelect}
                className="rounded-md border"
                disabled={{ before: new Date() }}
              />
            </CardContent>
          </Card>
          
          <div>
            {selectedDate ? (
              <BookingForm
                freeSlotId={freeSlot.id}
                date={selectedDate}
                availableSlots={availableSlots}
                onSuccess={handleBookingSuccess}
                onBack={() => setSelectedDate(undefined)}
              />
            ) : (
              <Card>
                <CardHeader>
                  <CardTitle>{t('publicCalendar.availableSlots')}</CardTitle>
                </CardHeader>
                <CardContent className="flex flex-col items-center justify-center py-12">
                  <CalendarIcon className="h-16 w-16 text-gray-300 mb-4" />
                  <p className="text-gray-500 text-center">
                    {t('publicCalendar.selectDate')}
                  </p>
                </CardContent>
              </Card>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
