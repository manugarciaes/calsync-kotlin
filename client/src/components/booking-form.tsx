import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation } from '@tanstack/react-query';
import { apiRequest } from '@/lib/queryClient';
import { useToast } from '@/hooks/use-toast';
import { useTranslations } from '@/hooks/use-translations';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { format, parse } from 'date-fns';
import { Check, ChevronLeft } from 'lucide-react';

// Schema for booking form
const bookingSchema = z.object({
  freeSlotId: z.number(),
  startTime: z.string(),
  endTime: z.string(),
  customerName: z.string().min(2, { message: 'Name is required (minimum 2 characters)' }),
  customerEmail: z.string().email({ message: 'Valid email is required' }),
  notes: z.string().optional(),
});

type BookingFormValues = z.infer<typeof bookingSchema>;

interface BookingFormProps {
  freeSlotId: number;
  date: Date;
  availableSlots: {
    start: string;
    end: string;
  }[];
  onSuccess?: () => void;
  onBack?: () => void;
}

export function BookingForm({ freeSlotId, date, availableSlots, onSuccess, onBack }: BookingFormProps) {
  const { t } = useTranslations();
  const { toast } = useToast();
  const [bookingComplete, setBookingComplete] = useState(false);
  
  // Format date for display
  const formattedDate = format(date, 'EEEE, MMMM d, yyyy');
  
  // Initialize the form
  const form = useForm<BookingFormValues>({
    resolver: zodResolver(bookingSchema),
    defaultValues: {
      freeSlotId,
      startTime: '',
      endTime: '',
      customerName: '',
      customerEmail: '',
      notes: '',
    },
  });
  
  // Mutation for booking a slot
  const bookingMutation = useMutation({
    mutationFn: async (data: BookingFormValues) => {
      return await apiRequest('POST', '/api/public/book', data);
    },
    onSuccess: () => {
      setBookingComplete(true);
      if (onSuccess) onSuccess();
    },
    onError: (error) => {
      toast({
        title: t('common.error'),
        description: error.message,
        variant: 'destructive',
      });
    },
  });
  
  // Format time for display
  const formatTimeDisplay = (isoString: string) => {
    const date = new Date(isoString);
    return format(date, 'h:mm a');
  };
  
  // Handle slot selection
  const onSlotSelect = (start: string, end: string) => {
    form.setValue('startTime', start);
    form.setValue('endTime', end);
  };
  
  // Handle form submission
  const onSubmit = (data: BookingFormValues) => {
    bookingMutation.mutate(data);
  };

  // If booking is complete, show success message
  if (bookingComplete) {
    return (
      <Card className="max-w-md mx-auto">
        <CardHeader>
          <CardTitle className="text-center flex items-center justify-center">
            <div className="w-10 h-10 rounded-full bg-green-100 flex items-center justify-center mr-2">
              <Check className="h-5 w-5 text-green-600" />
            </div>
            {t('publicCalendar.bookingSuccess')}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-center text-gray-600 mb-6">
            {t('publicCalendar.bookingSuccessMessage')}
          </p>
        </CardContent>
        <CardFooter className="flex justify-center">
          <Button onClick={onBack}>{t('publicCalendar.goBack')}</Button>
        </CardFooter>
      </Card>
    );
  }

  return (
    <Card className="max-w-md mx-auto">
      <CardHeader>
        <CardTitle>{t('publicCalendar.bookingDetails')}</CardTitle>
        <CardDescription>
          {formattedDate}
          {form.watch('startTime') && (
            <span className="font-medium">
              {' '} - {formatTimeDisplay(form.watch('startTime'))} to {formatTimeDisplay(form.watch('endTime'))}
            </span>
          )}
        </CardDescription>
      </CardHeader>
      <CardContent>
        {availableSlots.length > 0 ? (
          <div className="mb-6">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              {t('publicCalendar.selectSlot')}
            </label>
            <div className="grid grid-cols-2 gap-2">
              {availableSlots.map((slot, index) => (
                <Button
                  key={index}
                  variant={form.watch('startTime') === slot.start ? 'default' : 'outline'}
                  className="justify-center"
                  onClick={() => onSlotSelect(slot.start, slot.end)}
                  type="button"
                >
                  {formatTimeDisplay(slot.start)}
                </Button>
              ))}
            </div>
          </div>
        ) : (
          <div className="text-center py-4 text-gray-500">
            {t('publicCalendar.noSlots')}
          </div>
        )}

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="customerName"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t('publicCalendar.yourName')}</FormLabel>
                  <FormControl>
                    <Input placeholder={t('publicCalendar.yourNamePlaceholder')} {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="customerEmail"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t('publicCalendar.yourEmail')}</FormLabel>
                  <FormControl>
                    <Input 
                      type="email"
                      placeholder={t('publicCalendar.yourEmailPlaceholder')} 
                      {...field} 
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="notes"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t('publicCalendar.notes')}</FormLabel>
                  <FormControl>
                    <Textarea 
                      placeholder={t('publicCalendar.notesPlaceholder')}
                      {...field} 
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="flex justify-between pt-4">
              {onBack && (
                <Button type="button" variant="outline" onClick={onBack}>
                  <ChevronLeft className="h-4 w-4 mr-2" />
                  {t('common.back')}
                </Button>
              )}
              <Button
                type="submit"
                disabled={
                  bookingMutation.isPending || 
                  !form.watch('startTime') || 
                  !form.formState.isValid
                }
                className="ml-auto"
              >
                {bookingMutation.isPending
                  ? t('publicCalendar.booking')
                  : t('publicCalendar.confirm')}
              </Button>
            </div>
          </form>
        </Form>
      </CardContent>
    </Card>
  );
}
