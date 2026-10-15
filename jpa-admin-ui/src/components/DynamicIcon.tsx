import React from 'react';
import * as Icons from 'lucide-react';

interface DynamicIconProps {
  name: string;
  className?: string;
  size?: number;
}

export const DynamicIcon: React.FC<DynamicIconProps> = ({ name, className = 'w-5 h-5', size = 18 }) => {
  // Convert kebab-case or lowercase to PascalCase (e.g. shopping-cart -> ShoppingCart, users -> Users)
  const pascalName = name
    .split('-')
    .map(part => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
    .join('');

  // @ts-ignore
  const IconComponent = Icons[pascalName] || Icons[name] || Icons.Database;

  return <IconComponent className={className} size={size} />;
};
