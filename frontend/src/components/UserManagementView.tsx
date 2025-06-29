"use client";

import React, { useState, useEffect } from 'react';
import MessageDisplay from './MessageDisplay'; // Import the reusable message component

interface UserManagementViewProps {
    API_BASE_URL: string;
    authToken: string | null;
    getAuthHeaders: (contentType?: string) => HeadersInit;
    USER_ROLES: string[];
}

const UserManagementView: React.FC<UserManagementViewProps> = ({ API_BASE_URL, authToken, getAuthHeaders, USER_ROLES }) => {
    const [newUser, setNewUser] = useState({
        username: '', password: '', firstName: '',
        lastName: '', email: '', role: 'PATIENT'
    });
    const [users, setUsers] = useState<any[]>([]);
    const [userFilterRole, setUserFilterRole] = useState<string>('ALL');
    const [userSearchQuery, setUserSearchQuery] = useState<string>('');

    const [userLoading, setUserLoading] = useState<boolean>(false);
    const [userError, setUserError] = useState<string | null>(null);
    const [userSuccessMessage, setUserSuccessMessage] = useState<string | null>(null);
    const [usernameSuggestions, setUsernameSuggestions] = useState<string[]>([]);

    const fetchUsers = async () => {
        setUserLoading(true);
        setUserError(null);

        try {
            let fetchedUsers: any[] = [];

            if (userSearchQuery) {
                const searchResponse = await fetch(`${API_BASE_URL}/api/users/search?query=${encodeURIComponent(userSearchQuery)}`, {
                    method: 'GET',
                    headers: getAuthHeaders(),
                });
                const searchData = await searchResponse.json();
                if (searchResponse.ok && searchData.success) {
                    fetchedUsers = searchData.data || [];
                } else {
                    setUserError(searchData.message || 'Failed to search users.');
                }
            } else if (userFilterRole === 'ALL') {
                try {
                    const allUsersResponse = await fetch(`${API_BASE_URL}/api/users`, {
                        method: 'GET',
                        headers: getAuthHeaders(),
                    });
                    const allUsersData = await allUsersResponse.json();
                    if (allUsersResponse.ok && allUsersData.success) {
                        fetchedUsers = allUsersData.data || [];
                    } else {
                        console.warn('GET /api/users failed or returned an error. Falling back to fetching users by individual roles.');
                        let combinedUsers: any[] = [];
                        for (const role of USER_ROLES) {
                            const roleResponse = await fetch(`${API_BASE_URL}/api/users/role/${role}`, {
                                method: 'GET',
                                headers: getAuthHeaders(),
                            });
                            if (roleResponse.ok) {
                                const roleData = await roleResponse.json();
                                if (roleData.success && roleData.data) {
                                    combinedUsers = [...combinedUsers, ...(roleData.data || [])];
                                }
                            }
                        }
                        fetchedUsers = combinedUsers;
                    }
                } catch (e: any) {
                    console.warn('Network error or backend missing GET /api/users endpoint. Falling back to fetching users by individual roles.', e);
                    let combinedUsers: any[] = [];
                    for (const role of USER_ROLES) {
                        const roleResponse = await fetch(`${API_BASE_URL}/api/users/role/${role}`, {
                            method: 'GET',
                            headers: getAuthHeaders(),
                        });
                        if (roleResponse.ok) {
                            const roleData = await roleResponse.json();
                            if (roleData.success && roleData.data) {
                                combinedUsers = [...combinedUsers, ...(roleData.data || [])];
                            }
                        }
                    }
                    fetchedUsers = combinedUsers;
                }
            } else {
                const roleResponse = await fetch(`${API_BASE_URL}/api/users/role/${userFilterRole}`, {
                    method: 'GET',
                    headers: getAuthHeaders(),
                });
                const roleData = await roleResponse.json();
                if (roleResponse.ok && roleData.success) {
                    fetchedUsers = roleData.data || [];
                } else {
                    setUserError(roleData.message || 'Failed to fetch users by role.');
                }
            }
            setUsers(fetchedUsers);
        } catch (err: any) {
            console.error('Final error fetching users:', err);
            setUserError('An unexpected error occurred while trying to fetch users.');
        } finally {
            setUserLoading(false);
        }
    };

    const handleRegisterUser = async (event: React.FormEvent) => {
        event.preventDefault();

        setUserLoading(true);
        setUserError(null);
        setUserSuccessMessage(null);
        setUsernameSuggestions([]);

        try {
            const response = await fetch(`${API_BASE_URL}/api/users/register`, {
                method: 'POST',
                headers: getAuthHeaders(),
                body: JSON.stringify(newUser),
            });

            const data = await response.json();

            if (response.ok && data.status === 'success') {
                setUserSuccessMessage(data.message || 'User registered successfully!');
                await fetchUsers();
                setNewUser({
                    username: '', password: '', firstName: '',
                    lastName: '', email: '', role: 'PATIENT'
                });
            } else {
                if (data.status === 'conflict' && data.suggestions) {
                    setUsernameSuggestions(data.suggestions);
                    setUserError(data.message || 'Username already exists. Try a different one.');
                } else {
                    setUserError(data.message || 'Failed to register user.');
                }
            }
        } catch (err: any) {
            console.error('Error during user registration:', err);
            setUserError('Network error or an unexpected issue occurred during user registration.');
        } finally {
            setUserLoading(false);
        }
    };

    const toggleUserActiveStatus = async (userId: number, currentStatus: boolean) => {
        setUserLoading(true);
        setUserError(null);

        const endpoint = currentStatus ? 'deactivate' : 'activate';
        try {
            const response = await fetch(`${API_BASE_URL}/api/users/${userId}/${endpoint}`, {
                method: 'PATCH',
                headers: getAuthHeaders(),
            });

            const data = await response.json();

            if (response.ok && data.success) {
                setUserSuccessMessage(data.message);
                await fetchUsers();
            } else {
                setUserError(data.message || `Failed to ${endpoint} user.`);
            }
        } catch (err: any) {
            console.error(`Error during user ${endpoint}:`, err);
            setUserError(`Network error or an unexpected issue occurred during user ${endpoint}.`);
        } finally {
            setUserLoading(false);
        }
    };

    const updateUserRole = async (userId: number, newRole: string) => {
        setUserLoading(true);
        setUserError(null);

        try {
            const response = await fetch(`${API_BASE_URL}/api/users/${userId}/role`, {
                method: 'PUT',
                headers: getAuthHeaders(),
                body: JSON.stringify({ role: newRole }),
            });

            const data = await response.json();

            if (response.ok && data.success) {
                setUserSuccessMessage(data.message);
                await fetchUsers();
            } else {
                setUserError(data.message || 'Failed to update user role.');
            }
        } catch (err: any) {
            console.error('Error updating user role:', err);
            setUserError('Network error or an unexpected issue occurred while updating user role.');
        } finally {
            setUserLoading(false);
        }
    };

    useEffect(() => {
        if (authToken) {
            void fetchUsers();
        }
    }, [authToken, userFilterRole, userSearchQuery]);

    return (
        <div className="space-y-8">
            {/* Register New User Form */}
            <div>
                <h2 className="text-2xl font-bold text-gray-800 mb-4">Register New User</h2>
                <form onSubmit={handleRegisterUser} className="grid grid-cols-1 md:grid-cols-2 gap-4 bg-gray-50 p-6 rounded-lg shadow-inner">
                    <div>
                        <label htmlFor="newUsername" className="block text-gray-700 text-sm font-semibold mb-1">Username:</label>
                        <input type="text" id="newUsername" className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-400 focus:border-blue-400"
                               value={newUser.username} onChange={(e) => setNewUser({ ...newUser, username: e.target.value })} required />
                    </div>
                    <div>
                        <label htmlFor="newPassword" className="block text-gray-700 text-sm font-semibold mb-1">Password:</label>
                        <input type="password" id="newPassword" className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-400 focus:border-blue-400"
                               value={newUser.password} onChange={(e) => setNewUser({ ...newUser, password: e.target.value })} required />
                    </div>
                    <div>
                        <label htmlFor="newFirstName" className="block text-gray-700 text-sm font-semibold mb-1">First Name:</label>
                        <input type="text" id="newFirstName" className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-400 focus:border-blue-400"
                               value={newUser.firstName} onChange={(e) => setNewUser({ ...newUser, firstName: e.target.value })} required />
                    </div>
                    <div>
                        <label htmlFor="newLastName" className="block text-gray-700 text-sm font-semibold mb-1">Last Name:</label>
                        <input type="text" id="newLastName" className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-400 focus:border-blue-400"
                               value={newUser.lastName} onChange={(e) => setNewUser({ ...newUser, lastName: e.target.value })} required />
                    </div>
                    <div className="md:col-span-1">
                        <label htmlFor="newEmail" className="block text-gray-700 text-sm font-semibold mb-1">Email:</label>
                        <input type="email" id="newEmail" className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-400 focus:border-blue-400"
                               value={newUser.email} onChange={(e) => setNewUser({ ...newUser, email: e.target.value })} required />
                    </div>
                    <div>
                        <label htmlFor="newUserRole" className="block text-gray-700 text-sm font-semibold mb-1">Role:</label>
                        <select id="newUserRole" className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-400 focus:border-blue-400"
                                value={newUser.role} onChange={(e) => setNewUser({ ...newUser, role: e.target.value })} required>
                            {USER_ROLES.map(role => (
                                <option key={role} value={role}>{role}</option>
                            ))}
                        </select>
                    </div>
                    <div className="md:col-span-2 text-center">
                        <button
                            type="submit"
                            disabled={userLoading}
                            className={`py-2 px-6 rounded-full font-bold text-white transition-all duration-300 ease-in-out shadow-md flex items-center justify-center space-x-2
                                ${userLoading ? 'bg-gray-400 cursor-not-allowed' : 'bg-blue-600 hover:bg-blue-700 transform hover:scale-105 active:scale-95'}`}
                        >
                            {userLoading ? (
                                <>
                                    <svg className="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                    </svg>
                                    <span>Registering User...</span>
                                </>
                            ) : (
                                <span>Register User</span>
                            )}
                        </button>
                    </div>
                </form>
                <MessageDisplay
                    type="success"
                    message={userSuccessMessage}
                />
                <MessageDisplay
                    type="error"
                    message={userError}
                    extraContent={usernameSuggestions.length > 0 && (
                        <div className="mt-2">
                            <p>Suggestions:</p>
                            <ul className="list-disc list-inside">
                                {usernameSuggestions.map((suggestion, index) => (
                                    <li key={index}>{suggestion}</li>
                                ))}
                            </ul>
                        </div>
                    )}
                />
            </div>

            {/* List All Users */}
            <div>
                <h2 className="text-2xl font-bold text-gray-800 mb-4 flex items-center justify-between">
                    System Users
                    <div className="flex items-center space-x-2">
                        <input
                            type="text"
                            placeholder="Search by username or name..."
                            className="px-3 py-1 border border-gray-300 rounded-md focus:ring-blue-400 focus:border-blue-400 text-sm"
                            value={userSearchQuery}
                            onChange={(e) => setUserSearchQuery(e.target.value)}
                            disabled={userLoading}
                        />
                        <label htmlFor="userRoleFilter" className="text-base text-gray-700">Filter by Role:</label>
                        <select
                            id="userRoleFilter"
                            className="px-3 py-1 border border-gray-300 rounded-md focus:ring-blue-400 focus:border-blue-400"
                            value={userFilterRole}
                            onChange={(e) => setUserFilterRole(e.target.value)}
                            disabled={userLoading || userSearchQuery !== ''} // Disable if search is active
                        >
                            <option value="ALL">All Roles</option>
                            {USER_ROLES.map(role => (
                                <option key={role} value={role}>{role}</option>
                            ))}
                        </select>
                        <button
                            onClick={() => void fetchUsers()}
                            className="bg-gray-200 hover:bg-gray-300 text-gray-800 font-semibold py-1 px-3 rounded-full text-sm transition-colors duration-200"
                            disabled={userLoading}
                        >
                            {userLoading ? 'Refreshing...' : 'Refresh List'}
                        </button>
                    </div>
                </h2>
                {userLoading && users.length === 0 && (
                    <div className="text-center text-gray-500">Loading users...</div>
                )}
                {users.length > 0 ? (
                    <div className="overflow-x-auto rounded-lg shadow-md border border-gray-200">
                        <table className="min-w-full divide-y divide-gray-200">
                            <thead className="bg-gray-100">
                            <tr>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">ID</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Username</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Role</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Enabled</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                            </tr>
                            </thead>
                            <tbody className="bg-white divide-y divide-gray-200">
                            {users.map((user: any) => (
                                <tr key={user.id}>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{user.id}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{user.username}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                                        <select
                                            value={user.role}
                                            onChange={(e) => void updateUserRole(user.id, e.target.value)}
                                            className="px-2 py-1 border border-gray-300 rounded-md focus:ring-blue-400 focus:border-blue-400 text-sm"
                                            disabled={userLoading}
                                        >
                                            {USER_ROLES.map(role => (
                                                <option key={role} value={role}>{role}</option>
                                            ))}
                                        </select>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                                            <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${user.enabled ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                                                {user.enabled ? 'Active' : 'Inactive'}
                                            </span>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                                        <button
                                            onClick={() => void toggleUserActiveStatus(user.id, user.enabled)}
                                            className={`ml-2 px-3 py-1 rounded-md text-white text-xs font-semibold transition-all duration-200
                                                    ${user.enabled ? 'bg-orange-500 hover:bg-orange-600' : 'bg-blue-500 hover:bg-blue-600'}`}
                                            disabled={userLoading}
                                        >
                                            {user.enabled ? 'Deactivate' : 'Activate'}
                                        </button>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                ) : (
                    !userLoading && userError === null && (
                        <div className="text-center text-gray-500 p-4 border border-gray-200 rounded-lg">No users found for the selected role/search query.</div>
                    )
                )}
            </div>
        </div>
    );
};

export default UserManagementView;
