"use client";

import React, { useState, useEffect } from 'react';
import MessageDisplay from './MessageDisplay'; // Import the reusable message component

interface PatientManagementViewProps {
    API_BASE_URL: string;
    authToken: string | null;
    getAuthHeaders: (contentType?: string) => HeadersInit;
}

const PatientManagementView: React.FC<PatientManagementViewProps> = ({ API_BASE_URL, authToken, getAuthHeaders }) => {
    // Updated patient state to match PatientRequest DTO
    const [newPatient, setNewPatient] = useState({
        fullName: '',
        gender: '',
        dateOfBirth: '', // Format: YYYY-MM-DD
        placeOfBirth: '',
        age: '', // Changed to string for input, will be converted to number for API
        contactInfo: '', // Tanzanian phone number format
        education: '',
        medicalHistory: ''
    });
    const [patients, setPatients] = useState<any[]>([]);
    const [patientPageInfo, setPatientPageInfo] = useState({ currentPage: 0, totalPages: 0, totalItems: 0 });

    const [patientLoading, setPatientLoading] = useState<boolean>(false);
    const [patientError, setPatientError] = useState<string | null>(null);
    const [patientSuccessMessage, setPatientSuccessMessage] = useState<string | null>(null);

    const fetchPatients = async (page = 0, size = 10) => {
        setPatientLoading(true);
        setPatientError(null);

        try {
            const response = await fetch(`${API_BASE_URL}/api/patients/get?page=${page}&size=${size}`, {
                method: 'GET',
                headers: getAuthHeaders(),
            });

            const data = await response.json();

            if (response.ok && data.status === 'success') {
                setPatients(data.data?.content || []); // Safely access content and provide fallback
                setPatientPageInfo({
                    currentPage: data.pageInfo?.currentPage || 0,
                    totalPages: data.pageInfo?.totalPages || 0,
                    totalItems: data.pageInfo?.totalItems || 0
                });
            } else {
                setPatientError(data.message || 'Failed to fetch patients.');
            }
        } catch (err: any) {
            console.error('Error fetching patients:', err);
            setPatientError('Network error or an unexpected issue occurred while fetching patients.');
        } finally {
            setPatientLoading(false);
        }
    };

    const handleCreatePatient = async (event: React.FormEvent) => {
        event.preventDefault();

        setPatientLoading(true);
        setPatientError(null);
        setPatientSuccessMessage(null);

        // Client-side validation for age (basic check before sending to backend)
        const ageNum = parseInt(newPatient.age);
        if (isNaN(ageNum) || ageNum < 18 || ageNum > 120) {
            setPatientError("Age must be between 18 and 120.");
            setPatientLoading(false);
            return;
        }

        // Prepare data to match backend DTO, converting age to number
        const patientData = {
            ...newPatient,
            age: ageNum,
        };

        try {
            const response = await fetch(`${API_BASE_URL}/api/patients/register`, {
                method: 'POST',
                headers: getAuthHeaders(),
                body: JSON.stringify(patientData),
            });

            const data = await response.json();

            if (response.ok && data.status === 'success') {
                setPatientSuccessMessage(data.message || 'Patient registered successfully!');
                await fetchPatients();
                // Reset form fields
                setNewPatient({
                    fullName: '', gender: '', dateOfBirth: '',
                    placeOfBirth: '', age: '', contactInfo: '',
                    education: '', medicalHistory: ''
                });
            } else {
                setPatientError(data.message || 'Failed to register patient. Please check inputs.');
            }
        } catch (err: any) {
            console.error('Error during patient registration:', err);
            setPatientError('Network error or an unexpected issue occurred during patient registration.');
        } finally {
            setPatientLoading(false);
        }
    };

    useEffect(() => {
        if (authToken) {
            void fetchPatients(); // Call fetchPatients when component mounts or authToken changes
        }
    }, [authToken]); // Dependency on authToken to refetch when logged in

    return (
        <div className="space-y-8">
            {/* Register New Patient Form */}
            <div>
                <h2 className="text-2xl font-bold text-gray-800 mb-4">Register New Patient</h2>
                <form onSubmit={handleCreatePatient} className="grid grid-cols-1 md:grid-cols-2 gap-4 bg-gray-50 p-6 rounded-lg shadow-inner">
                    <div className="md:col-span-2"> {/* Full name takes full width */}
                        <label htmlFor="fullName" className="block text-gray-700 text-sm font-semibold mb-1">Full Name:</label>
                        <input type="text" id="fullName" className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-400 focus:border-blue-400"
                               value={newPatient.fullName} onChange={(e) => setNewPatient({ ...newPatient, fullName: e.target.value })} required
                               placeholder="e.g., John Doe" />
                    </div>
                    <div>
                        <label htmlFor="dateOfBirth" className="block text-gray-700 text-sm font-semibold mb-1">Date of Birth:</label>
                        <input type="date" id="dateOfBirth" className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-400 focus:border-blue-400"
                               value={newPatient.dateOfBirth} onChange={(e) => setNewPatient({ ...newPatient, dateOfBirth: e.target.value })} required />
                    </div>
                    <div>
                        <label htmlFor="age" className="block text-gray-700 text-sm font-semibold mb-1">Age:</label>
                        <input type="number" id="age" className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-400 focus:border-blue-400"
                               value={newPatient.age} onChange={(e) => setNewPatient({ ...newPatient, age: e.target.value })} required
                               min="18" max="120" placeholder="Must be 18-120" />
                    </div>
                    <div>
                        <label htmlFor="gender" className="block text-gray-700 text-sm font-semibold mb-1">Gender:</label>
                        <select id="gender" className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-400 focus:border-blue-400"
                                value={newPatient.gender} onChange={(e) => setNewPatient({ ...newPatient, gender: e.target.value })} required>
                            <option value="">Select Gender</option>
                            <option value="Male">Male</option>
                            <option value="Female">Female</option>
                            <option value="Other">Other</option>
                        </select>
                    </div>
                    <div>
                        <label htmlFor="placeOfBirth" className="block text-gray-700 text-sm font-semibold mb-1">Place of Birth:</label>
                        <input type="text" id="placeOfBirth" className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-400 focus:border-blue-400"
                               value={newPatient.placeOfBirth} onChange={(e) => setNewPatient({ ...newPatient, placeOfBirth: e.target.value })}
                               placeholder="e.g., Dar es Salaam" />
                    </div>
                    <div>
                        <label htmlFor="contactInfo" className="block text-gray-700 text-sm font-semibold mb-1">Contact Info (Phone):</label>
                        <input type="tel" id="contactInfo" className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-400 focus:border-blue-400"
                               placeholder="e.g., +255712345678 or 0712345678"
                               value={newPatient.contactInfo} onChange={(e) => setNewPatient({ ...newPatient, contactInfo: e.target.value })} required />
                    </div>
                    <div>
                        <label htmlFor="education" className="block text-gray-700 text-sm font-semibold mb-1">Education:</label>
                        <input type="text" id="education" className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-400 focus:border-blue-400"
                               value={newPatient.education} onChange={(e) => setNewPatient({ ...newPatient, education: e.target.value })}
                               placeholder="e.g., University" />
                    </div>
                    <div className="md:col-span-2">
                        <label htmlFor="medicalHistory" className="block text-gray-700 text-sm font-semibold mb-1">Medical History:</label>
                        <textarea id="medicalHistory" rows={3} className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-400 focus:border-blue-400"
                                  value={newPatient.medicalHistory} onChange={(e) => setNewPatient({ ...newPatient, medicalHistory: e.target.value })}
                                  placeholder="Any relevant medical conditions or past treatments" />
                    </div>
                    <div className="md:col-span-2 text-center">
                        <button
                            type="submit"
                            disabled={patientLoading}
                            className={`py-2 px-6 rounded-full font-bold text-white transition-all duration-300 ease-in-out shadow-md flex items-center justify-center space-x-2
                                ${patientLoading ? 'bg-gray-400 cursor-not-allowed' : 'bg-green-600 hover:bg-green-700 transform hover:scale-105 active:scale-95'}`}
                        >
                            {patientLoading ? (
                                <>
                                    <svg className="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                    </svg>
                                    <span>Registering...</span>
                                </>
                            ) : (
                                <span>Register Patient</span>
                            )}
                        </button>
                    </div>
                </form>
                <MessageDisplay type="success" message={patientSuccessMessage} />
                <MessageDisplay type="error" message={patientError} />
            </div>

            {/* List All Patients */}
            <div>
                <h2 className="text-2xl font-bold text-gray-800 mb-4 flex items-center justify-between">
                    All Patients
                    <button
                        onClick={() => void fetchPatients()}
                        className="bg-gray-200 hover:bg-gray-300 text-gray-800 font-semibold py-1 px-3 rounded-full text-sm transition-colors duration-200"
                        disabled={patientLoading}
                    >
                        {patientLoading ? 'Refreshing...' : 'Refresh List'}
                    </button>
                </h2>
                {patientLoading && patients.length === 0 && (
                    <div className="text-center text-gray-500">Loading patients...</div>
                )}
                {patients.length > 0 ? (
                    <div className="overflow-x-auto rounded-lg shadow-md border border-gray-200">
                        <table className="min-w-full divide-y divide-gray-200">
                            <thead className="bg-gray-100">
                            <tr>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">ID</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Full Name</th> {/* Updated header */}
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">DOB</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Age</th> {/* New header */}
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Gender</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Contact</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Place of Birth</th> {/* New header */}
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Education</th> {/* New header */}
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Medical History</th> {/* New header */}
                            </tr>
                            </thead>
                            <tbody className="bg-white divide-y divide-gray-200">
                            {patients.map((patient: any) => (
                                <tr key={patient.id}>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{patient.id}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{patient.fullName}</td> {/* Updated to fullName */}
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{patient.dateOfBirth}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{patient.age}</td> {/* Display age */}
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{patient.gender}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{patient.contactInfo}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{patient.placeOfBirth}</td> {/* Display placeOfBirth */}
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{patient.education}</td> {/* Display education */}
                                    <td className="px-6 py-4 text-sm text-gray-900 max-w-xs overflow-hidden text-ellipsis">{patient.medicalHistory}</td> {/* Display medicalHistory */}
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                ) : (
                    !patientLoading && patientError === null && (
                        <div className="text-center text-gray-500 p-4 border border-gray-200 rounded-lg">No patients found.</div>
                    )
                )}
                {/* Pagination Controls */}
                {patientPageInfo.totalPages > 1 && (
                    <div className="flex justify-center items-center space-x-2 mt-4">
                        <button
                            onClick={() => void fetchPatients(patientPageInfo.currentPage - 1)}
                            disabled={patientPageInfo.currentPage === 0 || patientLoading}
                            className="px-3 py-1 bg-gray-200 rounded-md disabled:opacity-50"
                        >
                            Previous
                        </button>
                        <span className="text-sm text-gray-700">
                            Page {patientPageInfo.currentPage + 1} of {patientPageInfo.totalPages}
                        </span>
                        <button
                            onClick={() => void fetchPatients(patientPageInfo.currentPage + 1)}
                            disabled={patientPageInfo.currentPage === patientPageInfo.totalPages - 1 || patientLoading}
                            className="px-3 py-1 bg-gray-200 rounded-md disabled:opacity-50"
                        >
                            Next
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
};

export default PatientManagementView;
