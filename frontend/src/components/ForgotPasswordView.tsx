"use client";

import React, { useState } from 'react';
import MessageDisplay from './MessageDisplay';

interface ForgotPasswordViewProps {
    API_BASE_URL: string;
    onBackToLogin: () => void;
    onResetPasswordSuccess: () => void; // Callback to switch to reset password form after successful email request
}

const ForgotPasswordView: React.FC<ForgotPasswordViewProps> = ({ API_BASE_URL, onBackToLogin, onResetPasswordSuccess }) => {
    const [email, setEmail] = useState<string>('');
    const [loading, setLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);
    const [successMessage, setSuccessMessage] = useState<string | null>(null);

    const handleSubmit = async (event: React.FormEvent) => {
        event.preventDefault();
        setLoading(true);
        setError(null);
        setSuccessMessage(null);

        try {
            const response = await fetch(`${API_BASE_URL}/api/auth/forgot-password`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email }),
            });

            const data = await response.json();

            if (response.ok && data.status === "success") { // Assuming backend sends { status: "success", message: "..." }
                setSuccessMessage(data.message || 'Password reset link sent to your email.');
                setEmail(''); // Clear email field
                onResetPasswordSuccess(); // Optionally, switch to reset password view immediately if token is expected in URL
            } else {
                setError(data.message || `Failed to send reset link. Server responded with status: ${response.status} ${response.statusText}.`);
            }
        } catch (err: any) {
            console.error('Error requesting password reset:', err);
            setError('Network error or an unexpected issue occurred. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <>
            <h1 className="text-4xl font-extrabold text-center text-gray-800 mb-8 tracking-tight">
                <span className="bg-clip-text text-transparent bg-gradient-to-r from-purple-600 to-pink-700">
                    Forgot Password
                </span>
            </h1>

            <form onSubmit={handleSubmit} className="space-y-6">
                <div>
                    <label htmlFor="email" className="block text-gray-700 text-sm font-semibold mb-2">
                        Enter your email address:
                    </label>
                    <input
                        type="email"
                        id="email"
                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent outline-none transition-all duration-200"
                        placeholder="your.email@example.com"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                    />
                </div>

                <button
                    type="submit"
                    disabled={loading}
                    className={`w-full py-3 px-6 rounded-full font-bold text-white transition-all duration-300 ease-in-out shadow-lg flex items-center justify-center space-x-2
                        ${loading ? 'bg-gray-400 cursor-not-allowed' : 'bg-purple-600 hover:bg-purple-700 transform hover:scale-105 active:scale-95'}`}
                >
                    {loading ? (
                        <>
                            <svg className="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                            </svg>
                            <span>Sending...</span>
                        </>
                    ) : (
                        <span>Send Reset Link</span>
                    )}
                </button>
            </form>

            <MessageDisplay type="error" message={error} />
            <MessageDisplay type="success" message={successMessage} />

            <div className="text-center mt-6">
                <button
                    type="button"
                    onClick={onBackToLogin}
                    className="text-blue-600 hover:text-blue-800 text-sm font-semibold transition-colors duration-200"
                >
                    Back to Login
                </button>
            </div>
        </>
    );
};

export default ForgotPasswordView;
