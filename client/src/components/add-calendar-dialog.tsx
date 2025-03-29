import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation } from '@tanstack/react-query';
import { apiRequest, queryClient } from '@/lib/queryClient';
import { useToast } from '@/hooks/use-toast';
import { useTranslations } from '@/hooks/use-translations';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';
import { Link, Upload } from 'lucide-react';

// Schema for URL tab
const urlSchema = z.object({
  name: z.string().min(1, { message: 'Name is required' }),
  url: z.string().url({ message: 'Please enter a valid URL' }),
  color: z.string(),
});

// Schema for file upload tab
const fileSchema = z.object({
  name: z.string().min(1, { message: 'Name is required' }),
  color: z.string(),
});

type UrlFormValues = z.infer<typeof urlSchema>;
type FileFormValues = z.infer<typeof fileSchema>;

interface AddCalendarDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function AddCalendarDialog({ open, onOpenChange }: AddCalendarDialogProps) {
  const { t } = useTranslations();
  const { toast } = useToast();
  const [activeTab, setActiveTab] = useState<string>('url');
  const [file, setFile] = useState<File | null>(null);
  
  // Form for URL tab
  const urlForm = useForm<UrlFormValues>({
    resolver: zodResolver(urlSchema),
    defaultValues: {
      name: '',
      url: '',
      color: '#2563eb',
    },
  });

  // Form for file upload tab
  const fileForm = useForm<FileFormValues>({
    resolver: zodResolver(fileSchema),
    defaultValues: {
      name: '',
      color: '#2563eb',
    },
  });

