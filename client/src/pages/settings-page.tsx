import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Layout } from '@/components/layout';
import { useTranslations } from '@/hooks/use-translations';
import { useAuth } from '@/hooks/use-auth';
import { useMutation } from '@tanstack/react-query';
import { apiRequest, queryClient } from '@/lib/queryClient';
import { useToast } from '@/hooks/use-toast';
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from '@/components/ui/tabs';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';

// Schema for profile form
const profileSchema = z.object({
  name: z.string().min(1, { message: 'Name is required' }),
  email: z.string().email({ message: 'Valid email is required' }),
});

// Schema for password form
const passwordSchema = z.object({
  currentPassword: z.string().min(1, { message: 'Current password is required' }),
  newPassword: z.string().min(8, { message: 'Password must be at least 8 characters' }),
  confirmNewPassword: z.string().min(1, { message: 'Please confirm your password' }),
}).refine(data => data.newPassword === data.confirmNewPassword, {
  message: "Passwords don't match",
  path: ['confirmNewPassword'],
});

// Schema for sync settings form
const syncSettingsSchema = z.object({
  syncInterval: z.number().min(1, { message: 'Sync interval is required' }),
});

type ProfileFormValues = z.infer<typeof profileSchema>;
type PasswordFormValues = z.infer<typeof passwordSchema>;
type SyncSettingsFormValues = z.infer<typeof syncSettingsSchema>;

