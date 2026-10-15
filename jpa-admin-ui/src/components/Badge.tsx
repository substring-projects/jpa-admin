import React from 'react';
import { Badge as ShadcnBadge } from './ui/badge';
import { BadgeColor } from '../types/metadata';

interface BadgeProps extends React.HTMLAttributes<HTMLDivElement> {
  color?: BadgeColor | string;
  variant?: 'default' | 'secondary' | 'destructive' | 'outline';
  children: React.ReactNode;
}

export const Badge: React.FC<BadgeProps> = ({
  color,
  variant,
  children,
  className,
  ...props
}) => {
  return (
    <ShadcnBadge
      badgeColor={color}
      variant={variant}
      className={className}
      {...props}
    >
      {children}
    </ShadcnBadge>
  );
};
