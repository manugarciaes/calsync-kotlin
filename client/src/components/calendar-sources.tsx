import React from 'react';
import { useMutation } from '@tanstack/react-query';
import { Edit, RefreshCw, Trash2 } from 'lucide-react';
import { Calendar } from '@/types';
import { useTranslations } from '@/hooks/use-translations';
import { apiRequest, queryClient } from '@/lib/queryClient';
import { useToast } from '@/hooks/use-toast';
import { formatDistanceToNow } from 'date-fns';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';

interface CalendarSourcesProps {
  calendars: Calendar[];
  onEdit?: (calendar: Calendar) => void;
}

export function CalendarSources({ calendars, onEdit }: CalendarSourcesProps) {
  const { t } = useTranslations();
  const { toast } = useToast();
  const [calendarToDelete, setCalendarToDelete] = React.useState<Calendar | null>(null);

  const getSourceLabel = (source: string) => {
    switch (source) {
      case 'google':
        return 'Google Calendar';
      case 'outlook':
        return 'Outlook';
      case 'ics_url':
        return 'ICS URL';
      case 'ics_file':
        return 'ICS File';
      default:
        return source;
    }
  };

  const formatLastUpdated = (date: Date) => {
    return formatDistanceToNow(new Date(date), { addSuffix: true });
  };

  const deleteMutation = useMutation({
    mutationFn: async (id: number) => {
      await apiRequest('DELETE', `/api/calendars/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['/api/calendars'] });
      toast({
        title: t('common.success'),
        description: t('calendar.deleteSuccess'),
      });
      setCalendarToDelete(null);
    },
    onError: (error) => {
      toast({
        title: t('common.error'),
        description: error.message,
        variant: 'destructive',
      });
    },
  });

  const syncMutation = useMutation({
    mutationFn: async (id: number) => {
      await apiRequest('POST', `/api/calendars/${id}/sync`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['/api/calendars'] });
      toast({
        title: t('common.success'),
        description: t('calendar.syncSuccess'),
      });
    },
    onError: (error) => {
      toast({
        title: t('common.error'),
        description: error.message,
        variant: 'destructive',
      });
    },
  });

  return (
    <>
      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>{t('calendarSources.name')}</TableHead>
              <TableHead>{t('calendarSources.source')}</TableHead>
              <TableHead>{t('calendarSources.status')}</TableHead>
              <TableHead>{t('calendarSources.lastUpdated')}</TableHead>
              <TableHead>{t('calendarSources.actions')}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {calendars.map((calendar) => (
              <TableRow key={calendar.id}>
                <TableCell>
                  <div className="flex items-center">
                    <div className="h-4 w-4 rounded-full mr-2" style={{ backgroundColor: calendar.color }}></div>
                    <div className="text-sm font-medium text-gray-900">{calendar.name}</div>
                  </div>
                </TableCell>
                <TableCell>
                  <div className="text-sm text-gray-900">{getSourceLabel(calendar.source)}</div>
                </TableCell>
                <TableCell>
                  <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
                    calendar.lastSyncStatus === 'synced' 
                      ? 'bg-green-100 text-green-800' 
                      : 'bg-yellow-100 text-yellow-800'
                  }`}>
                    {calendar.lastSyncStatus === 'synced' 
                      ? t('calendarSources.synced') 
                      : t('calendarSources.updateNeeded')}
                  </span>
                </TableCell>
                <TableCell className="text-sm text-gray-500">
                  {formatLastUpdated(calendar.lastUpdated)}
                </TableCell>
                <TableCell>
                  <div className="flex space-x-2">
                    {onEdit && (
                      <button 
                        className="text-gray-500 hover:text-primary"
                        onClick={() => onEdit(calendar)}
                      >
                        <Edit className="h-4 w-4" />
                      </button>
                    )}
                    <button 
                      className="text-gray-500 hover:text-primary"
                      onClick={() => syncMutation.mutate(calendar.id)}
                      disabled={syncMutation.isPending}
                    >
                      <RefreshCw className={`h-4 w-4 ${syncMutation.isPending ? 'animate-spin' : ''}`} />
                    </button>
                    <button 
                      className="text-gray-500 hover:text-error"
                      onClick={() => setCalendarToDelete(calendar)}
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      <AlertDialog open={!!calendarToDelete} onOpenChange={() => setCalendarToDelete(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{t('calendar.deleteCalendar')}</AlertDialogTitle>
            <AlertDialogDescription>
              {t('calendar.deleteConfirm')}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{t('common.cancel')}</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => calendarToDelete && deleteMutation.mutate(calendarToDelete.id)}
              className="bg-red-500 hover:bg-red-600"
            >
              {t('common.delete')}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
