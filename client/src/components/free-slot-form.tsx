import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Calendar as DatePicker } from '@/components/ui/calendar';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Checkbox } from '@/components/ui/checkbox';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Button } from '@/components/ui/button';
import { Calendar as CalendarIcon } from 'lucide-react';
import { useTranslations } from '@/hooks/use-translations';
import { useMutation } from '@tanstack/react-query';
import { apiRequest, queryClient } from '@/lib/queryClient';
import { useToast } from '@/hooks/use-toast';
import { Calendar } from '@/types';
import { format } from 'date-fns';
import { cn } from '@/lib/utils';

// Schema for free slot form
const freeSlotSchema = z.object({
  name: z.string().min(1, { message: 'Name is required' }),
  calendarIds: z.string().min(1, { message: 'At least one calendar must be selected' }),
  slotDuration: z.number().min(1, { message: 'Slot duration is required' }),
  bufferTime: z.number(),
  startTime: z.string(),
  endTime: z.string(),
  availableDays: z.string().min(1, { message: 'At least one day must be selected' }),
  dateRange: z.object({
    start: z.string(),
    end: z.string(),
  }).optional(),
});

type FreeSlotFormValues = z.infer<typeof freeSlotSchema>;

interface FreeSlotFormProps {
  calendars: Calendar[];
  onSuccess?: () => void;
}

