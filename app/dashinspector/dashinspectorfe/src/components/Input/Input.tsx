import React from 'react';
import './Input.css';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
    label?: React.ReactNode;
    error?: string;
    fullWidth?: boolean;
}

export const Input: React.FC<InputProps> = ({
    label,
    error,
    fullWidth = false,
    className = '',
    id,
    ...props
}) => {
    const inputId = id || React.useId();

    return (
        <div className={`input-wrapper ${fullWidth ? 'input-full-width' : ''} ${className}`}>
            {label && (
                <label htmlFor={inputId} className="input-label">
                    {label}
                </label>
            )}
            <input
                id={inputId}
                className={`input-field ${error ? 'input-error' : ''}`}
                {...props}
            />
            {error && <span className="input-error-message">{error}</span>}
        </div>
    );
};
