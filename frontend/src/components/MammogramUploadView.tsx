"use client";

import React, { useState } from 'react';
import MessageDisplay from './MessageDisplay'; // Import the reusable message component

interface MammogramUploadViewProps {
    API_BASE_URL: string;
    authToken: string | null;
}

const MammogramUploadView: React.FC<MammogramUploadViewProps> = ({ API_BASE_URL, authToken }) => {
    const [mammogramPatientId, setMammogramPatientId] = useState<string>(''); // Keep as string for input field
    const [imageFile, setImageFile] = useState<File | null>(null);
    const [mammogramNotes, setMammogramNotes] = useState<string>('');
    const [imagePreview, setImagePreview] = useState<string | null>(null);

    const [uploadLoading, setUploadLoading] = useState<boolean>(false);
    const [uploadError, setUploadError] = useState<string | null>(null);
    const [uploadSuccessMessage, setUploadSuccessMessage] = useState<string | null>(null);
    const [uploadedMammogramData, setUploadedMammogramData] = useState<any | null>(null);

    const handleImageFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files ? event.target.files[0] : null;
        setImageFile(file);
        if (file) {
            setImagePreview(window.URL.createObjectURL(file));
        } else {
            setImagePreview(null);
        }
        setUploadError(null); // Clear errors on new file selection
        setUploadSuccessMessage(null); // Clear success on new file selection
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

        // Validate and convert patient ID to a number
        const patientIdAsNumber = parseInt(mammogramPatientId, 10);
        if (isNaN(patientIdAsNumber) || patientIdAsNumber <= 0) {
            setUploadError('Please enter a valid positive Patient ID.');
            return;
        }

        setUploadLoading(true);
        setUploadError(null); // Clear previous errors
        setUploadSuccessMessage(null); // Clear previous success messages
        setUploadedMammogramData(null);

        const formData = new FormData();
        formData.append('patientId', patientIdAsNumber.toString());
        formData.append('imageFile', imageFile);
        if (mammogramNotes) {
            formData.append('notes', mammogramNotes);
        }

        try {
            const headers = new Headers();
            if (authToken) {
                headers.append('Authorization', `Bearer ${authToken}`);
            }

            const response = await fetch(`${API_BASE_URL}/api/mammograms`, {
                method: 'POST',
                headers: headers,
                body: formData,
            });

            const data = await response.json();

            // Check for 'success: true' in the response body
            // This aligns with the backend response structure shown in the screenshot.
            if (response.ok && data.success) {
                setUploadSuccessMessage(data.message || 'Mammogram uploaded successfully!');
                setUploadedMammogramData(data.data);
                // Clear form fields only on success
                setMammogramPatientId('');
                setImageFile(null);
                setMammogramNotes('');
                setImagePreview(null);
            } else {
                // If response.ok is false, or data.success is false
                setUploadError(data.message || `Failed to upload mammogram. Server responded with status: ${response.status} ${response.statusText}.`);
            }
        } catch (err: any) {
            console.error('Error during mammogram upload:', err);
            setUploadError('Network error or an unexpected issue occurred during upload. Please check your API_BASE_URL and network connection.');
        } finally {
            setUploadLoading(false);
        }
    };

    return (
        <>
            <form onSubmit={handleMammogramUpload} className="space-y-6">
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

            {/* Display error message only if uploadError is not null AND uploadSuccessMessage is null */}
            {uploadError && !uploadSuccessMessage && <MessageDisplay type="error" message={uploadError} />}
            {/* Display success message only if uploadSuccessMessage is not null */}
            {uploadSuccessMessage && <MessageDisplay type="success" message={uploadSuccessMessage} extraContent={uploadedMammogramData && (
                <div className="mt-4">
                    <h3 className="text-lg font-semibold text-green-800">Uploaded Data:</h3>
                    <pre className="whitespace-pre-wrap font-mono text-sm bg-green-50 p-3 rounded-md overflow-x-auto border border-green-200">
                        {JSON.stringify(uploadedMammogramData, null, 2)}
                    </pre>
                </div>
            )} />}
        </>
    );
};

export default MammogramUploadView;