export function FreeSlotForm({ calendars, onSuccess }: FreeSlotFormProps) {
  const { t } = useTranslations();
  const { toast } = useToast();
  const [dateRange, setDateRange] = useState<{
    from: Date | undefined;
    to: Date | undefined;
  }>({
    from: undefined,
    to: undefined,
  });

  // Days of week checkboxes
  const daysOfWeek = [
    { id: '1', label: t('freeSlots.mon') },
    { id: '2', label: t('freeSlots.tue') },
    { id: '3', label: t('freeSlots.wed') },
    { id: '4', label: t('freeSlots.thu') },
    { id: '5', label: t('freeSlots.fri') },
    { id: '6', label: t('freeSlots.sat') },
    { id: '0', label: t('freeSlots.sun') },
  ];

  // Default form values
  const defaultValues: Partial<FreeSlotFormValues> = {
    name: '',
    calendarIds: '',
    slotDuration: 30,
    bufferTime: 0,
    startTime: '09:00',
    endTime: '17:00',
    availableDays: '1,2,3,4,5',
  };

  // Initialize the form
  const form = useForm<FreeSlotFormValues>({
    resolver: zodResolver(freeSlotSchema),
    defaultValues,
  });

  // Handle calendar checkboxes
  const [selectedCalendars, setSelectedCalendars] = useState<number[]>([]);
  
  const handleCalendarCheckboxChange = (calendarId: number, checked: boolean) => {
    let newSelectedCalendars: number[];
    
    if (checked) {
      newSelectedCalendars = [...selectedCalendars, calendarId];
    } else {
      newSelectedCalendars = selectedCalendars.filter(id => id !== calendarId);
    }
    
    setSelectedCalendars(newSelectedCalendars);
    form.setValue('calendarIds', newSelectedCalendars.join(','));
  };

  // Handle days of week checkboxes
  const [selectedDays, setSelectedDays] = useState<string[]>(['1', '2', '3', '4', '5']);
  
  const handleDayCheckboxChange = (dayId: string, checked: boolean) => {
    let newSelectedDays: string[];
    
    if (checked) {
      newSelectedDays = [...selectedDays, dayId];
    } else {
      newSelectedDays = selectedDays.filter(id => id !== dayId);
    }
    
    setSelectedDays(newSelectedDays);
    form.setValue('availableDays', newSelectedDays.join(','));
  };

  // Mutation for creating free slot
  const createSlotMutation = useMutation({
    mutationFn: async (data: FreeSlotFormValues) => {
      return await apiRequest('POST', '/api/free-slots', data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['/api/free-slots'] });
      toast({
        title: t('common.success'),
        description: t('freeSlots.success'),
      });
      form.reset(defaultValues);
      setSelectedCalendars([]);
      setSelectedDays(['1', '2', '3', '4', '5']);
      setDateRange({ from: undefined, to: undefined });
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

  // Handle form submission
  const onSubmit = (data: FreeSlotFormValues) => {
    // Add date range if selected
    if (dateRange.from && dateRange.to) {
      data.dateRange = {
        start: format(dateRange.from, 'yyyy-MM-dd'),
        end: format(dateRange.to, 'yyyy-MM-dd'),
      };
    }
    
    createSlotMutation.mutate(data);
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t('freeSlots.createNew')}</CardTitle>
      </CardHeader>
      <CardContent>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="name"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t('freeSlots.slotName')}</FormLabel>
                  <FormControl>
                    <Input placeholder={t('freeSlots.namePlaceholder')} {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="calendarIds"
              render={() => (
                <FormItem>
                  <FormLabel>{t('freeSlots.selectCalendars')}</FormLabel>
                  <div className="mt-2 space-y-2">
                    {calendars.map((calendar) => (
                      <div key={calendar.id} className="flex items-center">
                        <Checkbox
                          id={`calendar-${calendar.id}`}
                          checked={selectedCalendars.includes(calendar.id)}
                          onCheckedChange={(checked) => 
                            handleCalendarCheckboxChange(calendar.id, !!checked)
                          }
                        />
                        <Label
                          htmlFor={`calendar-${calendar.id}`}
                          className="ml-2 flex items-center"
                        >
                          <div
                            className="w-3 h-3 rounded-full mr-2"
                            style={{ backgroundColor: calendar.color }}
                          ></div>
                          {calendar.name}
                        </Label>
                      </div>
                    ))}
                  </div>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <FormItem>
                <FormLabel>{t('freeSlots.dateRange')}</FormLabel>
                <Popover>
                  <PopoverTrigger asChild>
                    <Button
                      variant="outline"
                      className={cn(
                        "w-full justify-start text-left font-normal",
                        !dateRange.from && "text-muted-foreground"
                      )}
                    >
                      <CalendarIcon className="mr-2 h-4 w-4" />
                      {dateRange.from ? (
                        dateRange.to ? (
                          `${format(dateRange.from, "LLL dd, y")} - ${format(dateRange.to, "LLL dd, y")}`
                        ) : (
                          format(dateRange.from, "LLL dd, y")
                        )
                      ) : (
                        <span>Pick a date range</span>
                      )}
                    </Button>
                  </PopoverTrigger>
                  <PopoverContent className="w-auto p-0" align="start">
                    <DatePicker
                      mode="range"
                      selected={dateRange}
                      onSelect={setDateRange as any}
                      initialFocus
                    />
                  </PopoverContent>
                </Popover>
              </FormItem>

              <FormField
                control={form.control}
                name="slotDuration"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('freeSlots.timeSlots')}</FormLabel>
                    <Select
                      onValueChange={(value) => field.onChange(parseInt(value))}
                      defaultValue={field.value.toString()}
                    >
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Select duration" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value="30">{t('freeSlots.duration30')}</SelectItem>
                        <SelectItem value="60">{t('freeSlots.duration60')}</SelectItem>
                        <SelectItem value="90">{t('freeSlots.duration90')}</SelectItem>
                        <SelectItem value="120">{t('freeSlots.duration120')}</SelectItem>
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <FormField
              control={form.control}
              name="availabilityHours"
              render={() => (
                <FormItem>
                  <FormLabel>{t('freeSlots.availabilityHours')}</FormLabel>
                  <div className="mt-2 grid grid-cols-2 gap-4">
                    <div>
                      <Label htmlFor="start-time" className="block text-xs text-gray-500">
                        {t('freeSlots.startTime')}
                      </Label>
                      <Input
                        id="start-time"
                        type="time"
                        value={form.watch('startTime')}
                        onChange={(e) => form.setValue('startTime', e.target.value)}
                      />
                    </div>
                    <div>
                      <Label htmlFor="end-time" className="block text-xs text-gray-500">
                        {t('freeSlots.endTime')}
                      </Label>
                      <Input
                        id="end-time"
                        type="time"
                        value={form.watch('endTime')}
                        onChange={(e) => form.setValue('endTime', e.target.value)}
                      />
                    </div>
                  </div>
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="availableDays"
              render={() => (
                <FormItem>
                  <FormLabel>{t('freeSlots.daysAvailable')}</FormLabel>
                  <div className="mt-2 grid grid-cols-4 sm:grid-cols-7 gap-2">
                    {daysOfWeek.map((day) => (
                      <div key={day.id} className="flex items-center justify-center">
                        <Checkbox
                          id={`day-${day.id}`}
                          checked={selectedDays.includes(day.id)}
                          onCheckedChange={(checked) => 
                            handleDayCheckboxChange(day.id, !!checked)
                          }
                        />
                        <Label htmlFor={`day-${day.id}`} className="ml-2">
                          {day.label}
                        </Label>
                      </div>
                    ))}
                  </div>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="bufferTime"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t('freeSlots.bufferTime')}</FormLabel>
                  <Select
                    onValueChange={(value) => field.onChange(parseInt(value))}
                    defaultValue={field.value.toString()}
                  >
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Select buffer time" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectItem value="0">{t('freeSlots.buffer0')}</SelectItem>
                      <SelectItem value="15">{t('freeSlots.buffer15')}</SelectItem>
                      <SelectItem value="30">{t('freeSlots.buffer30')}</SelectItem>
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="pt-3">
              <Button
                type="submit"
                className="w-full"
                disabled={createSlotMutation.isPending}
              >
                {createSlotMutation.isPending
                  ? t('common.loading')
                  : t('freeSlots.createSlots')}
              </Button>
            </div>
          </form>
        </Form>
      </CardContent>
    </Card>
  );
}