export default function SettingsPage() {
  const { t, currentLanguage, setLanguage } = useTranslations();
  const { user } = useAuth();
  const { toast } = useToast();
  const [activeTab, setActiveTab] = useState<string>('account');
  
  // Profile form
  const profileForm = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      name: user?.name || '',
      email: user?.email || '',
    },
  });
  
  // Password form
  const passwordForm = useForm<PasswordFormValues>({
    resolver: zodResolver(passwordSchema),
    defaultValues: {
      currentPassword: '',
      newPassword: '',
      confirmNewPassword: '',
    },
  });
  
  // Sync settings form
  const syncSettingsForm = useForm<SyncSettingsFormValues>({
    resolver: zodResolver(syncSettingsSchema),
    defaultValues: {
      syncInterval: 60,
    },
  });
  
  // Profile update mutation
  const profileMutation = useMutation({
    mutationFn: async (data: ProfileFormValues) => {
      return await apiRequest('PUT', '/api/user/profile', data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['/api/user'] });
      toast({
        title: t('common.success'),
        description: t('settings.updateSuccess'),
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
  
  // Password update mutation
  const passwordMutation = useMutation({
    mutationFn: async (data: PasswordFormValues) => {
      return await apiRequest('PUT', '/api/user/password', {
        currentPassword: data.currentPassword,
        newPassword: data.newPassword,
      });
    },
    onSuccess: () => {
      toast({
        title: t('common.success'),
        description: t('settings.passwordUpdated'),
      });
      passwordForm.reset();
    },
    onError: (error) => {
      toast({
        title: t('common.error'),
        description: error.message,
        variant: 'destructive',
      });
    },
  });
  
  // Sync settings mutation
  const syncSettingsMutation = useMutation({
    mutationFn: async (data: SyncSettingsFormValues) => {
      return await apiRequest('PUT', '/api/settings/sync', data);
    },
    onSuccess: () => {
      toast({
        title: t('common.success'),
        description: t('settings.updateSuccess'),
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
  
  // Handle form submissions
  const onProfileSubmit = (data: ProfileFormValues) => {
    profileMutation.mutate(data);
  };
  
  const onPasswordSubmit = (data: PasswordFormValues) => {
    passwordMutation.mutate(data);
  };
  
  const onSyncSettingsSubmit = (data: SyncSettingsFormValues) => {
    syncSettingsMutation.mutate(data);
  };
  
  // Handle language change
  const handleLanguageChange = (language: string) => {
    setLanguage(language as 'en' | 'es');
  };

  return (
    <Layout>
      <div className="container mx-auto">
        <div className="mb-6">
          <h2 className="text-2xl font-semibold text-gray-800">{t('settings.title')}</h2>
          <p className="text-gray-600">{t('settings.subtitle')}</p>
        </div>
        
        <Tabs defaultValue="account" value={activeTab} onValueChange={setActiveTab}>
          <div className="flex flex-col md:flex-row gap-6">
            <div className="md:w-64 flex-shrink-0">
              <Card>
                <CardContent className="p-4">
                  <TabsList className="flex flex-col items-start space-y-1 h-auto">
                    <TabsTrigger 
                      value="account" 
                      className="w-full justify-start text-left px-3"
                    >
                      {t('settings.account')}
                    </TabsTrigger>
                    <TabsTrigger 
                      value="language" 
                      className="w-full justify-start text-left px-3"
                    >
                      {t('settings.language')}
                    </TabsTrigger>
                    <TabsTrigger 
                      value="syncSettings" 
                      className="w-full justify-start text-left px-3"
                    >
                      {t('settings.syncSettings')}
                    </TabsTrigger>
                  </TabsList>
                </CardContent>
              </Card>
            </div>
            
            <div className="flex-1">
              <TabsContent value="account" className="m-0">
                <Card>
                  <CardHeader>
                    <CardTitle>{t('settings.personalInfo')}</CardTitle>
                    <CardDescription>
                      {t('settings.profile')}
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    <Form {...profileForm}>
                      <form onSubmit={profileForm.handleSubmit(onProfileSubmit)} className="space-y-4">
                        <FormField
                          control={profileForm.control}
                          name="name"
                          render={({ field }) => (
                            <FormItem>
                              <FormLabel>{t('auth.name')}</FormLabel>
                              <FormControl>
                                <Input {...field} />
                              </FormControl>
                              <FormMessage />
                            </FormItem>
                          )}
                        />
                        
                        <FormField
                          control={profileForm.control}
                          name="email"
                          render={({ field }) => (
                            <FormItem>
                              <FormLabel>{t('auth.email')}</FormLabel>
                              <FormControl>
                                <Input {...field} />
                              </FormControl>
                              <FormMessage />
                            </FormItem>
                          )}
                        />
                        
                        <Button 
                          type="submit" 
                          disabled={profileMutation.isPending}
                        >
                          {profileMutation.isPending ? t('common.loading') : t('common.save')}
                        </Button>
                      </form>
                    </Form>
                  </CardContent>
                </Card>
                
                <Card className="mt-6">
                  <CardHeader>
                    <CardTitle>{t('settings.security')}</CardTitle>
                    <CardDescription>
                      {t('settings.changePassword')}
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    <Form {...passwordForm}>
                      <form onSubmit={passwordForm.handleSubmit(onPasswordSubmit)} className="space-y-4">
                        <FormField
                          control={passwordForm.control}
                          name="currentPassword"
                          render={({ field }) => (
                            <FormItem>
                              <FormLabel>{t('settings.currentPassword')}</FormLabel>
                              <FormControl>
                                <Input type="password" {...field} />
                              </FormControl>
                              <FormMessage />
                            </FormItem>
                          )}
                        />
                        
                        <FormField
                          control={passwordForm.control}
                          name="newPassword"
                          render={({ field }) => (
                            <FormItem>
                              <FormLabel>{t('settings.newPassword')}</FormLabel>
                              <FormControl>
                                <Input type="password" {...field} />
                              </FormControl>
                              <FormMessage />
                            </FormItem>
                          )}
                        />
                        
                        <FormField
                          control={passwordForm.control}
                          name="confirmNewPassword"
                          render={({ field }) => (
                            <FormItem>
                              <FormLabel>{t('settings.confirmNewPassword')}</FormLabel>
                              <FormControl>
                                <Input type="password" {...field} />
                              </FormControl>
                              <FormMessage />
                            </FormItem>
                          )}
                        />
                        
                        <Button 
                          type="submit" 
                          disabled={passwordMutation.isPending}
                        >
                          {passwordMutation.isPending ? t('common.loading') : t('common.save')}
                        </Button>
                      </form>
                    </Form>
                  </CardContent>
                </Card>
                
                <Card className="mt-6">
                  <CardHeader>
                    <CardTitle className="text-red-600">{t('settings.dangerZone')}</CardTitle>
                    <CardDescription>
                      {t('settings.deleteAccount')}
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    <p className="text-sm text-gray-500 mb-4">
                      {t('settings.deleteWarning')}
                    </p>
                    <Button variant="destructive">
                      {t('settings.deleteConfirm')}
                    </Button>
                  </CardContent>
                </Card>
              </TabsContent>
              
              <TabsContent value="language" className="m-0">
                <Card>
                  <CardHeader>
                    <CardTitle>{t('settings.language')}</CardTitle>
                    <CardDescription>
                      {t('settings.subtitle')}
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-4">
                      <div className="grid w-full max-w-sm items-center gap-1.5">
                        <Label htmlFor="language">{t('settings.language')}</Label>
                        <Select
                          defaultValue={currentLanguage}
                          onValueChange={handleLanguageChange}
                        >
                          <SelectTrigger id="language">
                            <SelectValue placeholder="Select language" />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="en">English</SelectItem>
                            <SelectItem value="es">Español</SelectItem>
                          </SelectContent>
                        </Select>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </TabsContent>
              
              <TabsContent value="syncSettings" className="m-0">
                <Card>
                  <CardHeader>
                    <CardTitle>{t('settings.syncSettings')}</CardTitle>
                    <CardDescription>
                      {t('calendar.subtitle')}
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    <Form {...syncSettingsForm}>
                      <form onSubmit={syncSettingsForm.handleSubmit(onSyncSettingsSubmit)} className="space-y-4">
                        <FormField
                          control={syncSettingsForm.control}
                          name="syncInterval"
                          render={({ field }) => (
                            <FormItem>
                              <FormLabel>{t('settings.syncInterval')}</FormLabel>
                              <Select
                                onValueChange={(value) => field.onChange(parseInt(value))}
                                defaultValue={field.value.toString()}
                              >
                                <FormControl>
                                  <SelectTrigger>
                                    <SelectValue placeholder="Select interval" />
                                  </SelectTrigger>
                                </FormControl>
                                <SelectContent>
                                  <SelectItem value="5">5 {t('settings.minutes')}</SelectItem>
                                  <SelectItem value="15">15 {t('settings.minutes')}</SelectItem>
                                  <SelectItem value="30">30 {t('settings.minutes')}</SelectItem>
                                  <SelectItem value="60">60 {t('settings.minutes')}</SelectItem>
                                </SelectContent>
                              </Select>
                              <FormMessage />
                            </FormItem>
                          )}
                        />
                        
                        <Button 
                          type="submit" 
                          disabled={syncSettingsMutation.isPending}
                        >
                          {syncSettingsMutation.isPending ? t('common.loading') : t('common.save')}
                        </Button>
                      </form>
                    </Form>
                  </CardContent>
                </Card>
              </TabsContent>
            </div>
          </div>
        </Tabs>
      </div>
    </Layout>
  );
}

// Label component for this page
function Label({ htmlFor, children }: { htmlFor: string, children: React.ReactNode }) {
  return (
    <label
      htmlFor={htmlFor}
      className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
    >
      {children}
    </label>
  );
}
