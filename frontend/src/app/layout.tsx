import type { Metadata } from 'next';
import { Inter } from 'next/font/google'; // Ensure Inter font is imported
import './globals.css'; // This imports your Tailwind CSS and other global styles

const inter = Inter({ subsets: ['latin'] });

export const metadata: Metadata = {
    title: 'Mammogram Diagnostic App',
    description: 'A comprehensive app for mammogram diagnostics and management.',
};

export default function RootLayout({
                                       children,
                                   }: {
    children: React.ReactNode;
}) {
    return (
        <html lang="en">
        {/* Apply the Inter font to the body */}
        <body className={inter.className}>{children}</body>
        </html>
    );
}
