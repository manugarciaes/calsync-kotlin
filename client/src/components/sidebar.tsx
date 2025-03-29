import React from 'react';
import { Link, useLocation } from 'wouter';
import { 
  LayoutDashboard, 
  Calendar, 
  Clock, 
  Share2, 
  Settings, 
  X,
  CalendarCheck
} from 'lucide-react';
import { useAuth } from '@/hooks/use-auth';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { ScrollArea } from '@/components/ui/scroll-area';
import { cn } from '@/lib/utils';
import { useTranslations } from '@/hooks/use-translations';

type SidebarProps = {
  isOpen: boolean;
  onClose: () => void;
};

export function Sidebar({ isOpen, onClose }: SidebarProps) {
  const [location] = useLocation();
  const { user } = useAuth();
  const { t } = useTranslations();

  // Get initials for avatar
  const getInitials = () => {
    if (!user) return '';
    
    if (user.name) {
      return user.name
        .split(' ')
        .map(part => part[0])
        .join('')
        .toUpperCase()
        .substring(0, 2);
    }
    
    return user.username.substring(0, 2).toUpperCase();
  };

  const isActive = (path: string) => {
    return location === path;
  };

  // Sidebar navigation items
  const navItems = [
    {
      name: t('sidebar.dashboard'),
      href: '/',
      icon: LayoutDashboard,
    },
    {
      name: t('sidebar.myCalendars'),
      href: '/calendar',
      icon: Calendar,
    },
    {
      name: t('sidebar.freeSlots'),
      href: '/free-slots',
      icon: Clock,
    },
    {
      name: t('sidebar.sharedCalendars'),
      href: '/shared-calendars',
      icon: Share2,
    },
    {
      name: t('sidebar.bookingPage'),
      href: '/booking-page',
      icon: CalendarCheck,
    },
    {
      name: t('sidebar.settings'),
      href: '/settings',
      icon: Settings,
    },
  ];

  return (
    <>
      {/* Desktop sidebar */}
      <aside className="w-64 bg-white border-r border-gray-200 hidden md:block flex-shrink-0">
        <div className="h-full flex flex-col">
          <ScrollArea className="flex-1 p-4">
            <nav className="space-y-1">
              {navItems.map((item) => (
                <Link key={item.href} href={item.href}>
                  <a
                    className={cn(
                      'flex items-center px-3 py-2 text-sm font-medium rounded-md',
                      isActive(item.href)
                        ? 'bg-primary/10 text-primary'
                        : 'text-gray-700 hover:bg-gray-100'
                    )}
                  >
                    <item.icon 
                      className={cn(
                        'mr-3 h-5 w-5',
                        isActive(item.href) ? 'text-primary' : 'text-gray-400'
                      )} 
                    />
                    {item.name}
                  </a>
                </Link>
              ))}
            </nav>
          </ScrollArea>
          
          {user && (
            <div className="p-4 border-t border-gray-200">
              <div className="group block">
                <div className="flex items-center">
                  <Avatar className="h-8 w-8">
                    <AvatarFallback>{getInitials()}</AvatarFallback>
                  </Avatar>
                  <div className="ml-3">
                    <p className="text-sm font-medium text-gray-700 group-hover:text-primary">
                      {user.name || user.username}
                    </p>
                    <p className="text-xs font-medium text-gray-500">
                      {user.email}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </aside>

      {/* Mobile sidebar (overlay) */}
      <div 
        className={cn(
          'fixed inset-0 flex z-40 md:hidden',
          isOpen ? 'block' : 'hidden'
        )}
        role="dialog" 
        aria-modal="true"
      >
        <div 
          className="fixed inset-0 bg-gray-600 bg-opacity-50" 
          aria-hidden="true"
          onClick={onClose}
        ></div>
        
        <div className="relative flex-1 flex flex-col max-w-xs w-full bg-white">
          <div className="absolute top-0 right-0 -mr-12 pt-2">
            <button 
              className="ml-1 flex items-center justify-center h-10 w-10 rounded-full focus:outline-none focus:ring-2 focus:ring-inset focus:ring-white"
              onClick={onClose}
            >
              <span className="sr-only">Close sidebar</span>
              <X className="h-6 w-6 text-white" />
            </button>
          </div>
          
          <div className="flex-1 h-0 pt-5 pb-4 overflow-y-auto">
            <div className="flex-shrink-0 flex items-center px-4">
              <Calendar className="h-6 w-6 text-primary" />
              <h1 className="text-xl font-semibold text-primary ml-2">CalSync</h1>
            </div>
            <nav className="mt-5 px-2 space-y-1">
              {navItems.map((item) => (
                <Link key={item.href} href={item.href}>
                  <a
                    className={cn(
                      'flex items-center px-3 py-2 text-sm font-medium rounded-md',
                      isActive(item.href)
                        ? 'bg-primary/10 text-primary'
                        : 'text-gray-700 hover:bg-gray-100'
                    )}
                    onClick={onClose}
                  >
                    <item.icon 
                      className={cn(
                        'mr-3 h-5 w-5',
                        isActive(item.href) ? 'text-primary' : 'text-gray-400'
                      )} 
                    />
                    {item.name}
                  </a>
                </Link>
              ))}
            </nav>
          </div>
          
          {user && (
            <div className="flex-shrink-0 flex border-t border-gray-200 p-4">
              <div className="flex-shrink-0 group block">
                <div className="flex items-center">
                  <Avatar className="h-8 w-8">
                    <AvatarFallback>{getInitials()}</AvatarFallback>
                  </Avatar>
                  <div className="ml-3">
                    <p className="text-sm font-medium text-gray-700 group-hover:text-primary">
                      {user.name || user.username}
                    </p>
                    <p className="text-xs font-medium text-gray-500">
                      {user.email}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </>
  );
}
