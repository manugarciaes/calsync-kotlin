import React from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { StatCard as StatCardType } from '@/types';

interface StatCardProps {
  stat: StatCardType;
}

export function StatCard({ stat }: StatCardProps) {
  const { title, value, icon, color } = stat;

  return (
    <Card>
      <CardContent className="p-4">
        <div className="flex items-center">
          <div className={`rounded-full p-2 mr-4`} style={{ backgroundColor: `${color}20` }}>
            {React.cloneElement(icon as React.ReactElement, { className: "h-5 w-5", style: { color } })}
          </div>
          <div>
            <p className="text-sm text-gray-500">{title}</p>
            <p className="text-2xl font-semibold text-gray-800">{value}</p>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
