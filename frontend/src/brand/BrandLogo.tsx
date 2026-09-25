import { emblemLogo } from './emblemAsset';
import { iconLogo } from './iconAsset';

type BrandLogoProps = {
  variant?: 'icon' | 'emblem' | 'horizontal';
  className?: string;
};

function BrandLogo({ variant = 'horizontal', className = '' }: BrandLogoProps) {
  if (variant === 'icon') {
    return <img className={`brand-image brand-image-icon ${className}`} src={iconLogo} alt="LinePulse" />;
  }

  if (variant === 'emblem') {
    return <img className={`brand-image brand-image-emblem ${className}`} src={emblemLogo} alt="LinePulse" />;
  }

  return (
    <div className={`brand-horizontal ${className}`} aria-label="LinePulse">
      <img className="brand-horizontal-emblem" src={emblemLogo} alt="" aria-hidden="true" />
      <div className="brand-horizontal-copy">
        <strong>LinePulse</strong>
        <span>Smart flow tracking</span>
      </div>
    </div>
  );
}

export default BrandLogo;
