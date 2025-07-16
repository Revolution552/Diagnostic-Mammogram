"use client";

import React, { useState, useEffect } from 'react';
import MessageDisplay from './MessageDisplay'; // Assuming MessageDisplay is in the same directory

interface ReportManagementViewProps {
    API_BASE_URL: string;
    authToken: string | null;
    getAuthHeaders: (contentType?: string) => HeadersInit; // Added this prop
}

const ReportManagementView: React.FC<ReportManagementViewProps> = ({ API_BASE_URL, authToken, getAuthHeaders }) => {
    const [newReport, setNewReport] = useState({
        mammogramId: '',
        patientId: '',
        findings: '',
        conclusion: '',
        recommendation: '',
        reportDate: new Date().toISOString().split('T')[0],
        createdByUserId: ''
    });
    const [reports, setReports] = useState<any[]>([]);

    const [reportLoading, setReportLoading] = useState<boolean>(false);
    const [reportError, setReportError] = useState<string | null>(null);
    const [reportSuccessMessage, setReportSuccessMessage] = useState<string | null>(null);

    const fetchReports = async () => {
        setReportLoading(true);
        setReportError(null);
        setReportSuccessMessage(null);

        try {
            const response = await fetch(`${API_BASE_URL}/api/reports`, {
                method: 'GET',
                headers: getAuthHeaders(),
            });

            const data = await response.json();

            if (response.ok && data.success) {
                setReports(data.data || []);
            } else {
                setReportError(data.message || 'Failed to fetch reports.');
            }
        } catch (err: any) {
            console.error('Error fetching reports:', err);
            setReportError('Network error or an unexpected issue occurred while fetching reports.');
        } finally {
            setReportLoading(false);
        }
    };

    const handleCreateReport = async (event: React.FormEvent) => {
        event.preventDefault();

        if (!newReport.findings.trim()) {
            setReportError('Findings cannot be empty.');
            return;
        }
        if (!newReport.conclusion.trim()) {
            setReportError('Conclusion cannot be empty.');
            return;
        }
        if (!newReport.recommendation.trim()) {
            setReportError('Recommendations cannot be empty.');
            return;
        }

        setReportLoading(true);
        setReportError(null);
        setReportSuccessMessage(null);

        const reportData = {
            ...newReport,
            mammogramId: Number(newReport.mammogramId),
            patientId: Number(newReport.patientId),
            createdByUserId: newReport.createdByUserId ? Number(newReport.createdByUserId) : undefined,
            findings: newReport.findings.trim(),
            conclusion: newReport.conclusion.trim(),
            recommendations: newReport.recommendation.trim(),
        };

        try {
            const response = await fetch(`${API_BASE_URL}/api/reports`, {
                method: 'POST',
                headers: getAuthHeaders(),
                body: JSON.stringify(reportData),
            });

            const data = await response.json();

            if (response.ok && data.success) {
                setReportSuccessMessage(data.message || 'Report created successfully!');
                await fetchReports();
                setNewReport({
                    mammogramId: '', patientId: '',
                    findings: '', conclusion: '',
                    recommendation: '', reportDate: new Date().toISOString().split('T')[0], createdByUserId: ''
                });
            } else {
                if (data.errors && Array.isArray(data.errors) && data.errors.length > 0) {
                    const errorMessages = data.errors.map((err: any) => err.defaultMessage || err.message).join('; ');
                    setReportError(`Validation failed: ${errorMessages}`);
                } else {
                    setReportError(data.message || `Failed to create report. Server responded with status: ${response.status} ${response.statusText}.`);
                }
            }
        } catch (err: any) {
            console.error('Error during report creation:', err);
            setReportError('Network error or an unexpected issue occurred during report creation.');
        } finally {
            setReportLoading(false);
        }
    };

    const handleDownloadPdf = async (reportId: number) => {
        setReportLoading(true);
        setReportError(null);
        setReportSuccessMessage(null);

        try {
            const response = await fetch(`${API_BASE_URL}/api/reports/${reportId}/pdf`, {
                method: 'GET',
                headers: getAuthHeaders(),
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to download PDF: ${response.status} ${response.statusText} - ${errorText}`);
            }

            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = window.document.createElement('a');
            a.href = url;
            a.download = `mammogram_report_${reportId}.pdf`;
            window.document.body.appendChild(a);
            a.click();
            a.remove();
            window.URL.revokeObjectURL(url);

            setReportSuccessMessage(`PDF for Report ID ${reportId} downloaded successfully!`);
        } catch (err: any) {
            console.error('Error downloading PDF:', err);
            setReportError(err.message || 'An error occurred while downloading the PDF.');
        } finally {
            setReportLoading(false);
        }
    };

    useEffect(() => {
        if (authToken) {
            void fetchReports();
        }
    }, [authToken]);

    return (
        <div className="space-y-8">
            {/* Create New Report Form */}
            <div>
                <h2 className="text-2xl font-bold text-gray-800 mb-4">Create New Report</h2>
                <form onSubmit={handleCreateReport} className="grid grid-cols-1 md:grid-cols-2 gap-4 bg-gray-50 p-6 rounded-lg shadow-inner">
                    <div>
                        <label htmlFor="reportMammogramId" className="block text-gray-700 text-sm font-semibold mb-1">Mammogram ID:</label>
                        <input type="number" id="reportMammogramId" className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-400 focus:border-blue-400"
                               value={newReport.mammogramId} onChange={(e) => setNewReport({ ...newReport, mammogramId: e.target.value })} required />
                    </div>
                    <div>
                        <label htmlFor="reportPatientId" className="block text-gray-700 text-sm font-semibold mb-1">Patient ID:</label>
                        <input type="number" id="reportPatientId" className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-400 focus:border-blue-400"
                               value={newReport.patientId} onChange={(e) => setNewReport({ ...newReport, patientId: e.target.value })} required />
                    </div>
                    <div className="md:col-span-2">
                        <label htmlFor="findings" className="block text-gray-700 text-sm font-semibold mb-1">Findings:</label>
                        <textarea id="findings" rows="3" className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-400 focus:border-blue-400"
                                  value={newReport.findings} onChange={(e) => setNewReport({ ...newReport, findings: e.target.value })} required></textarea>
                    </div>
                    <div className="md:col-span-2">
                        <label htmlFor="conclusion" className="block text-gray-700 text-sm font-semibold mb-1">Conclusion:</label>
                        <textarea id="conclusion" rows="3" className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-400 focus:border-blue-400"
                                  value={newReport.conclusion} onChange={(e) => setNewReport({ ...newReport, conclusion: e.target.value })} required></textarea>
                    </div>
                    <div className="md:col-span-2">
                        <label htmlFor="recommendations" className="block text-gray-700 text-sm font-semibold mb-1">Recommendations:</label>
                        <textarea id="recommendations" rows="3" className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-400 focus:border-blue-400"
                                  value={newReport.recommendation} onChange={(e) => setNewReport({ ...newReport, recommendation: e.target.value })} required></textarea>
                    </div>
                    <div>
                        <label htmlFor="reportDate" className="block text-gray-700 text-sm font-semibold mb-1">Report Date:</label>
                        <input type="date" id="reportDate" className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-400 focus:border-blue-400"
                               value={newReport.reportDate} onChange={(e) => setNewReport({ ...newReport, reportDate: e.target.value })} required />
                    </div>
                    <div>
                        <label htmlFor="createdByUserId" className="block text-gray-700 text-sm font-semibold mb-1">Created By User ID (Optional):</label>
                        <input type="number" id="createdByUserId" className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-400 focus:border-blue-400"
                               value={newReport.createdByUserId} onChange={(e) => setNewReport({ ...newReport, createdByUserId: e.target.value })} />
                    </div>
                    <div className="md:col-span-2 text-center">
                        <button
                            type="submit"
                            disabled={reportLoading}
                            className={`py-2 px-6 rounded-full font-bold text-white transition-all duration-300 ease-in-out shadow-md flex items-center justify-center space-x-2
                                ${reportLoading ? 'bg-gray-400 cursor-not-allowed' : 'bg-green-600 hover:bg-green-700 transform hover:scale-105 active:scale-95'}`}
                        >
                            {reportLoading ? (
                                <>
                                    <svg className="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                    </svg>
                                    <span>Creating Report...</span>
                                </>
                            ) : (
                                <span>Create Report</span>
                            )}
                        </button>
                    </div>
                </form>
                {reportSuccessMessage && <MessageDisplay type="success" message={reportSuccessMessage} />}
                {reportError && <MessageDisplay type="error" message={reportError} />}
            </div>

            {/* List All Reports */}
            <div>
                <h2 className="text-2xl font-bold text-gray-800 mb-4 flex items-center justify-between">
                    All Reports
                    <button
                        onClick={() => void fetchReports()}
                        className="bg-gray-200 hover:bg-gray-300 text-gray-800 font-semibold py-1 px-3 rounded-full text-sm transition-colors duration-200"
                        disabled={reportLoading}
                    >
                        {reportLoading ? 'Refreshing...' : 'Refresh List'}
                    </button>
                </h2>
                {reportLoading && reports.length === 0 && (
                    <div className="text-center text-gray-500">Loading reports...</div>
                )}
                {reports.length > 0 ? (
                    <div className="overflow-x-auto rounded-lg shadow-md border border-gray-200">
                        <table className="min-w-full divide-y divide-gray-200">
                            <thead className="bg-gray-100">
                            <tr>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">ID</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Mammogram ID</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Patient ID</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Findings</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Conclusion</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Recommendations</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Report Date</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                            </tr>
                            </thead>
                            <tbody className="bg-white divide-y divide-gray-200">
                            {reports.map((report: any) => (
                                <tr key={report.id}>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{report.id}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{report.mammogramId}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{report.patientId}</td>
                                    <td className="px-6 py-4 text-sm text-gray-900 max-w-xs overflow-hidden text-ellipsis">{report.findings}</td>
                                    <td className="px-6 py-4 text-sm text-gray-900 max-w-xs overflow-hidden text-ellipsis">{report.conclusion}</td>
                                    <td className="px-6 py-4 text-sm text-gray-900 max-w-xs overflow-hidden text-ellipsis">{report.recommendations}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{report.reportDate}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                                        <button
                                            onClick={() => void handleDownloadPdf(report.id)}
                                            className="text-blue-600 hover:text-blue-900 inline-flex items-center space-x-1"
                                            disabled={reportLoading}
                                        >
                                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
                                            </svg>
                                            <span>PDF</span>
                                        </button>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                ) : (
                    !reportLoading && reportError === null && (
                        <div className="text-center text-gray-500 p-4 border border-gray-200 rounded-lg">No reports found.</div>
                    )
                )}
            </div>
        </div>
    );
};

export default ReportManagementView;
