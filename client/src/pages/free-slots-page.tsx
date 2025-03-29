import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Layout } from '@/components/layout';
import { FreeSlotForm } from '@/components/free-slot-form';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { useTranslations } from '@/hooks/use-translations';
import { Clock, Copy, Link as LinkIcon, Calendar } from 'lucide-react';
import { format } from 'date-fns';
import { useToast } from '@/hooks/use-toast';
import { EmptyState } from '@/components/empty-state';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';

export default function FreeSlotsPage() {
  const { t } = useTranslations();
  const { toast } = useToast();
  const [showCreateForm, setShowCreateForm] = useState(false);
  
  // Fetch calendars
  const { data: calendars = [], isLoading: isLoadingCalendars } = useQuery({
    queryKey: ['/api/calendars'],
  });
  
  // Fetch free slots
  const { data: freeSlots = [], isLoading: isLoadingFreeSlots } = useQuery({
    queryKey: ['/api/free-slots'],
  });
  
  // Handle copy link button
  const handleCopyLink = (shareUrl: string) => {
    const url = `${window.location.origin}/s/${shareUrl}`;
    navigator.clipboard.writeText(url);
    toast({
      title: t('common.success'),
      description: t('freeSlots.linkCopied'),
    });
  };
  
  // Format days available
  const formatDaysAvailable = (daysStr: string) => {
    const dayMap: Record<string, string> = {
      '0': t('freeSlots.sun'),
      '1': t('freeSlots.mon'),
      '2': t('freeSlots.tue'),
      '3': t('freeSlots.wed'),
      '4': t('freeSlots.thu'),
      '5': t('freeSlots.fri'),
      '6': t('freeSlots.sat'),
    };
    
    return daysStr.split(',').map(day => dayMap[day]).join(', ');
  };

  return (
    <Layout>
      <div className="container mx-auto">
        <div className="mb-6">
          <h2 className="text-2xl font-semibold text-gray-800">{t('freeSlots.title')}</h2>
          <p className="text-gray-600">{t('freeSlots.subtitle')}</p>
        </div>
        
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Form */}
          {showCreateForm ? (
            <div className="lg:col-span-2">
              <FreeSlotForm 
                calendars={calendars} 
                onSuccess={() => setShowCreateForm(false)}
              />
            </div>
          ) : (
            <div className="lg:col-span-2">
              <Card className="h-full flex flex-col justify-center items-center py-12">
                <EmptyState
                  icon={<Clock className="h-8 w-8" />}
                  title={isLoadingFreeSlots ? t('common.loading') : t('emptyStates.noFreeSlots.title')}
                  description={t('emptyStates.noFreeSlots.description')}
                  actionLabel={t('emptyStates.noFreeSlots.action')}
                  onAction={() => setShowCreateForm(true)}
                />
              </Card>
            </div>
          )}
          
          {/* Slot configurations */}
          <div className="lg:col-span-1">
            <Card>
              <CardHeader className="border-b border-gray-200">
                <CardTitle>{t('freeSlots.yourConfigurations')}</CardTitle>
              </CardHeader>
              <CardContent className="p-4">
                {isLoadingFreeSlots ? (
                  <div className="text-center py-8 text-gray-500">{t('common.loading')}</div>
                ) : freeSlots.length > 0 ? (
                  <div className="space-y-4">
                    {freeSlots.map((slot, index) => (
                      <div key={index} className="border border-gray-200 rounded-md p-3">
                        <div className="flex justify-between items-center">
                          <h4 className="text-sm font-medium text-gray-900">{slot.name}</h4>
                          <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                            slot.isActive 
                              ? 'bg-green-100 text-green-800' 
                              : 'bg-yellow-100 text-yellow-800'
                          }`}>
                            {slot.isActive ? t('dashboard.active') : t('dashboard.needsUpdate')}
                          </span>
                        </div>
                        <div className="mt-2 text-xs text-gray-500">
                          <p>{formatDaysAvailable(slot.availableDays)}, {slot.startTime} - {slot.endTime}</p>
                          <p>{slot.slotDuration} {t('settings.minutes')} {t('freeSlots.timeSlots').toLowerCase()}</p>
                        </div>
                        <div className="mt-3 flex items-center space-x-2 text-xs">
                          <Button 
                            variant="link" 
                            size="sm" 
                            className="h-auto p-0 text-primary"
                            onClick={() => handleCopyLink(slot.shareUrl)}
                          >
                            <Copy className="h-3 w-3 mr-1" />
                            {t('freeSlots.copyLink')}
                          </Button>
                          <span className="text-gray-300">|</span>
                          <Button 
                            variant="link" 
                            size="sm" 
                            className="h-auto p-0 text-gray-700"
                            asChild
                          >
                            <a href={`/s/${slot.shareUrl}`} target="_blank">
                              <LinkIcon className="h-3 w-3 mr-1" />
                              {t('freeSlots.viewLink')}
                            </a>
                          </Button>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="text-center py-4 text-gray-500">
                    {t('emptyStates.noFreeSlots.description')}
                  </div>
                )}
                
                {freeSlots.length > 0 && !showCreateForm && (
                  <Button
                    className="w-full mt-4"
                    onClick={() => setShowCreateForm(true)}
                  >
                    {t('emptyStates.noFreeSlots.action')}
                  </Button>
                )}
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    </Layout>
  );
}
