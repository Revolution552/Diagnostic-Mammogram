"use client";

import React from 'react';
import MessageDisplay from './MessageDisplay'; // Import the reusable message component

interface AuthenticationViewProps {
    username: string;
    setUsername: (username: string) => void;
    password: string;
    setPassword: (password: string) => void;
    handleLogin: (event: React.FormEvent) => Promise<void>;
    loginLoading: boolean;
    loginError: string | null;
    loginSuccessMessage: string | null;
    authToken: string | null;
    onForgotPasswordClick: () => void; // Added: Callback to switch to forgot password view
}

const AuthenticationView: React.FC<AuthenticationViewProps> = ({
                                                                   username, setUsername, password, setPassword,
                                                                   handleLogin, loginLoading, loginError, loginSuccessMessage, authToken,
                                                                   onForgotPasswordClick // Destructure the new prop
                                                               }) => (
    <>
        <h1 className="text-4xl font-extrabold text-center text-gray-800 mb-8 tracking-tight">
            <span className="bg-clip-text text-transparent bg-gradient-to-r from-purple-600 to-pink-700">
                Login
            </span>
        </h1>

        <form onSubmit={handleLogin} className="space-y-6">
            <div>
                <label htmlFor="username" className="block text-gray-700 text-sm font-semibold mb-2">
                    Username:
                </label>
                <input
                    type="text"
                    id="username"
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent outline-none transition-all duration-200"
                    placeholder="Enter your username"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    required
                />
            </div>

            <div>
                <label htmlFor="password" className="block text-gray-700 text-sm font-semibold mb-2">
                    Password:
                </label>
                <input
                    type="password"
                    id="password"
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent outline-none transition-all duration-200"
                    placeholder="Enter your password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                />
            </div>

            <button
                type="submit"
                disabled={loginLoading}
                className={`w-full py-3 px-6 rounded-full font-bold text-white transition-all duration-300 ease-in-out shadow-lg flex items-center justify-center space-x-2
                    ${loginLoading ? 'bg-gray-400 cursor-not-allowed' : 'bg-purple-600 hover:bg-purple-700 transform hover:scale-105 active:scale-95'}`}
            >
                {loginLoading ? (
                    <>
                        <svg className="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                        </svg>
                        <span>Logging In...</span>
                    </>
                ) : (
                    <>
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3v-1m3-4H3"></path>
                        </svg>
                        <span>Login</span>
                    </>
                )}
            </button>
            <div className="text-center mt-4">
                <button
                    type="button" // Important: type="button" to prevent form submission
                    onClick={onForgotPasswordClick}
                    className="text-blue-600 hover:text-blue-800 text-sm font-semibold transition-colors duration-200"
                >
                    Forgot Password?
                </button>
            </div>
        </form>

        {loginError && <MessageDisplay type="error" message={loginError} />}

        {loginSuccessMessage && (
            <MessageDisplay
                type="success"
                message={loginSuccessMessage}
                extraContent={authToken && (
                    <div className="mt-4">
                        <h3 className="text-lg font-semibold text-green-800">Authentication Token:</h3>
                        <pre className="whitespace-pre-wrap font-mono text-sm bg-green-50 p-3 rounded-md overflow-x-auto border border-green-200">
                            {authToken}
                        </pre>
                        <p className="text-sm mt-2 text-green-600">
                            (This token is now being used for authenticated requests.)
                        </p>
                    </div>
                )}
            />
        )}
    </>
);

export default AuthenticationView;
