import React from 'react';
import { Link, useLocation } from 'wouter';
import { LayoutDashboard, Calendar, Clock, Settings } from 'lucide-react';
import { cn } from '@/lib/utils';
import { useTranslations } from '@/hooks/use-translations';

export function MobileNav() {
  const [location] = useLocation();
  const { t } = useTranslations();

  const isActive = (path: string) => {
    return location === path;
  };

  const navItems = [
    {
      name: t('sidebar.dashboard'),
      href: '/',
      icon: LayoutDashboard
    },
    {
      name: t('sidebar.myCalendars'),
      href: '/calendar',
      icon: Calendar
    },
    {
      name: t('sidebar.freeSlots'),
      href: '/free-slots',
      icon: Clock
    },
    {
      name: t('sidebar.settings'),
      href: '/settings',
      icon: Settings
    }
  ];

  return (
    <div className="md:hidden bg-white border-t border-gray-200">
      <div className="flex justify-around">
        {navItems.map((item) => (
          <Link key={item.href} href={item.href}>
            <a className={cn(
              "flex flex-col items-center py-2",
              isActive(item.href) ? "text-primary" : "text-gray-500 hover:text-primary"
            )}>
              <item.icon className="h-5 w-5" />
              <span className="text-xs mt-1">{item.name}</span>
            </a>
          </Link>
        ))}
      </div>
    </div>
  );
}