  // Mutation for adding calendar from URL
  const urlMutation = useMutation({
    mutationFn: async (data: UrlFormValues) => {
      return await apiRequest('POST', '/api/calendars/url', data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['/api/calendars'] });
      toast({
        title: t('common.success'),
        description: t('addCalendar.success'),
      });
      onOpenChange(false);
      urlForm.reset();
    },
    onError: (error) => {
      toast({
        title: t('common.error'),
        description: error.message,
        variant: 'destructive',
      });
    },
  });

  // Mutation for uploading ICS file
  const fileMutation = useMutation({
    mutationFn: async (data: FileFormValues) => {
      if (!file) throw new Error(t('addCalendar.fileRequired'));
      
      const formData = new FormData();
      formData.append('file', file);
      formData.append('name', data.name);
      formData.append('color', data.color);
      
      const response = await fetch('/api/calendars/upload', {
        method: 'POST',
        body: formData,
        credentials: 'include',
      });
      
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || response.statusText);
      }
      
      return await response.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['/api/calendars'] });
      toast({
        title: t('common.success'),
        description: t('addCalendar.success'),
      });
      onOpenChange(false);
      fileForm.reset();
      setFile(null);
    },
    onError: (error) => {
      toast({
        title: t('common.error'),
        description: error.message,
        variant: 'destructive',
      });
    },
  });

  // Handle file upload
  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      setFile(e.target.files[0]);
    }
  };

  // Handle form submission
  const onUrlSubmit = (data: UrlFormValues) => {
    urlMutation.mutate(data);
  };

  const onFileSubmit = (data: FileFormValues) => {
    fileMutation.mutate(data);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>{t('addCalendar.title')}</DialogTitle>
          <DialogDescription>
            {activeTab === 'url' 
              ? t('addCalendar.fromUrl')
              : t('addCalendar.uploadFile')}
          </DialogDescription>
        </DialogHeader>
        
        <Tabs defaultValue="url" value={activeTab} onValueChange={setActiveTab}>
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="url">{t('addCalendar.fromUrl')}</TabsTrigger>
            <TabsTrigger value="file">{t('addCalendar.uploadFile')}</TabsTrigger>
          </TabsList>
          
          <TabsContent value="url">
            <Form {...urlForm}>
              <form onSubmit={urlForm.handleSubmit(onUrlSubmit)} className="space-y-4 py-4">
                <FormField
                  control={urlForm.control}
                  name="name"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>{t('addCalendar.calendarName')}</FormLabel>
                      <FormControl>
                        <Input 
                          placeholder={t('addCalendar.calendarNamePlaceholder')}
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                
                <FormField
                  control={urlForm.control}
                  name="url"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>{t('addCalendar.icsUrl')}</FormLabel>
                      <FormControl>
                        <div className="relative">
                          <Link className="absolute left-3 top-2.5 h-4 w-4 text-gray-400" />
                          <Input 
                            className="pl-10"
                            placeholder={t('addCalendar.icsUrlPlaceholder')}
                            {...field}
                          />
                        </div>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                
                <FormField
                  control={urlForm.control}
                  name="color"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>{t('addCalendar.selectColor')}</FormLabel>
                      <FormControl>
                        <div className="flex items-center gap-2">
                          <Input
                            type="color"
                            className="w-12 h-8 p-1"
                            {...field}
                          />
                          <div
                            className="w-6 h-6 rounded-full border"
                            style={{ backgroundColor: field.value }}
                          ></div>
                          <Input
                            type="text"
                            className="flex-1"
                            value={field.value}
                            onChange={field.onChange}
                          />
                        </div>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                
                <DialogFooter>
                  <Button 
                    type="submit" 
                    disabled={urlMutation.isPending}
                  >
                    {urlMutation.isPending 
                      ? t('addCalendar.addingCalendar')
                      : t('common.save')}
                  </Button>
                </DialogFooter>
              </form>
            </Form>
          </TabsContent>
          
          <TabsContent value="file">
            <Form {...fileForm}>
              <form onSubmit={fileForm.handleSubmit(onFileSubmit)} className="space-y-4 py-4">
                <FormField
                  control={fileForm.control}
                  name="name"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>{t('addCalendar.calendarName')}</FormLabel>
                      <FormControl>
                        <Input 
                          placeholder={t('addCalendar.calendarNamePlaceholder')}
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                
                <div className="space-y-2">
                  <Label>{t('addCalendar.chooseFile')}</Label>
                  <div className="border-2 border-dashed rounded-md p-6 text-center">
                    <Upload className="h-8 w-8 mx-auto text-gray-400 mb-2" />
                    <p className="text-sm text-gray-500 mb-2">{t('addCalendar.dragDrop')}</p>
                    <p className="text-sm text-gray-500 mb-4">{t('addCalendar.or')}</p>
                    <div className="relative">
                      <input
                        type="file"
                        id="file-upload"
                        className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                        accept=".ics"
                        onChange={handleFileChange}
                      />
                      <Button type="button" size="sm" className="relative z-10">
                        {t('addCalendar.browse')}
                      </Button>
                    </div>
                    {file && (
                      <p className="text-sm text-gray-700 mt-2">
                        {file.name} ({(file.size / 1024).toFixed(1)} KB)
                      </p>
                    )}
                  </div>
                </div>
                
                <FormField
                  control={fileForm.control}
                  name="color"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>{t('addCalendar.selectColor')}</FormLabel>
                      <FormControl>
                        <div className="flex items-center gap-2">
                          <Input
                            type="color"
                            className="w-12 h-8 p-1"
                            {...field}
                          />
                          <div
                            className="w-6 h-6 rounded-full border"
                            style={{ backgroundColor: field.value }}
                          ></div>
                          <Input
                            type="text"
                            className="flex-1"
                            value={field.value}
                            onChange={field.onChange}
                          />
                        </div>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                
                <DialogFooter>
                  <Button 
                    type="submit"
                    disabled={fileMutation.isPending || !file}
                  >
                    {fileMutation.isPending
                      ? t('addCalendar.addingCalendar')
                      : t('common.save')}
                  </Button>
                </DialogFooter>
              </form>
            </Form>
          </TabsContent>
        </Tabs>
      </DialogContent>
    </Dialog>
  );
}
