"use client";

import React, { useState } from 'react';
import MessageDisplay from './MessageDisplay';

interface ResetPasswordViewProps {
    API_BASE_URL: string;
    onBackToLogin: () => void;
}

const ResetPasswordView: React.FC<ResetPasswordViewProps> = ({ API_BASE_URL, onBackToLogin }) => {
    const [token, setToken] = useState<string>('');
    const [newPassword, setNewPassword] = useState<string>('');
    const [confirmPassword, setConfirmPassword] = useState<string>('');
    const [loading, setLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);
    const [successMessage, setSuccessMessage] = useState<string | null>(null);

    const handleSubmit = async (event: React.FormEvent) => {
        event.preventDefault();
        setLoading(true);
        setError(null);
        setSuccessMessage(null);

        if (newPassword !== confirmPassword) {
            setError('New password and confirm password do not match.');
            setLoading(false);
            return;
        }

        try {
            const response = await fetch(`${API_BASE_URL}/api/auth/reset-password`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ token, newPassword, confirmPassword }),
            });

            const data = await response.json();

            if (response.ok && data.status === "success") { // Assuming backend sends { status: "success", message: "..." }
                setSuccessMessage(data.message || 'Password has been reset successfully. You can now log in.');
                setToken('');
                setNewPassword('');
                setConfirmPassword('');
                // Optionally, automatically navigate back to login after a short delay
                setTimeout(onBackToLogin, 3000);
            } else {
                setError(data.message || `Failed to reset password. Server responded with status: ${response.status} ${response.statusText}.`);
            }
        } catch (err: any) {
            console.error('Error resetting password:', err);
            setError('Network error or an unexpected issue occurred. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <>
            <h1 className="text-4xl font-extrabold text-center text-gray-800 mb-8 tracking-tight">
                <span className="bg-clip-text text-transparent bg-gradient-to-r from-purple-600 to-pink-700">
                    Reset Password
                </span>
            </h1>

            <form onSubmit={handleSubmit} className="space-y-6">
                <div>
                    <label htmlFor="token" className="block text-gray-700 text-sm font-semibold mb-2">
                        Reset Token:
                    </label>
                    <input
                        type="text"
                        id="token"
                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent outline-none transition-all duration-200"
                        placeholder="Enter the token from your email"
                        value={token}
                        onChange={(e) => setToken(e.target.value)}
                        required
                    />
                </div>

                <div>
                    <label htmlFor="newPassword" className="block text-gray-700 text-sm font-semibold mb-2">
                        New Password:
                    </label>
                    <input
                        type="password"
                        id="newPassword"
                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent outline-none transition-all duration-200"
                        placeholder="Enter your new password"
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                        required
                    />
                </div>

                <div>
                    <label htmlFor="confirmPassword" className="block text-gray-700 text-sm font-semibold mb-2">
                        Confirm New Password:
                    </label>
                    <input
                        type="password"
                        id="confirmPassword"
                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent outline-none transition-all duration-200"
                        placeholder="Confirm your new password"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
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
                            <span>Resetting...</span>
                        </>
                    ) : (
                        <span>Reset Password</span>
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

export default ResetPasswordView;
