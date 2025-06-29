"use client";

import React from 'react';

interface MessageDisplayProps {
    type: 'success' | 'error';
    message: string | null;
    extraContent?: React.ReactNode; // Optional for additional content like auth token or suggestions
}

const MessageDisplay: React.FC<MessageDisplayProps> = ({ type, message, extraContent }) => {
    if (!message) return null;

    const baseClasses = "mt-6 p-4 border-l-4 rounded-lg shadow-sm";
    const successClasses = "bg-green-100 border-green-500 text-green-700";
    const errorClasses = "bg-red-100 border-red-500 text-red-700";

    return (
        <div className={`${baseClasses} ${type === 'success' ? successClasses : errorClasses}`}>
            <p className="font-semibold">{type === 'success' ? 'Success:' : 'Error:'}</p>
            <p>{message}</p>
            {extraContent && <div className="mt-4">{extraContent}</div>}
        </div>
    );
};

export default MessageDisplay;
