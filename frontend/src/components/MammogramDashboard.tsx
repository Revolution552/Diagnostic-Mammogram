"use client"; // This component itself uses useState, so it must be a client component

import React, { useState } from 'react';
// Import the separated components
import AuthenticationView from './AuthenticationView';
import MammogramUploadView from './MammogramUploadView';
import PatientManagementView from './PatientManagementView';
import ReportManagementView from './ReportManagementView';
import UserManagementView from './UserManagementView';

const MammogramDashboard = () => {
    // Shared Authentication States (managed here as they affect the entire app's access)
    const [username, setUsername] = useState<string>('');
    const [password, setPassword] = useState<string>('');
    const [authToken, setAuthToken] = useState<string | null>(null);
    const [isLoggedIn, setIsLoggedIn] = useState<boolean>(false);

    // Shared UI Feedback States for Login (passed to AuthenticationView)
    const [loginLoading, setLoginLoading] = useState<boolean>(false);
    const [loginError, setLoginError] = useState<string | null>(null);
    const [loginSuccessMessage, setLoginSuccessMessage] = useState<string | null>(null);

    // Current View State
    const [currentView, setCurrentView] = useState<string>('mammogram');

    // Base URL for your Spring Boot API.
    const API_BASE_URL = 'http://localhost:8080';

    /**
     * Helper to get common fetch headers, conditionally including Authorization.
     * This function is defined here because authToken is in this scope and passed down.
     */
    const getAuthHeaders = (contentType: string = 'application/json'): HeadersInit => {
        const headers: Record<string, string> = {
            'Content-Type': contentType,
        };
        if (authToken) {
            headers['Authorization'] = `Bearer ${authToken}`;
        }
        return headers;
    };

    /**
     * Handles the login form submission.
     * This function remains in the main dashboard as it manages core login state.
     */
    const handleLogin = async (event: React.FormEvent) => {
        event.preventDefault();

        setLoginLoading(true);
        // Clear both success and error messages at the start of every login attempt
        setLoginError(null);
        setLoginSuccessMessage(null);
        setAuthToken(null);
        setIsLoggedIn(false);

        const requestBody = { username, password };

        try {
            const response = await fetch(`${API_BASE_URL}/api/auth/authenticate`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(requestBody),
            });

            const data = await response.json();

            // *** CRITICAL CHANGE HERE: Check for data.status === "success" (string) ***
            // Strict check for a true successful login
            // This requires the HTTP response to be OK (2xx) AND
            // the backend's internal status to be "success" AND
            // a token to be present in the data.
            if (response.ok && data.status === "success" && data.data && data.data.token) {
                setAuthToken(data.data.token);
                setIsLoggedIn(true);
                setCurrentView('mammogram'); // Switch to mammogram view after login
                setLoginSuccessMessage(data.message || 'Authentication successful!'); // Use message from backend
                setUsername('');
                setPassword('');
            } else {
                // This block handles ALL failure scenarios:
                // 1. Non-2xx HTTP responses (e.g., 401 Unauthorized, 500 Internal Server Error)
                // 2. 2xx HTTP responses where backend's internal status is NOT "success"
                //    (e.g., for incorrect credentials, your backend might send status: "failure" or similar, or just omit the token)
                // 3. 2xx HTTP responses where status is "success", but token is missing (which is still a login failure as we can't proceed).

                // Construct an error message from backend's response or a generic one
                const errorMessage = data.message || `Authentication failed. Server responded with HTTP Status: ${response.status} ${response.statusText}.`;
                setLoginError(errorMessage);
            }
        } catch (err: any) {
            console.error('Error during authentication:', err);
            setLoginError('Network error or an unexpected issue occurred during login. Please check your API_BASE_URL and network connection.');
        } finally {
            setLoginLoading(false);
        }
    };

    /**
     * Logout function.
     * This function remains in the main dashboard as it resets core authentication state.
     */
    const handleLogout = () => {
        setAuthToken(null);
        setIsLoggedIn(false);
        setUsername('');
        setPassword('');
        setCurrentView('mammogram'); // Reset view on logout
        setLoginError(null); // Clear any previous login errors
        setLoginSuccessMessage(null); // Clear any previous login messages
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-purple-100 to-pink-200 flex items-center justify-center p-4 font-inter">
            <div className="bg-white p-8 rounded-2xl shadow-xl w-full max-w-4xl transform transition-all duration-300 hover:scale-[1.01] overflow-hidden">

                {!isLoggedIn ? (
                    // Render AuthenticationView when not logged in
                    <AuthenticationView
                        username={username}
                        setUsername={setUsername}
                        password={password}
                        setPassword={setPassword}
                        handleLogin={handleLogin}
                        loginLoading={loginLoading}
                        loginError={loginError}
                        loginSuccessMessage={loginSuccessMessage}
                        authToken={authToken}
                    />
                ) : (
                    // Render authenticated dashboard content
                    <>
                        <div className="flex justify-between items-center mb-8">
                            <h1 className="text-4xl font-extrabold text-gray-800 tracking-tight">
                                <span className="bg-clip-text text-transparent bg-gradient-to-r from-blue-600 to-indigo-700">
                                    {currentView === 'mammogram' ? 'Mammogram Management' :
                                        currentView === 'patients' ? 'Patient Management' :
                                            currentView === 'reports' ? 'Report Management' :
                                                'User Management'}
                                </span>
                            </h1>
                            <button
                                onClick={handleLogout}
                                className="bg-red-500 hover:bg-red-600 text-white font-semibold py-2 px-4 rounded-full shadow-md transition-all duration-200 transform hover:scale-105 active:scale-95"
                            >
                                Logout
                            </button>
                        </div>

                        {/* Navigation Tabs */}
                        <div className="mb-8 border-b border-gray-200">
                            <nav className="-mb-px flex space-x-8" aria-label="Tabs">
                                <button
                                    onClick={() => setCurrentView('mammogram')}
                                    className={`${currentView === 'mammogram' ? 'border-indigo-500 text-indigo-600' : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'} whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm transition-colors duration-200`}
                                >
                                    Upload Mammogram
                                </button>
                                <button
                                    onClick={() => setCurrentView('patients')}
                                    className={`${currentView === 'patients' ? 'border-indigo-500 text-indigo-600' : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'} whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm transition-colors duration-200`}
                                >
                                    Manage Patients
                                </button>
                                <button
                                    onClick={() => setCurrentView('reports')}
                                    className={`${currentView === 'reports' ? 'border-indigo-500 text-indigo-600' : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'} whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm transition-colors duration-200`}
                                >
                                    Manage Reports
                                </button>
                                <button
                                    onClick={() => setCurrentView('users')}
                                    className={`${currentView === 'users' ? 'border-indigo-500 text-indigo-600' : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'} whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm transition-colors duration-200`}
                                >
                                    Manage Users
                                </button>
                            </nav>
                        </div>

                        {/* Conditionally render views based on currentView state */}
                        {currentView === 'mammogram' && (
                            <MammogramUploadView
                                API_BASE_URL={API_BASE_URL}
                                authToken={authToken}
                                getAuthHeaders={getAuthHeaders}
                            />
                        )}

                        {currentView === 'patients' && (
                            <PatientManagementView
                                API_BASE_URL={API_BASE_URL}
                                authToken={authToken}
                                getAuthHeaders={getAuthHeaders}
                            />
                        )}

                        {currentView === 'reports' && (
                            <ReportManagementView
                                API_BASE_URL={API_BASE_URL}
                                authToken={authToken}
                                getAuthHeaders={getAuthHeaders}
                            />
                        )}

                        {currentView === 'users' && (
                            <UserManagementView
                                API_BASE_URL={API_BASE_URL}
                                authToken={authToken}
                                getAuthHeaders={getAuthHeaders}
                                USER_ROLES={['ADMIN', 'DOCTOR', 'RADIOLOGIST', 'PATIENT']} // Pass roles constant
                            />
                        )}
                    </>
                )}

                <div className="mt-8 text-center text-gray-500 text-sm">
                    <p>
                        This application integrates authentication, mammogram upload, and patient and report management functionalities.
                    </p>
                    <p className="mt-2">
                        <strong className="text-gray-600">Important:</strong> Ensure your Spring Boot APIs are running and accessible at the configured `API_BASE_URL`.
                    </p>
                </div>
            </div>
        </div>
    );
};

export default MammogramDashboard;
