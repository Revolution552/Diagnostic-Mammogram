"use client";

import React, { useState, useCallback } from 'react';
import MessageDisplay from './MessageDisplay'; // Import the reusable message component
import MammogramDetails from './MammogramDetails'; // Import MammogramDetails component

interface MammogramUploadViewProps {
    API_BASE_URL: string;
    authToken: string | null;
    getAuthHeaders: (contentType?: string) => HeadersInit;
}

const MammogramUploadView: React.FC<MammogramUploadViewProps> = ({ API_BASE_URL, authToken, getAuthHeaders }) => {
    const [mammogramPatientId, setMammogramPatientId] = useState<string>('');
    const [imageFile, setImageFile] = useState<File | null>(null);
    const [mammogramNotes, setMammogramNotes] = useState<string>('');
    const [imagePreview, setImagePreview] = useState<string | null>(null);

    const [uploadLoading, setUploadLoading] = useState<boolean>(false);
    const [uploadError, setUploadError] = useState<string | null>(null);
    const [uploadSuccessMessage, setUploadSuccessMessage] = useState<string | null>(null);
    const [uploadedMammogramData, setUploadedMammogramData] = useState<any | null>(null);

    const [mammogramIdToView, setMammogramIdToView] = useState<string>('');
    const [displayMammogramDetails, setDisplayMammogramDetails] = useState<boolean>(false);

    // AI DIAGNOSIS STATES
    const [allAiDiagnoses, setAllAiDiagnoses] = useState<any[]>([]);
    const [aiDiagnosisLoading, setAiDiagnosisLoading] = useState<boolean>(false);
    const [aiDiagnosisError, setAiDiagnosisError] = useState<string | null>(null);
    const [aiDiagnosisIdToView, setAiDiagnosisIdToView] = useState<string>('');
    const [specificAiDiagnosisData, setSpecificAiDiagnosisData] = useState<any | null>(null);

    // NEW STATES FOR AI DIAGNOSIS SEARCH BY NAME/PREDICTION
    const [patientNameToSearch, setPatientNameToSearch] = useState<string>('');
    const [predictionTypeToSearch, setPredictionTypeToSearch] = useState<string>('');
    const [searchedAiDiagnoses, setSearchedAiDiagnoses] = useState<any[]>([]);


    const handleImageFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files ? event.target.files[0] : null;
        setImageFile(file);
        if (file) {
            setImagePreview(window.URL.createObjectURL(file));
        } else {
            setImagePreview(null);
        }
        setUploadError(null);
        setUploadSuccessMessage(null);
        setUploadedMammogramData(null);
    };

    const handleMammogramUpload = async (event: React.FormEvent) => {
        event.preventDefault();

        if (!imageFile) {
            setUploadError('Please select an image file to upload.');
            return;
        }
        if (!mammogramPatientId) {
            setUploadError('Please enter a Patient ID for the mammogram.');
            return;
        }

        const patientIdAsNumber = parseInt(mammogramPatientId, 10);
        if (isNaN(patientIdAsNumber) || patientIdAsNumber <= 0) {
            setUploadError('Please enter a valid positive Patient ID.');
            return;
        }

        setUploadLoading(true);
        setUploadError(null);
        setUploadSuccessMessage(null);
        setUploadedMammogramData(null);

        const formData = new FormData();
        formData.append('patientId', patientIdAsNumber.toString());
        formData.append('imageFile', imageFile);
        if (mammogramNotes) {
            formData.append('notes', mammogramNotes);
        }

        try {
            const headers = new Headers(getAuthHeaders());
            if (headers.has('Content-Type')) {
                headers.delete('Content-Type');
            }

            const response = await fetch(`${API_BASE_URL}/api/mammograms`, {
                method: 'POST',
                headers: headers,
                body: formData,
            });

            if (!response.ok) {
                let errorData;
                try {
                    errorData = await response.json();
                } catch (jsonError) {
                    errorData = { message: response.statusText || 'Unknown error', status: response.status };
                }
                throw new Error(`HTTP error! Status: ${response.status}, Message: ${errorData.message || 'Unknown error'}`);
            }

            const data = await response.json();

            if (data.success) {
                setUploadSuccessMessage(data.message || 'Mammogram uploaded successfully!');
                setUploadedMammogramData(data.data);
                setMammogramPatientId('');
                setImageFile(null);
                setMammogramNotes('');
                setImagePreview(null);
            } else {
                setUploadError(data.message || 'Failed to upload mammogram due to server logic.');
            }
        } catch (err: any) {
            console.error('Error during mammogram upload:', err);
            setUploadError('Network error or an unexpected issue occurred during upload. Please check your API_BASE_URL and network connection.');
        } finally {
            setUploadLoading(false);
        }
    };

    const handleViewMammogramDetails = (event: React.FormEvent) => {
        event.preventDefault();
        if (mammogramIdToView.trim()) {
            setDisplayMammogramDetails(true);
            setUploadError(null);
        } else {
            setUploadError('Please enter a Mammogram ID to view details.');
            setDisplayMammogramDetails(false);
        }
    };

    const handleFetchAllAiDiagnoses = useCallback(async () => {
        setAiDiagnosisLoading(true);
        setAiDiagnosisError(null);
        setAllAiDiagnoses([]);
        setSpecificAiDiagnosisData(null); // Clear specific data when fetching all
        setSearchedAiDiagnoses([]); // Clear search results

        const token = authToken;
        if (!token) {
            setAiDiagnosisError("Authentication token not found. Please log in.");
            setAiDiagnosisLoading(false);
            return;
        }

        try {
            const response = await fetch(`${API_BASE_URL}/api/ai-diagnoses`, {
                method: 'GET',
                headers: new Headers(getAuthHeaders('application/json')),
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(`HTTP error! Status: ${response.status}, Message: ${errorData.message || 'Unknown error'}`);
            }

            const responseJson = await response.json();
            if (responseJson.success) {
                setAllAiDiagnoses(responseJson.data);
                if (responseJson.data.length === 0) {
                    setAiDiagnosisError("No AI diagnosis results found.");
                }
            } else {
                setAiDiagnosisError(responseJson.message || "Failed to retrieve all AI diagnosis results.");
            }
        } catch (error: any) {
            console.error("Error fetching all AI diagnosis results:", error);
            setAiDiagnosisError(error.message || "Network error fetching all AI diagnosis results.");
        } finally {
            setAiDiagnosisLoading(false);
        }
    }, [API_BASE_URL, authToken, getAuthHeaders]);

    const handleFetchSpecificAiDiagnosis = useCallback(async (event: React.FormEvent) => {
        event.preventDefault();
        if (!aiDiagnosisIdToView.trim()) {
            setAiDiagnosisError('Please enter an AI Diagnosis ID to view.');
            setSpecificAiDiagnosisData(null);
            return;
        }

        setAiDiagnosisLoading(true);
        setAiDiagnosisError(null);
        setSpecificAiDiagnosisData(null); // Clear previous specific data
        setAllAiDiagnoses([]); // Clear all data
        setSearchedAiDiagnoses([]); // Clear search results


        const token = authToken;
        if (!token) {
            setAiDiagnosisError("Authentication token not found. Please log in.");
            setAiDiagnosisLoading(false);
            return;
        }

        try {
            const response = await fetch(`${API_BASE_URL}/api/ai-diagnoses/${aiDiagnosisIdToView}`, {
                method: 'GET',
                headers: new Headers(getAuthHeaders('application/json')),
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(`HTTP error! Status: ${response.status}, Message: ${errorData.message || 'Unknown error'}`);
            }

            const responseJson = await response.json();
            if (responseJson.success) {
                setSpecificAiDiagnosisData(responseJson.data);
            } else {
                setAiDiagnosisError(responseJson.message || "Failed to retrieve specific AI diagnosis result.");
            }
        } catch (error: any) {
            console.error("Error fetching specific AI diagnosis result:", error);
            setAiDiagnosisError(error.message || "Network error fetching specific AI diagnosis result.");
        } finally {
            setAiDiagnosisLoading(false);
        }
    }, [API_BASE_URL, authToken, getAuthHeaders, aiDiagnosisIdToView]);


    // NEW: Handle Fetch AI Diagnosis by Patient Name
    const handleFetchAiDiagnosisByPatientName = useCallback(async (event: React.FormEvent) => {
        event.preventDefault();
        if (!patientNameToSearch.trim()) {
            setAiDiagnosisError('Please enter a patient name to search.');
            setSearchedAiDiagnoses([]);
            return;
        }

        setAiDiagnosisLoading(true);
        setAiDiagnosisError(null);
        setSearchedAiDiagnoses([]);
        setAllAiDiagnoses([]); // Clear other data
        setSpecificAiDiagnosisData(null); // Clear other data

        const token = authToken;
        if (!token) {
            setAiDiagnosisError("Authentication token not found. Please log in.");
            setAiDiagnosisLoading(false);
            return;
        }

        try {
            const response = await fetch(`${API_BASE_URL}/api/ai-diagnoses/by-patient-name/${encodeURIComponent(patientNameToSearch)}`, {
                method: 'GET',
                headers: new Headers(getAuthHeaders('application/json')),
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(`HTTP error! Status: ${response.status}, Message: ${errorData.message || 'Unknown error'}`);
            }

            const responseJson = await response.json();
            if (responseJson.success) {
                setSearchedAiDiagnoses(responseJson.data);
                if (responseJson.data.length === 0) {
                    setAiDiagnosisError(`No AI diagnosis results found for patient name: "${patientNameToSearch}".`);
                }
            } else {
                setAiDiagnosisError(responseJson.message || "Failed to retrieve AI diagnosis results by patient name.");
            }
        } catch (error: any) {
            console.error("Error fetching AI diagnosis results by patient name:", error);
            setAiDiagnosisError(error.message || "Network error fetching AI diagnosis results by patient name.");
        } finally {
            setAiDiagnosisLoading(false);
        }
    }, [API_BASE_URL, authToken, getAuthHeaders, patientNameToSearch]);


    // NEW: Handle Fetch AI Diagnosis by Prediction Type
    const handleFetchAiDiagnosisByPrediction = useCallback(async (event: React.FormEvent) => {
        event.preventDefault();
        if (!predictionTypeToSearch.trim()) {
            setAiDiagnosisError('Please enter a prediction type to search (e.g., NORMAL, ABNORMAL).');
            setSearchedAiDiagnoses([]);
            return;
        }

        setAiDiagnosisLoading(true);
        setAiDiagnosisError(null);
        setSearchedAiDiagnoses([]);
        setAllAiDiagnoses([]); // Clear other data
        setSpecificAiDiagnosisData(null); // Clear other data

        const token = authToken;
        if (!token) {
            setAiDiagnosisError("Authentication token not found. Please log in.");
            setAiDiagnosisLoading(false);
            return;
        }

        try {
            const response = await fetch(`${API_BASE_URL}/api/ai-diagnoses/by-prediction/${encodeURIComponent(predictionTypeToSearch)}`, {
                method: 'GET',
                headers: new Headers(getAuthHeaders('application/json')),
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(`HTTP error! Status: ${response.status}, Message: ${errorData.message || 'Unknown error'}`);
            }

            const responseJson = await response.json();
            if (responseJson.success) {
                setSearchedAiDiagnoses(responseJson.data);
                if (responseJson.data.length === 0) {
                    setAiDiagnosisError(`No AI diagnosis results found for prediction type: "${predictionTypeToSearch}".`);
                }
            } else {
                setAiDiagnosisError(responseJson.message || "Failed to retrieve AI diagnosis results by prediction type.");
            }
        } catch (error: any) {
            console.error("Error fetching AI diagnosis results by prediction type:", error);
            setAiDiagnosisError(error.message || "Network error fetching AI diagnosis results by prediction type.");
        } finally {
            setAiDiagnosisLoading(false);
        }
    }, [API_BASE_URL, authToken, getAuthHeaders, predictionTypeToSearch]);


    return (
        <div className="space-y-8 font-sans">
            {/* Mammogram Upload Section */}
            <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200">
                <h2 className="text-2xl font-bold text-gray-800 mb-4 border-b pb-2">Upload New Mammogram</h2>
                <form onSubmit={handleMammogramUpload} className="space-y-4">
                    <div>
                        <label htmlFor="mammogramPatientId" className="block text-gray-700 text-sm font-semibold mb-2">
                            Patient ID:
                        </label>
                        <input
                            type="number"
                            id="mammogramPatientId"
                            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none transition-all duration-200"
                            placeholder="Enter patient ID for mammogram"
                            value={mammogramPatientId}
                            onChange={(e) => setMammogramPatientId(e.target.value)}
                            required
                        />
                    </div>

                    <div className="flex flex-col items-center">
                        <label htmlFor="imageFile" className="cursor-pointer bg-blue-500 hover:bg-blue-600 text-white font-semibold py-3 px-6 rounded-full shadow-lg transition-all duration-300 ease-in-out transform hover:scale-105 active:scale-95 flex items-center space-x-2">
                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12"></path>
                            </svg>
                            <span>{imageFile ? imageFile.name : 'Choose Mammogram Image'}</span>
                        </label>
                        <input
                            id="imageFile"
                            type="file"
                            accept="image/*"
                            onChange={handleImageFileChange}
                            className="hidden"
                            required
                        />
                        {imagePreview && (
                            <div className="mt-6 border-2 border-dashed border-gray-300 rounded-lg p-4 bg-gray-50 max-w-xs">
                                <h3 className="text-lg font-semibold text-gray-700 mb-2 text-center">Image Preview:</h3>
                                <img src={imagePreview} alt="Mammogram Preview" className="max-w-full h-auto rounded-md shadow-md" />
                            </div>
                        )}
                    </div>

                    <div>
                        <label htmlFor="mammogramNotes" className="block text-gray-700 text-sm font-semibold mb-2">
                            Notes (Optional):
                        </label>
                        <textarea
                            id="mammogramNotes"
                            rows="3"
                            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none transition-all duration-200"
                            placeholder="Add any relevant notes for the mammogram"
                            value={mammogramNotes}
                            onChange={(e) => setMammogramNotes(e.target.value)}
                        ></textarea>
                    </div>

                    <button
                        type="submit"
                        disabled={uploadLoading || !imageFile || !mammogramPatientId}
                        className={`w-full py-3 px-6 rounded-full font-bold text-white transition-all duration-300 ease-in-out shadow-lg flex items-center justify-center space-x-2
                            ${uploadLoading || !imageFile || !mammogramPatientId ? 'bg-gray-400 cursor-not-allowed' : 'bg-indigo-600 hover:bg-indigo-700 transform hover:scale-105 active:scale-95'}`}
                    >
                        {uploadLoading ? (
                            <>
                                <svg className="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                </svg>
                                <span>Uploading...</span>
                            </>
                        ) : (
                            <>
                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 0115.9 6L16 6a3 3 0 013 3v10a2 2 0 01-2 2H7a2 2 0 01-2-2v-1"></path>
                                </svg>
                                <span>Upload Mammogram</span>
                            </>
                        )}
                    </button>
                </form>

                {uploadError && !uploadSuccessMessage && <MessageDisplay type="error" message={uploadError} />}
                {uploadSuccessMessage && <MessageDisplay type="success" message={uploadSuccessMessage} extraContent={uploadedMammogramData && (
                    <div className="mt-4">
                        <h3 className="text-lg font-semibold text-green-800">Uploaded Data:</h3>
                        <pre className="whitespace-pre-wrap font-mono text-sm bg-green-50 p-3 rounded-md overflow-x-auto border border-green-200">
                            {JSON.stringify(uploadedMammogramData, null, 2)}
                        </pre>
                    </div>
                )} />}
            </div>

            {/* View Mammogram Details Section */}
            <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200">
                <h2 className="text-2xl font-bold text-gray-800 mb-4 border-b pb-2">View Mammogram Details</h2>
                <form onSubmit={handleViewMammogramDetails} className="space-y-4">
                    <div>
                        <label htmlFor="mammogramIdToView" className="block text-gray-700 text-sm font-semibold mb-1">Mammogram ID:</label>
                        <input
                            type="text"
                            id="mammogramIdToView"
                            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-400 focus:border-blue-400"
                            value={mammogramIdToView}
                            onChange={(e) => {
                                setMammogramIdToView(e.target.value);
                                setDisplayMammogramDetails(false);
                            }}
                            placeholder="Enter Mammogram ID to view"
                            required
                        />
                    </div>
                    <button
                        type="submit"
                        className="py-2 px-6 rounded-full font-bold text-white bg-blue-600 hover:bg-blue-700 transition-all duration-300 ease-in-out shadow-lg transform hover:scale-105 active:scale-95"
                    >
                        View Details
                    </button>
                </form>

                {displayMammogramDetails && mammogramIdToView && (
                    <div className="mt-8">
                        <MammogramDetails
                            mammogramId={mammogramIdToView}
                            API_BASE_URL={API_BASE_URL}
                            authToken={authToken}
                            getAuthHeaders={getAuthHeaders}
                        />
                    </div>
                )}
            </div>

            {/* AI Diagnosis Results Section */}
            <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200">
                <h2 className="text-2xl font-bold text-gray-800 mb-4 border-b pb-2">AI Diagnosis Results</h2>

                {/* Fetch All AI Diagnoses */}
                <div className="mb-6">
                    <button
                        onClick={handleFetchAllAiDiagnoses}
                        disabled={aiDiagnosisLoading}
                        className={`w-full py-3 px-6 rounded-full font-bold text-white transition-all duration-300 ease-in-out shadow-lg flex items-center justify-center space-x-2
                            ${aiDiagnosisLoading ? 'bg-gray-400 cursor-not-allowed' : 'bg-green-600 hover:bg-green-700 transform hover:scale-105 active:scale-95'}`}
                    >
                        {aiDiagnosisLoading ? (
                            <>
                                <svg className="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                </svg>
                                <span>Loading All AI Diagnoses...</span>
                            </>
                        ) : (
                            <span>Fetch All AI Diagnoses</span>
                        )}
                    </button>
                </div>

                {/* Fetch Specific AI Diagnosis by ID */}
                <div className="mt-6">
                    <h3 className="text-xl font-semibold text-gray-800 mb-3">Fetch Specific AI Diagnosis</h3>
                    <form onSubmit={handleFetchSpecificAiDiagnosis} className="space-y-4">
                        <div>
                            <label htmlFor="aiDiagnosisIdToView" className="block text-gray-700 text-sm font-semibold mb-1">AI Diagnosis ID:</label>
                            <input
                                type="text"
                                id="aiDiagnosisIdToView"
                                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-400 focus:border-blue-400"
                                value={aiDiagnosisIdToView}
                                onChange={(e) => {
                                    setAiDiagnosisIdToView(e.target.value);
                                    setSpecificAiDiagnosisData(null);
                                }}
                                placeholder="Enter AI Diagnosis ID"
                                required
                            />
                        </div>
                        <button
                            type="submit"
                            disabled={aiDiagnosisLoading || !aiDiagnosisIdToView.trim()}
                            className={`py-2 px-6 rounded-full font-bold text-white transition-all duration-300 ease-in-out shadow-lg transform hover:scale-105 active:scale-95
                                ${aiDiagnosisLoading || !aiDiagnosisIdToView.trim() ? 'bg-gray-400 cursor-not-allowed' : 'bg-purple-600 hover:bg-purple-700'}`}
                        >
                            {aiDiagnosisLoading ? 'Fetching...' : 'Fetch AI Diagnosis'}
                        </button>
                    </form>
                </div>

                {/* NEW: Fetch AI Diagnosis by Patient Name */}
                <div className="mt-6">
                    <h3 className="text-xl font-semibold text-gray-800 mb-3">Search AI Diagnosis by Patient Name</h3>
                    <form onSubmit={handleFetchAiDiagnosisByPatientName} className="space-y-4">
                        <div>
                            <label htmlFor="patientNameToSearch" className="block text-gray-700 text-sm font-semibold mb-1">Patient Name (partial):</label>
                            <input
                                type="text"
                                id="patientNameToSearch"
                                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-400 focus:border-blue-400"
                                value={patientNameToSearch}
                                onChange={(e) => {
                                    setPatientNameToSearch(e.target.value);
                                    setSearchedAiDiagnoses([]); // Clear results on input change
                                }}
                                placeholder="Enter patient name (e.g., John Doe)"
                                required
                            />
                        </div>
                        <button
                            type="submit"
                            disabled={aiDiagnosisLoading || !patientNameToSearch.trim()}
                            className={`py-2 px-6 rounded-full font-bold text-white transition-all duration-300 ease-in-out shadow-lg transform hover:scale-105 active:scale-95
                                ${aiDiagnosisLoading || !patientNameToSearch.trim() ? 'bg-gray-400 cursor-not-allowed' : 'bg-orange-600 hover:bg-orange-700'}`}
                        >
                            {aiDiagnosisLoading ? 'Searching...' : 'Search by Patient Name'}
                        </button>
                    </form>
                </div>

                {/* NEW: Fetch AI Diagnosis by Prediction Type */}
                <div className="mt-6">
                    <h3 className="text-xl font-semibold text-gray-800 mb-3">Search AI Diagnosis by Prediction</h3>
                    <form onSubmit={handleFetchAiDiagnosisByPrediction} className="space-y-4">
                        <div>
                            <label htmlFor="predictionTypeToSearch" className="block text-gray-700 text-sm font-semibold mb-1">Prediction Type:</label>
                            <select
                                id="predictionTypeToSearch"
                                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-400 focus:border-blue-400 bg-white"
                                value={predictionTypeToSearch}
                                onChange={(e) => {
                                    setPredictionTypeToSearch(e.target.value);
                                    setSearchedAiDiagnoses([]); // Clear results on input change
                                }}
                                required
                            >
                                <option value="">Select Prediction</option>
                                <option value="NORMAL">NORMAL</option>
                                <option value="ABNORMAL">ABNORMAL</option>
                            </select>
                        </div>
                        <button
                            type="submit"
                            disabled={aiDiagnosisLoading || !predictionTypeToSearch.trim()}
                            className={`py-2 px-6 rounded-full font-bold text-white transition-all duration-300 ease-in-out shadow-lg transform hover:scale-105 active:scale-95
                                ${aiDiagnosisLoading || !predictionTypeToSearch.trim() ? 'bg-gray-400 cursor-not-allowed' : 'bg-teal-600 hover:bg-teal-700'}`}
                        >
                            {aiDiagnosisLoading ? 'Searching...' : 'Search by Prediction'}
                        </button>
                    </form>
                </div>

                {aiDiagnosisError && <MessageDisplay type="error" message={aiDiagnosisError} />}

                {/* Display Area for All AI Diagnoses */}
                {allAiDiagnoses.length > 0 && (
                    <div className="mt-4 max-h-60 overflow-y-auto bg-gray-50 p-3 rounded-md border border-gray-200">
                        <h3 className="text-lg font-semibold text-gray-800 mb-2">All AI Diagnosis Results:</h3>
                        <ul className="list-disc pl-5 space-y-2">
                            {allAiDiagnoses.map((diagnosis: any) => (
                                <li key={diagnosis.id} className="text-sm text-gray-700 break-words">
                                    <strong>ID:</strong> {diagnosis.id}, <strong>Patient ID:</strong> {diagnosis.patientId},
                                    <strong>Prediction:</strong> {diagnosis.prediction}
                                    {diagnosis.diagnosisSummary && `, ${diagnosis.diagnosisSummary}`}
                                    {diagnosis.detailedFindings && `, Findings: ${diagnosis.detailedFindings}`}
                                    {diagnosis.recommendation && `, Rec: ${diagnosis.recommendation}`}
                                </li>
                            ))}
                        </ul>
                    </div>
                )}

                {/* Display Area for Specific AI Diagnosis */}
                {specificAiDiagnosisData && (
                    <div className="mt-4 bg-gray-50 p-3 rounded-md border border-gray-200">
                        <h3 className="text-lg font-semibold text-gray-800 mb-2">Specific AI Diagnosis Data:</h3>
                        <pre className="whitespace-pre-wrap font-mono text-sm overflow-x-auto">
                            {JSON.stringify(specificAiDiagnosisData, null, 2)}
                        </pre>
                        <div className="mt-2 text-gray-700">
                            <p><strong>ID:</strong> {specificAiDiagnosisData.id}</p> {/* Added ID */}
                            <p><strong>Prediction:</strong> {specificAiDiagnosisData.prediction}</p>
                            <p><strong>Confidence:</strong> {(specificAiDiagnosisData.confidenceScore * 100).toFixed(2)}%</p>
                            <p><strong>Summary:</strong> {specificAiDiagnosisData.diagnosisSummary}</p>
                            {specificAiDiagnosisData.detailedFindings && <p><strong>Detailed Findings:</strong> {specificAiDiagnosisData.detailedFindings}</p>}
                            {specificAiDiagnosisData.recommendation && <p><strong>Recommendation:</strong> {specificAiDiagnosisData.recommendation}</p>}
                            {specificAiDiagnosisData.probabilities && <p><strong>Probabilities:</strong> {specificAiDiagnosisData.probabilities.map((p: number) => p.toFixed(4)).join(', ')}</p>}
                            {specificAiDiagnosisData.patientId && <p><strong>Patient ID:</strong> {specificAiDiagnosisData.patientId}</p>}
                        </div>
                    </div>
                )}

                {/* Display Area for Searched AI Diagnoses (by Name or Prediction) */}
                {searchedAiDiagnoses.length > 0 && (
                    <div className="mt-4 max-h-60 overflow-y-auto bg-gray-50 p-3 rounded-md border border-gray-200">
                        <h3 className="text-lg font-semibold text-gray-800 mb-2">Searched AI Diagnosis Results:</h3>
                        <ul className="list-disc pl-5 space-y-2">
                            {searchedAiDiagnoses.map((diagnosis: any) => (
                                <li key={diagnosis.id} className="text-sm text-gray-700 break-words">
                                    <strong>ID:</strong> {diagnosis.id}, <strong>Patient ID:</strong> {diagnosis.patientId},
                                    <strong>Prediction:</strong> {diagnosis.prediction}
                                    {diagnosis.diagnosisSummary && `, ${diagnosis.diagnosisSummary}`}
                                    {diagnosis.detailedFindings && `, Findings: ${diagnosis.detailedFindings}`}
                                    {diagnosis.recommendation && `, Rec: ${diagnosis.recommendation}`}
                                </li>
                            ))}
                        </ul>
                    </div>
                )}
            </div>
        </div>
    );
};

export default MammogramUploadView;
