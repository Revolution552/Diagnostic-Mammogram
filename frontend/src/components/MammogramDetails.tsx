"use client";

import React, { useState, useEffect } from 'react';
import MessageDisplay from './MessageDisplay'; // Assuming MessageDisplay is in the same directory

interface MammogramDetailsProps {
    mammogramId: string; // The ID of the mammogram to display
    API_BASE_URL: string;
    authToken: string | null;
    getAuthHeaders: (contentType?: string) => HeadersInit;
}

interface AiDiagnosis {
    diagnosisSummary: string;
    prediction: string;
    confidenceScore: number;
    probabilities: number[]; // Assuming this is an array of numbers
}

interface MammogramDto {
    id: number;
    patientId: number;
    patientName: string; // Assuming this field exists in your MammogramDto
    dateUploaded: string; // Date string, e.g., ISO 8601
    imagePath: string; // URL to the image
    notes: string;
    aiDiagnosis: AiDiagnosis | null; // AI diagnosis object, can be null
}

const MammogramDetails: React.FC<MammogramDetailsProps> = ({ mammogramId, API_BASE_URL, authToken, getAuthHeaders }) => {
    const [mammogram, setMammogram] = useState<MammogramDto | null>(null);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchDetails = async () => {
            setLoading(true);
            setError(null);
            setMammogram(null); // Clear previous mammogram details

            if (!mammogramId) {
                setError("No Mammogram ID provided.");
                setLoading(false);
                return;
            }
            if (!authToken) {
                setError("Authentication token is missing. Please log in.");
                setLoading(false);
                return;
            }

            try {
                // Assuming your backend has an endpoint like /api/mammograms/{id}
                const response = await fetch(`${API_BASE_URL}/api/mammograms/${mammogramId}`, {
                    method: 'GET',
                    headers: getAuthHeaders(),
                });

                if (response.ok) {
                    const data: MammogramDto = await response.json();
                    setMammogram(data);
                } else {
                    const errorData = await response.json().catch(() => ({ message: `Server error: ${response.status} ${response.statusText}` }));
                    setError(errorData.message || "Failed to load mammogram data.");
                }
            } catch (err: any) {
                console.error("Error fetching mammogram details:", err);
                setError("Network error or an unexpected issue occurred while fetching mammogram details.");
            } finally {
                setLoading(false);
            }
        };

        void fetchDetails(); // Call the fetch function
    }, [mammogramId, API_BASE_URL, authToken, getAuthHeaders]); // Dependencies for useEffect

    if (loading) return <div className="text-center text-gray-600 p-4">Loading mammogram details...</div>;
    if (error) return <MessageDisplay type="error" message={error} />;
    if (!mammogram) return <div className="text-center text-gray-600 p-4">No mammogram found with ID: {mammogramId}.</div>;

    return (
        <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200 space-y-4">
            <h2 className="text-2xl font-bold text-gray-800 border-b pb-2 mb-4">Mammogram Details (ID: {mammogram.id})</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                    <p className="text-gray-700"><strong>Patient:</strong> {mammogram.patientName} (ID: {mammogram.patientId})</p>
                    <p className="text-gray-700"><strong>Uploaded On:</strong> {new Date(mammogram.dateUploaded).toLocaleDateString()}</p>
                    <p className="text-gray-700"><strong>Notes:</strong> {mammogram.notes || 'N/A'}</p>
                </div>
                {mammogram.imagePath && (
                    <div className="image-container flex justify-center items-center p-2 border rounded-md bg-gray-50">
                        <img src={mammogram.imagePath} alt={`Mammogram ${mammogram.id}`} className="max-w-full h-auto rounded-md shadow-sm" />
                    </div>
                )}
            </div>

            {mammogram.aiDiagnosis ? (
                <div className="ai-diagnosis-section bg-blue-50 p-4 rounded-lg border border-blue-200">
                    <h3 className="text-xl font-semibold text-blue-800 mb-2">AI Diagnosis:</h3>
                    <p className="text-blue-700"><strong>Summary:</strong> {mammogram.aiDiagnosis.diagnosisSummary}</p>
                    <p className="text-blue-700"><strong>Predicted Class:</strong> <span className="font-bold">{mammogram.aiDiagnosis.prediction}</span></p>
                    <p className="text-blue-700"><strong>Confidence:</strong> <span className="font-bold text-lg">{(mammogram.aiDiagnosis.confidenceScore * 100).toFixed(2)}%</span></p>
                    {/* You can display raw probabilities if needed */}
                    {/* <p className="text-blue-700">Probabilities: {mammogram.aiDiagnosis.probabilities.map(p => p.toFixed(4)).join(', ')}</p> */}
                </div>
            ) : (
                <p className="text-gray-600 bg-yellow-50 p-3 rounded-lg border border-yellow-200">AI diagnosis not available for this mammogram.</p>
            )}
        </div>
    );
};

export default MammogramDetails;
