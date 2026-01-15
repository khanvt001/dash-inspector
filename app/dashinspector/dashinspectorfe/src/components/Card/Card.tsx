import React from 'react';
import './Card.css';

interface CardProps {
    children: React.ReactNode;
    className?: string;
    title?: string;
    action?: React.ReactNode;
    noPadding?: boolean;
}

export const Card: React.FC<CardProps> = ({
    children,
    className = '',
    title,
    action,
    noPadding = false,
}) => {
    return (
        <div className={`card ${className}`}>
            {(title || action) && (
                <div className="card-header">
                    {title && <h3 className="card-title">{title}</h3>}
                    {action && <div className="card-action">{action}</div>}
                </div>
            )}
            <div className={`card-body ${noPadding ? 'no-padding' : ''}`}>
                {children}
            </div>
        </div>
    );
};
